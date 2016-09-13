/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.service;

import com.google.common.cache.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import net.coding.ide.event.GitCheckoutEvent;
import net.coding.ide.event.WorkspaceDeleteEvent;
import net.coding.ide.event.WorkspaceOfflineEvent;
import net.coding.ide.event.WorkspaceStatusEvent;
import net.coding.ide.git.PrivateKeyCredentialsProvider;
import net.coding.ide.model.*;
import net.coding.ide.model.ListStashResponse.Stash;
import net.coding.ide.model.RepositoryState;
import net.coding.ide.model.exception.GitInvalidPathException;
import net.coding.ide.model.exception.GitInvalidRefException;
import net.coding.ide.model.exception.GitOperationException;
import net.coding.ide.repository.WorkspaceRepository;
import net.coding.ide.utils.RebaseTodoUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.diff.DiffConfig;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.ignore.IgnoreNode;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.FollowFilter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static net.coding.ide.utils.RebaseTodoUtils.parseLines;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.eclipse.jgit.lib.ConfigConstants.*;

/**
 * Created by vangie on 14/12/29.
 */
@Slf4j
@Service
public class GitManagerImpl implements GitManager, ApplicationEventPublisherAware, ApplicationListener<WorkspaceStatusEvent> {
    /**
     * Prefix for branch refs
     */
    private static final String R_HEADS = "refs/heads/";

    /**
     * Prefix for remotes refs
     */
    private static final String R_REMOTES = "refs/remotes/";

    /**
     * Prefix for tag refs
     */
    private static final String R_TAGS = "refs/tags/";

    private static final String REMOTE_ORIGIN = "origin";

    private static final String CURRENT_DIRECTORY = "./";

    public static final String CONFLIX_FILE_BASE_SUFFIX = ".BASE";

    public static final String CONFLIX_FILE_LOCAL_SUFFIX = ".LOCAL";

    public static final String CONFLIX_FILE_REMOTE_SUFFIX = ".REMOTE";

    private static final String GIT_REBASE_TODO = "git-rebase-todo";

    /**
     * The name of the "rebase-merge" folder for interactive rebases.
     */
    private static final String REBASE_MERGE = "rebase-merge"; //$NON-NLS-1$

    /**
     * The name of the "rebase-apply" folder for non-interactive rebases.
     */
    private static final String REBASE_APPLY = "rebase-apply"; //$NON-NLS-1$

    private static final String DONE = "done"; //$NON-NLS-1$

    private ApplicationEventPublisher publisher;

    @Autowired
    private WorkspaceRepository wsRepo;

    @Autowired
    private KeyManager keyMgr;

    @Autowired
    private WorkspaceManager wsMgr;

    @Value("${USERNAME}")
    private String username;

    @Value("${EMAIL}")
    private String email;

    public GitManagerImpl() {
        synchronized (this) {
            reconfigureWindowCache();
        }
    }

    private static void reconfigureWindowCache() {
        final WindowCacheConfig windowCacheConfig = new WindowCacheConfig();
        windowCacheConfig.setPackedGitMMAP(false);
        windowCacheConfig.install();
    }

    private LoadingCache<String, Repository> repoCache = CacheBuilder
            .newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(100)
            .softValues()
            .removalListener(new RemovalListener<String, Repository>() {
                @Override
                public void onRemoval(RemovalNotification<String, Repository> notification) {
                    Repository repo = notification.getValue();
                    if (repo != null) {
                        repo.close();
                    }
                }
            })
            .build(new CacheLoader<String, Repository>() {
                @Override
                public Repository load(String spaceKey) throws Exception {
                    Workspace ws = wsMgr.getWorkspace(spaceKey);
                    return new FileRepositoryBuilder()
                            .setGitDir(new File(ws.getWorkingDir(), ".git"))
                            .setWorkTree(ws.getWorkingDir())
                            .setIndexFile(new File(ws.getWorkingDir(), ".git/index"))
                            .build();
                }
            });

    private static String toRemoteRefName(String ref) {
        return format("%s%s", R_REMOTES, ref);
    }

    private static String shortenRefName(String refName) {
        if (refName.startsWith(R_HEADS))
            return refName.substring(R_HEADS.length());
        if (refName.startsWith(R_TAGS))
            return refName.substring(R_TAGS.length());
        if (refName.startsWith(R_REMOTES))
            return refName.substring(R_REMOTES.length());
        return refName;
    }

    public Repository getRepository(String spaceKey) {

        try {
            return repoCache.get(spaceKey);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void invalidateRepository(String spaceKey) {
        repoCache.invalidate(spaceKey);
    }

    @Override
    public GitStatus status(Workspace ws, Path relativePath) throws Exception {
        if (new File(ws.getWorkingDir(), relativePath.toString()).isDirectory()) {
            return GitStatus.NONE;
        }
        String path = relativePath.toString();
        if (path != null && path.startsWith(CURRENT_DIRECTORY)) {
            path = relativePath.toString().substring(CURRENT_DIRECTORY.length());
        }

        Repository repository = getRepository(ws.getSpaceKey());
        Status status;

        try (Git git = new Git(repository)) {
            status = git.status().addPath(path).call();
        } catch (JGitInternalException e) {
            if (e.getCause() instanceof MissingObjectException
                    && e.getCause().getMessage().startsWith("Missing unknown")) {
                log.error("", e.getCause());
                return GitStatus.NONE;
            } else {
                throw e;
            }
        } catch (PatternSyntaxException e) {
            log.warn("error on pattern matching, relativePath: {}, pattern: {}: {}", relativePath, e.getPattern(), e.getMessage());
            return GitStatus.NONE;
        }


        if (!status.getConflicting().isEmpty()) {
            return GitStatus.CONFLICTION;
        } else if (!status.getUntracked().isEmpty() || !status.getUntrackedFolders().isEmpty()) {
            return GitStatus.UNTRACKED;
        } else if (!status.getModified().isEmpty()) {
            return GitStatus.MODIFIED;
        } else if (!status.getMissing().isEmpty()) {
            return GitStatus.MISSING;
        } else if (!status.getAdded().isEmpty()) {
            return GitStatus.ADDED;
        } else if (!status.getChanged().isEmpty()) {
            return GitStatus.CHANGED;
        } else if (!status.getRemoved().isEmpty()) {
            return GitStatus.REMOVED;
        } else if (!status.getIgnoredNotInIndex().isEmpty()) {
            return GitStatus.IGNORED;
        } else {
            return GitStatus.CLEAN;
        }
    }

    private CredentialsProvider getCredentialsProvider(Workspace ws) {
        File privateKeyFile = keyMgr.getPrivateKeyFile(ws);
        File knownHostsFile = keyMgr.getKnownHostsFile(ws);

        return new PrivateKeyCredentialsProvider(privateKeyFile, knownHostsFile);
    }

    @Override
    public boolean clone(Workspace ws) throws IOException, GitAPIException {

        Repository repository = getRepository(ws.getSpaceKey());

        CredentialsProvider cp = this.getCredentialsProvider(ws);

        String sshUrl = wsRepo.findProjectBySpaceKey(ws.getSpaceKey()).getSshUrl();

        try (Git git = new Git(repository)){
            git.cloneRepository()
                    .setBare(false)
                    .setCloneAllBranches(true)
                    .setDirectory(ws.getWorkingDir())
                    .setURI(sshUrl)
                    .setCredentialsProvider(cp)
                    .call()
                    .getRepository()
                    .close();
        }

        fetch(ws);

        return hasAtLeastOneReference(repository);

    }

    private boolean hasAtLeastOneReference(Repository repo) {

        for (Ref ref : repo.getAllRefs().values()) {
            if (ref.getObjectId() == null)
                continue;
            return true;
        }

        return false;
    }

    @Override
    public void config(Workspace ws) throws IOException {
        Repository repository = getRepository(ws.getSpaceKey());
        StoredConfig config = repository.getConfig();
        
        /**
         * [user]
         *      email = kevenyoung03@gmail.com
         *      name = kevenyoung03
         */
        config.setString("user", null, "email", email);
        config.setString("user", null, "name", username);

        /**
         *
         * rm warning: push.default is unset; its implicit value is changing in
         *
         * [push]
         *      default = simple
         */
        config.setString("push", null, "default", "simple");
        config.save();

        File excludeFile = new File(repository.getDirectory(), "info/exclude");

        Files.createParentDirs(excludeFile);

        Files.write(".coding-ide/", excludeFile, Charset.defaultCharset());

    }

    @Override
    public CommitStatus getStatus(Workspace ws) throws GitAPIException {
        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = new Git(repository)) {
            Status status = git.status().call();
            CommitStatus commitStatus = new CommitStatus(status.isClean());

            // staged files
            addToCommitStatus(commitStatus, status.getAdded(), GitStatus.ADDED);
            addToCommitStatus(commitStatus, status.getRemoved(), GitStatus.REMOVED);
            addToCommitStatus(commitStatus, status.getChanged(), GitStatus.CHANGED);
            addToCommitStatus(commitStatus, status.getConflicting(), GitStatus.CONFLICTION);

            // unstaged files
            addToCommitStatus(commitStatus, status.getUntracked(), GitStatus.UNTRACKED);
            addToCommitStatus(commitStatus, status.getMissing(), GitStatus.MISSING);
            addToCommitStatus(commitStatus, status.getModified(), GitStatus.MODIFIED);

            return commitStatus;
        }
    }

    private void addToCommitStatus(CommitStatus commitStatus, Set<String> files, GitStatus gitStatus) {
        files.stream().forEach(f -> commitStatus.putFile(f, gitStatus));
    }

    @Override
    public MergeResponse merge(Workspace ws, String branch) throws GitAPIException, IOException {
        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = new Git(repository)) {
            MergeCommand merge = git.merge();

            Ref mergeBranch = repository.findRef(branch);
            merge.include(mergeBranch);

            MergeResult mergeResult = merge.call();

            MergeResponse response = new MergeResponse();

            // prepare response
            response.setStatus(MergeResponse.Status.valueOf(mergeResult.getMergeStatus().name()));

            if (mergeResult.getMergeStatus().isSuccessful()) {
                response.setSuccess(true);

            } else {
                response.setSuccess(false);

                if (mergeResult.getConflicts() != null) {
                    mergeResult.getConflicts().forEach((k, v) -> response.addFailingPath(k));
                }
            }

            return response;
        }
    }

    @Override
    public void createStash(Workspace ws, String message) throws GitAPIException, GitOperationException {
        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = Git.wrap(repository)) {
            StashCreateCommand createCommand = git.stashCreate();

            createCommand.setIncludeUntracked(false);

            if (!isBlank(message)) {
                createCommand.setWorkingDirectoryMessage(message);
            }

            RevCommit commit = createCommand.call();

            if (commit == null) {
                throw new GitOperationException("没有要保存的本地修改");
            }
        }
    }

    /**
     *
     * @param ws
     * @param stashRef This will default to apply the latest stashed commit (stash@{0}) if unspecified
     * @param applyIndex
     * @throws GitAPIException
     */
    @Override
    public void applyStash(Workspace ws, String stashRef, boolean applyIndex, boolean pop) throws GitAPIException {
        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = Git.wrap(repository)) {
            StashApplyCommand applyCommand = git.stashApply();
            applyCommand.setApplyIndex(applyIndex);
            applyCommand.setStashRef(stashRef);

            applyCommand.call();

            if (pop) dropStash(ws, stashRef);
        }
    }

    public CheckoutResponse checkoutStash(Workspace ws, String stashRef, String branch) throws IOException, GitAPIException, GitOperationException {

        CheckoutResponse response = this.checkout(ws, branch, stashRef + "^");

        if (response.getStatus() == CheckoutResponse.Status.OK) {
            this.applyStash(ws, stashRef, true, true);
        }

        return response;
    }

    private DirCacheEntry[] findEntrys(Repository repository, String path) throws IOException {
        DirCache dirCache = repository.readDirCache();

        int eIdx = dirCache.findEntry(path);
        if (eIdx < 0) {
            throw new GitInvalidPathException(format("%s is not found in git index", path));
        }

        int lastIdx = dirCache.nextEntry(eIdx);

        final DirCacheEntry[] entries = new DirCacheEntry[lastIdx - eIdx];
        for (int i=0; i<entries.length; i++) {
            entries[i] = dirCache.getEntry(eIdx + i);
        }

        return entries;
    }

    @Override
    public ListStashResponse listStash(Workspace ws) throws GitAPIException {
        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = Git.wrap(repository)) {
            Collection<RevCommit> stashes = git.stashList().call();

            ListStashResponse response = new ListStashResponse();

            int index = 0;

            for (RevCommit rev : stashes) {
                response.addStash(new Stash(rev.getName(),
                        format(Constants.STASH + "@{%d}", index++),
                        rev.getShortMessage()));
            }

            return response;
        }
    }

    @Override
    public void dropAllStash(Workspace ws) throws GitAPIException {
        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = Git.wrap(repository)) {
            StashDropCommand dropCommand = git.stashDrop();
            dropCommand.setAll(true);
            dropCommand.call();
        }

    }

    @Override
    public void dropStash(Workspace ws, String stashRef) throws GitAPIException {
        Repository repository = getRepository(ws.getSpaceKey());

        String prefix = Constants.STASH + "@";

        int index;

        if (stashRef != null) {
            if (!stashRef.matches(prefix + "\\{\\d}")) {
                throw new GitInvalidRefException("stashRef must be \"stash@{}\" format");
            }

            stashRef = stashRef.substring(prefix.length() + 1);
            stashRef = stashRef.substring(0, stashRef.length() - 1);

            index = Integer.parseInt(stashRef);
        } else {
            index = 0;
        }

        try (Git git = Git.wrap(repository)) {
            StashDropCommand dropCommand = git.stashDrop();
            dropCommand.setStashRef(index);
            dropCommand.call();
        }
    }

    private ObjectId findMergeBase(Repository repository, ObjectId one, ObjectId two) throws IOException, GitOperationException {
        try (RevWalk revWalk = new RevWalk(repository)) {
            revWalk.markStart(revWalk.parseCommit(one));
            revWalk.markStart(revWalk.parseCommit(two));

            revWalk.setRevFilter(RevFilter.MERGE_BASE);
            RevCommit base = revWalk.next();
            if (base == null) {
                throw new GitOperationException(format("could't find merge base for %s and %s", one, two));
            }

            return base.toObjectId();
        }
    }

    private void checkRebaseState(Repository repository) throws GitOperationException {
        org.eclipse.jgit.lib.RepositoryState state = repository.getRepositoryState();

        if (getRebaseFile(repository, DONE).exists()) {
            throw new GitOperationException("update rebase todo is only allowed in begin state");
        }

        switch (state) {
            case REBASING_INTERACTIVE:
            case REBASING:
            case REBASING_REBASING:
            case REBASING_MERGE:
                break;
            default:
                throw new GitOperationException(format("repository state is %s, update rebase_todo is not allowed", state));
        }
    }

    public RebaseResponse rebase(Workspace ws, String upstream, boolean interactive, boolean preserve) throws GitAPIException, IOException, GitOperationException {
        return rebase(ws, null, upstream, interactive, preserve);
    }

    public RebaseResponse rebase(Workspace ws, String branch, String upstream, boolean interactive, boolean preserve) throws GitAPIException, IOException, GitOperationException {
        Repository repository = getRepository(ws.getSpaceKey());

        RebaseResponse response = new RebaseResponse();
        RebaseResult result = null;

        try (Git git = Git.wrap(repository)) {

            if (isNotBlank(branch)) {
                this.checkout(ws, branch, null);
            }

            if (interactive) {
                result = git.rebase()
                        .setUpstream(upstream)
                        .setPreserveMerges(preserve)
                        .runInteractively(null, true)
                        .call();

                try {
                    List<RebaseTodoLine> rebaseTodoLines = repository.readRebaseTodo(getRebasePath(repository, GIT_REBASE_TODO), false);

                    // trans to dto
                    List<RebaseResponse.RebaseTodoLine> lines = RebaseTodoUtils.loadFrom(rebaseTodoLines);
                    response.setRebaseTodoLines(lines);
                } catch (FileNotFoundException e) {
                    // if is update, git_rebase_todo file may not exist
                    response.setRebaseTodoLines(new ArrayList<>(0));
                }
            } else {
                result = git.rebase().setUpstream(upstream).call();
            }
        }

        response.setSuccess(result.getStatus().isSuccessful());
        response.setStatus(RebaseResponse.Status.valueOf(result.getStatus().name()));

        return response;
    }

    public RebaseResponse updateRebaseTodo(Workspace ws, List<RebaseResponse.RebaseTodoLine> lines) throws IOException, GitOperationException, GitAPIException {
        Repository repository = getRepository(ws.getSpaceKey());

        checkRebaseState(repository);

        List<RebaseTodoLine> rebaseTodoLines = parseLines(lines);

        repository.writeRebaseTodoFile(getRebasePath(repository, GIT_REBASE_TODO),
                rebaseTodoLines, false);

        return operateRebase(ws, RebaseOperation.PROCESS_STEPS);
    }

    public RebaseResponse operateRebase(Workspace ws, RebaseOperation operation) throws GitAPIException, IOException {
        return operateRebase(ws, operation, null);
    }

    /**
     * edit 状态需要 --amend
     * @param ws
     * @param operation
     * @return
     * @throws GitAPIException
     */
    public RebaseResponse operateRebase(Workspace ws, RebaseOperation operation, String message) throws GitAPIException, IOException {
        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = Git.wrap(repository)) {
            boolean amend = false;

            if (operation.equals(RebaseOperation.CONTINUE)) {
                try {
                    List<RebaseTodoLine> rebaseTodoLines = repository.readRebaseTodo(getRebasePath(repository, DONE), false);

                    if (rebaseTodoLines.size() != 0) {
                        // the last rebase_todo_line
                        RebaseTodoLine line = rebaseTodoLines.get(rebaseTodoLines.size() - 1);

                        Status status = git.status().call();

                        // need amend
                        if (line.getAction().equals(RebaseTodoLine.Action.EDIT) && status.getConflicting().size() == 0) {
                            if (message != null) {
                                git.commit()
                                        .setAll(true)
                                        .setAmend(true)
                                        .setNoVerify(true)
                                        .setMessage(message)
                                        .call();

                                amend = true;
                            } else { // tel frontend need message paramter
                                File messageFile = getRebaseFile(repository, "message");

                                RebaseResponse response = new RebaseResponse(false, RebaseResponse.Status.INTERACTIVE_EDIT);

                                if (messageFile.exists()) {
                                    response.setMessage(new String(IO.readFully(messageFile)));
                                }

                                return response;
                            }
                        } /*else if (line.getAction().equals(RebaseTodoLine.Action.REWORD)) { // todo: reward coundn't be stoped
                        if (message != null) {
                            git.rebase().runInteractively(new RebaseCommand.InteractiveHandler() {
                                @Override
                                public void prepareSteps(List<RebaseTodoLine> steps) {
                                    // do nothing
                                }

                                @Override
                                public String modifyCommitMessage(String commit) {
                                    return message;
                                }
                            }).setOperation(RebaseCommand.Operation.CONTINUE).call();
                        }
                    }*/
                    }
                } catch (FileNotFoundException e) {
                    // nothing_todo if done not exist
                }
            }

            RebaseResult result = git.rebase()
                    .setOperation(RebaseCommand.Operation.valueOf(operation.name()))
                    .call();

            // 如果 conflict and edit，amend 后 continue 会返回 NOTHING_TO_COMMIT
            // so skip this commit
            if (amend && result.getStatus().equals(RebaseResult.Status.NOTHING_TO_COMMIT)) {
                result = git.rebase().setOperation(RebaseCommand.Operation.SKIP).call();
            }

            return new RebaseResponse(result);
        }
    }

    @Override
    public RepositoryState state(Workspace ws) {
        Repository repository = getRepository(ws.getSpaceKey());

        org.eclipse.jgit.lib.RepositoryState state = repository.getRepositoryState();

        return RepositoryState.valueOf(state.name());
    }

    /***
     * @see RebaseCommand.RebaseState#getDir()
     */
    private File getRebaseStateDir(Repository repository) {
        File file = null;

        File rebaseApply = new File(repository.getDirectory(), REBASE_APPLY);
        if (rebaseApply.exists()) {
            file = rebaseApply;
        } else {
            file = new File(repository.getDirectory(), REBASE_MERGE);
        }

        return file;
    }

    public File getRebaseFile(Repository repository, String name) {
        return new File(getRebaseStateDir(repository), name);
    }

    public String getRebasePath(Repository repository, String name) {
        return (getRebaseStateDir(repository).getName() + "/" + name); //$NON-NLS-1$
    }

    private void generate_conflict_files(Workspace ws, ObjectId base, ObjectId local, ObjectId remote, String path) throws IOException {
        Repository repository = getRepository(ws.getSpaceKey());

        String content = readBlobContent(repository, base, ws.getEncoding());

        ws.write(path + CONFLIX_FILE_BASE_SUFFIX, content, false, true, false);

        content = readBlobContent(repository, local, ws.getEncoding());

        ws.write(path + CONFLIX_FILE_LOCAL_SUFFIX, content, false, true, false);

        content = readBlobContent(repository, remote, ws.getEncoding());

        ws.write(path + CONFLIX_FILE_REMOTE_SUFFIX, content, false, true, true);
    }


    /**
     * 读取 blob 中的内容
     * @param repository
     * @param objectId blob 的 id
     * @param encoding blob 的编码
     * @return 返回内容
     * @throws IOException
     */
    private String readBlobContent(Repository repository, ObjectId objectId, String encoding) throws IOException {
        if (objectId == null) return "";

        ObjectLoader loader = repository.open(objectId);
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        loader.copyTo(content);
        return content.toString(encoding);
    }

    /**
     * 读取某次提交的的某个文件的内容
     */
    @Override
    public String readFileFromRef(Workspace ws, String ref, String path, boolean base64) throws IOException {
        Repository repository = getRepository(ws.getSpaceKey());

        ObjectId objectId = repository.resolve(ref);

        if (objectId == null) {
            throw new GitInvalidRefException(format("ref %s is not exist", ref));
        }

        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(objectId);

            RevTree tree = commit.getTree();

            // now try to find a specific file
            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create(path));

                if (!treeWalk.next()) {
                    throw new GitInvalidPathException(format("Did not find expected file '%s'", path));
                }

                ObjectId blob = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(blob);

                ByteArrayOutputStream contentStream = new ByteArrayOutputStream();
                loader.copyTo(contentStream);

                revWalk.dispose();

                byte[] content = contentStream.toByteArray();

                if (base64) {
                    return BaseEncoding.base64().encode(content);
                } else {
                    return new String(content, ws.getEncoding());
                }
            }
        }
    }

    @Override
    public ConflictFile queryConflictFile(Workspace ws, String path, boolean base64) throws Exception {
        GitStatus status = status(ws, ws.getRelativePath(path));

        if (!status.equals(GitStatus.CONFLICTION)) {
            throw new GitOperationException(format("status of %s is not confliction", path));
        }

        String basePath = path + CONFLIX_FILE_BASE_SUFFIX;
        String localPath = path + CONFLIX_FILE_LOCAL_SUFFIX;
        String remotePath = path + CONFLIX_FILE_REMOTE_SUFFIX;

        if (!ws.exists(basePath)
                || !ws.exists(localPath)
                || !ws.exists(remotePath)) {
            Repository repository = getRepository(ws.getSpaceKey());

            DirCacheEntry[] entries = findEntrys(repository, ws.getRelativePath(path).toString());

            ObjectId local = null, remote = null, base = null;

            for (DirCacheEntry entry : entries) {
                if (entry.getStage() == DirCacheEntry.STAGE_1) base = entry.getObjectId();
                else if (entry.getStage() == DirCacheEntry.STAGE_2) local = entry.getObjectId();
                else if (entry.getStage() == DirCacheEntry.STAGE_3) remote = entry.getObjectId();
            }

            generate_conflict_files(ws, base, local, remote, path);
        }

        ConflictFile response = new ConflictFile();

        response.setBase(ws.read(basePath, base64));
        response.setLocal(ws.read(localPath, base64));
        response.setRemote(ws.read(remotePath, base64));

        return response;
    }

    @Override
    public void deleteConflictFile(Workspace ws, String path) throws Exception {
        GitStatus status = status(ws, ws.getRelativePath(path));

        if (!status.equals(GitStatus.CONFLICTION)) {
            throw new GitOperationException(format("status of %s is not confliction", path));
        }

        String basePath = path + CONFLIX_FILE_BASE_SUFFIX;
        String localPath = path + CONFLIX_FILE_LOCAL_SUFFIX;
        String remotePath = path + CONFLIX_FILE_REMOTE_SUFFIX;

        if (ws.exists(basePath)) ws.remove(basePath, false);
        if (ws.exists(localPath)) ws.remove(localPath, false);
        if (ws.exists(remotePath)) ws.remove(remotePath, false);
    }

    @Override
    public void resolveConflictFile(Workspace ws, String path, String content, boolean base64) throws Exception {
        GitStatus status = status(ws, ws.getRelativePath(path));

        if (!status.equals(GitStatus.CONFLICTION)) {
            throw new GitOperationException(format("status of %s is not confliction", path));
        }

        ws.write(path, content, base64, true, false);

        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = Git.wrap(repository)) {
            git.add().addFilepattern(path).call();
        }

        ws.remove(path + CONFLIX_FILE_BASE_SUFFIX, false);
        ws.remove(path + CONFLIX_FILE_LOCAL_SUFFIX, false);
        ws.remove(path + CONFLIX_FILE_REMOTE_SUFFIX, false);
    }

    @Override
    public List<String> commit(Workspace ws, List<String> files, String message) throws GitAPIException, IOException {

        List<String> result = Lists.newArrayList();

        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = Git.wrap(repository)) {
            CommitCommand commit = git.commit();

            CommitStatus commitStatus = this.getStatus(ws);

            // 上一次提交
            AbstractTreeIterator headTree = prepareTreeParser(repository, Constants.HEAD + "^{commit}");

            for (String file : files) {
                List<CommitStatus.File> fs = commitStatus.getFiles().stream()
                        .filter(f -> f.getName().equals(file))
                        .collect(Collectors.toList());

                if (fs.isEmpty()) {
                    continue;
                }

                if (fs.size() > 1) {

                    if (fs.stream()
                            .filter(f -> f.getStatus().equals(GitStatus.MISSING))
                            .findAny()
                            .isPresent()) {
                        new Git(repository).rm().addFilepattern(file).call();

                        try {
                            // 如果存在于版本库, 则需要提交
                            // 否则不需要, 如果这里不单独判断, 则如果没有修改的提交 jgit 会抛出异常, 提示 no changes.
                            // 即使设置 allowEmpty 也不行, 可以算是 jgit 的一个 bug.
                            if (headTree.findFile(file)) {
                                commit.setOnly(file);
                            } else {
                                continue;
                            }
                        } catch (CorruptObjectException e) {
                            log.error("commit missing file failed", e);
                        }
                    }
                }

                for (CommitStatus.File f : fs) {
                    GitStatus gitStatus = f.getStatus();

                    if (gitStatus == GitStatus.REMOVED) {
                        new Git(repository).rm().addFilepattern(file).call();
                    } else if (gitStatus != GitStatus.MISSING){
                        new Git(repository).add().addFilepattern(file).call();
                        result.add(file);
                    }

                    commit.setOnly(file);
                }
            }

            commit.setMessage(message).call();

            return result;
        }
    }

    @Override
    public List<String> commitAll(Workspace ws, String message) throws GitAPIException, IOException {
        CommitStatus commitStatus = this.getStatus(ws);

        List<String> files = Lists.newArrayList();
        for (CommitStatus.File file : commitStatus.getFiles()) {
            files.add(file.getName());
        }

        return commit(ws, files, message);
    }

    @Override
    public String diff(Workspace ws, String path, String oldRef, String newRef) throws IOException, GitAPIException {
        Repository repository = getRepository(ws.getSpaceKey());

        AbstractTreeIterator oldTreeParser = prepareTreeParser(repository, oldRef);
        AbstractTreeIterator newTreeParser = prepareTreeParser(repository, newRef);

        Config config = new Config();
        config.setBoolean("diff", null, "renames", true);
        DiffConfig diffConfig = config.get(DiffConfig.KEY);

        String relativePath = ws.getRelativePath(path).toString();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             Git git = Git.wrap(repository)) {

            git.diff()
                .setOldTree(oldTreeParser)
                .setNewTree(newTreeParser)
                .setPathFilter(FollowFilter.create(relativePath, diffConfig))
                .setOutputStream(baos)
                .call();

            return baos.toString();
        }
    }

    @Override
    public void sync(Workspace ws) throws GitAPIException {
        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = Git.wrap(repository)) {
            git.submoduleSync().call();
        }
    }

    private PushResponse processPushResults(Iterable<PushResult> results) {
        PushResponse response = new PushResponse();
        response.setNothingToPush(true);
        response.setOk(true);

        List<PushResponse.Update> updates = new ArrayList<>();
        for (PushResult result : results) {
            for (RemoteRefUpdate update : result.getRemoteUpdates()) {
                RemoteRefUpdate.Status s = update.getStatus();
                if (s != RemoteRefUpdate.Status.UP_TO_DATE) {
                    response.setNothingToPush(false);
                }

                if (s != RemoteRefUpdate.Status.OK) {
                    response.setOk(false);
                }


                PushResponse.Status status = PushResponse.Status.OK;
                if (s == RemoteRefUpdate.Status.AWAITING_REPORT) {
                    status = PushResponse.Status.AWAITING_REPORT;
                } else if (s == RemoteRefUpdate.Status.NON_EXISTING) {
                    status = PushResponse.Status.NON_EXISTING;
                } else if (s == RemoteRefUpdate.Status.NOT_ATTEMPTED) {
                    status = PushResponse.Status.NOT_ATTEMPTED;
                } else if (s == RemoteRefUpdate.Status.REJECTED_NODELETE) {
                    status = PushResponse.Status.REJECTED_NODELETE;
                } else if (s == RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD) {
                    status = PushResponse.Status.REJECTED_NONFASTFORWARD;
                } else if (s == RemoteRefUpdate.Status.REJECTED_OTHER_REASON) {
                    status = PushResponse.Status.REJECTED_OTHER_REASON;
                } else if (s == RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED) {
                    status = PushResponse.Status.REJECTED_REMOTE_CHANGED;
                } else if (s == RemoteRefUpdate.Status.UP_TO_DATE) {
                    status = PushResponse.Status.UP_TO_DATE;
                }

                PushResponse.Update u = new PushResponse.Update();
                u.setRemoteRefName(REMOTE_ORIGIN + "/" + shortenRefName(update.getRemoteName()));
                u.setLocalRefName(shortenRefName(update.getSrcRef()));
                u.setStatus(status);
                u.setMessage(update.getMessage());
                updates.add(u);
            }
        }

        response.setUpdates(updates);
        return response;
    }

    @Override
    public PushCommits getPushCommits(Workspace ws) throws IOException, GitAPIException, GitOperationException {
        Repository repository = getRepository(ws.getSpaceKey());
        String currentBranch = repository.getBranch();
        return this.getPushCommits(ws, currentBranch);
    }

    @Override
    public PushCommits getPushCommits(Workspace ws, String branch) throws IOException, GitAPIException, GitOperationException {
        Repository repository = getRepository(ws.getSpaceKey());

        PushCommits pushCommits = new PushCommits();
        pushCommits.setLocalRef(branch);
        pushCommits.setRemote(REMOTE_ORIGIN);

        try (RevWalk walk = new RevWalk(repository)) {
            Ref localRef = repository.getRef(R_HEADS + branch);
            if (localRef == null) {
                throw new GitOperationException(format("branch %s not existed", branch));
            }

            ObjectId localHead = localRef.getObjectId();

            String remoteBranch = REMOTE_ORIGIN + "/" + branch;
            Ref remoteRef = repository.getRef(R_REMOTES + remoteBranch);

            Iterable<RevCommit> commits;
            if (remoteRef == null) {
                commits = this.getUnmergedCommits(ws, localHead, walk);
                pushCommits.setRemoteRef("+" + branch);
            } else {
                commits = new Git(repository).log().addRange(remoteRef.getObjectId(), localHead).call();
                pushCommits.setRemoteRef(branch);
            }


            for (RevCommit commit : commits) {
                String sha = ObjectId.toString(commit.getId());

                PushCommits.Commit c = new PushCommits.Commit();
                c.setShortMessage(commit.getShortMessage());
                c.setFullMessage(commit.getFullMessage());
                c.setSha(sha);
                c.setDiffEntries(this.getDiffEntryForCommit(ws, sha));
                pushCommits.addCommit(c);
            }

            walk.dispose();
        }

        return pushCommits;
    }

    @Override
    public PushResponse push(Workspace ws) throws GitAPIException, IOException, GitOperationException {

        Repository repository = getRepository(ws.getSpaceKey());

        String branch = repository.getFullBranch();
        if (branch == null) {
            throw new GitOperationException("Current branch not found");
        }

        CredentialsProvider cp = this.getCredentialsProvider(ws);
        RefSpec refspec = new RefSpec(branch);

        try (Git git = Git.wrap(repository)) {
            Iterable<PushResult> results = git.push()
                    .setCredentialsProvider(cp)
                    .setRemote(REMOTE_ORIGIN)
                    .setRefSpecs(refspec)
                    .call();

            return processPushResults(results);
        }
    }

    @Override
    public PushResponse push(Workspace ws, String ref) throws GitAPIException, IOException, GitOperationException {

        Repository repository = getRepository(ws.getSpaceKey());
        CredentialsProvider cp = this.getCredentialsProvider(ws);

        // resolve full ref name
        String r = this.getFullRefName(ws, ref);
        if (r == null) {
            r = ref;
        }
        RefSpec refspec = new RefSpec(r);

        try (Git git = Git.wrap(repository)) {
            Iterable<PushResult> results = git.push()
                    .setCredentialsProvider(cp)
                    .setRemote(REMOTE_ORIGIN)
                    .setRefSpecs(refspec)
                    .call();

            return processPushResults(results);
        }
    }

    @Override
    public PushResponse pushAll(Workspace ws) throws GitAPIException, IOException, GitOperationException {

        Repository repository = getRepository(ws.getSpaceKey());
        CredentialsProvider cp = this.getCredentialsProvider(ws);

        try (Git git = Git.wrap(repository)) {

            Iterable<PushResult> results = git.push()
                    .setCredentialsProvider(cp)
                    .setRemote(REMOTE_ORIGIN)
                    .setPushAll()
                    .setPushTags()
                    .call();

            return processPushResults(results);
        }
    }

    @Override
    public boolean pull(Workspace ws) throws GitAPIException, IOException {
        Repository repository = getRepository(ws.getSpaceKey());

        String currentBranch = repository.getBranch();

        try (Git git = Git.wrap(repository)) {
            PullCommand command = git.pull()
                    .setRemote(REMOTE_ORIGIN)
                    .setRemoteBranchName(currentBranch);

            CredentialsProvider cp = this.getCredentialsProvider(ws);
            command.setCredentialsProvider(cp);

            PullResult result = command.call();

            return result.isSuccessful();
        }
    }

    @Override
    public CheckoutResponse checkout(Workspace ws, String name, String startPoint) throws GitAPIException, IOException, GitOperationException {
        Repository repository = getRepository(ws.getSpaceKey());


        try (Git git = Git.wrap(repository)) {
            CheckoutCommand command = git.checkout().setName(name);

            if (isRemoteBranch(ws, name)) {
                throw new GitOperationException("Remote branch must be checkout as new local branch");
            }

            if (StringUtils.isNotEmpty(startPoint)) {
                command.setStartPoint(startPoint);
                command.setCreateBranch(true);
            }

            try {
                command.call();
            } catch (CheckoutConflictException e) {
                // Ignore
            }

            CheckoutResult result = command.getResult();
            CheckoutResult.Status s = result.getStatus();

            CheckoutResponse.Status status = CheckoutResponse.Status.OK;
            if (s == CheckoutResult.Status.CONFLICTS) {
                status = CheckoutResponse.Status.CONFLICTS;
            } else if (s == CheckoutResult.Status.ERROR) {
                status = CheckoutResponse.Status.ERROR;
            } else if (s == CheckoutResult.Status.NONDELETED) {
                status = CheckoutResponse.Status.NONDELETED;
            } else if (s == CheckoutResult.Status.NOT_TRIED) {
                status = CheckoutResponse.Status.NOT_TRIED;
            }

            CheckoutResponse response = new CheckoutResponse();
            response.setStatus(status);
            response.setConflictList(result.getConflictList());
            response.setModifiedList(result.getModifiedList());
            response.setRemovedList(result.getRemovedList());
            response.setUndeletedList(result.getUndeletedList());

            publisher.publishEvent(new GitCheckoutEvent(ws, repository.getBranch()));

            return response;
        }

    }

    @Override
    public void fetch(Workspace ws) throws GitAPIException {
        fetch(ws, false);
    }

    /**
     * Fetch all known remotes.
     */
    @Override
    public void fetch(Workspace ws, boolean prune) throws GitAPIException {
        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = Git.wrap(repository)) {
            Set<String> remotes = repository.getRemoteNames();
            for (String remote : remotes) {
                FetchCommand command = git.fetch()
                        .setCheckFetchedObjects(true)
                        .setRemoveDeletedRefs(prune)
                        .setRemote(remote);

                CredentialsProvider cp = this.getCredentialsProvider(ws);
                command.setCredentialsProvider(cp);

                command.call();
            }
        }

    }

    @Override
    public String getBranch(Workspace ws) throws IOException {
        Repository repository = getRepository(ws.getSpaceKey());

        return repository.getBranch();
    }

    @Override
    public List<String> getLocalBranches(Workspace ws) throws GitAPIException {
        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = Git.wrap(repository)) {
            List<Ref> refs = git.branchList().call();
            List<String> branches = Lists.newArrayList();
            for (Ref ref : refs) {
                branches.add(Repository.shortenRefName(ref.getName()));
            }

            return branches;
        }
    }

    @Override
    public List<String> getRemoteBranches(Workspace ws) throws GitAPIException {
        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = Git.wrap(repository)) {
            List<Ref> refs = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();
            List<String> branches = Lists.newArrayList();
            for (Ref ref : refs) {
                branches.add(Repository.shortenRefName(ref.getName()));
            }

            return branches;
        }

    }

    @Override
    public Branches getBranches(Workspace ws) throws GitAPIException, IOException {

        String current = getBranch(ws);

        return Branches.of(current,
                getLocalBranches(ws),
                getRemoteBranches(ws));
    }

    @Override
    public void createBranch(Workspace ws, String branchName) throws GitAPIException {
        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = Git.wrap(repository)) {
            git.branchCreate().setName(branchName).call();
        }
    }

    @Override
    public void deleteBranch(Workspace ws, String branchName) throws GitAPIException, IOException, GitOperationException {
        Repository repository = getRepository(ws.getSpaceKey());

        boolean isRemote = isRemoteBranch(ws, branchName);

        try (Git git = Git.wrap(repository)) {
            git.branchDelete().setBranchNames(branchName).setForce(true).call();

            if (isRemote) {
                CredentialsProvider cp = this.getCredentialsProvider(ws);

                String[] splits = branchName.split("/");
                String remoteName = splits[0];
                String refName = splits[1];

                RefSpec refSpec = new RefSpec(":refs/heads/" + refName);

                git.push()
                        .setCredentialsProvider(cp)
                        .setRefSpecs(refSpec)
                        .setRemote(remoteName)
                        .setForce(true)
                        .call();
            }
        }


    }

    @Override
    public boolean hasBranch(Workspace ws, String branch) throws GitAPIException {
        List<String> localBranches = getLocalBranches(ws);
        if (localBranches.contains(branch)) {
            return true;
        }

        List<String> remoteBranches = getRemoteBranches(ws);
        return remoteBranches.contains(branch);

    }

    private boolean isRemoteBranch(Workspace ws, String branch) throws GitAPIException {
        List<String> remoteBranches = getRemoteBranches(ws);
        return remoteBranches.contains(branch);

    }

    @Override
    public List<String> getTags(Workspace ws) throws GitAPIException {
        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = Git.wrap(repository)) {
            List<Ref> refs = git.tagList().call();
            List<String> tags = new ArrayList<>();
            for (Ref ref : refs) {
                tags.add(Repository.shortenRefName(ref.getName()));
            }

            return tags;
        }
    }

    @Override
    public void createTag(Workspace ws, String tagName, String ref, String message, boolean force) throws GitAPIException, IOException {
        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = Git.wrap(repository);
             RevWalk walk = new RevWalk(repository)) {

            TagCommand tag = git.tag();

            // 为 tag 指定 commit
            if (!isBlank(ref)) {
                ObjectId objectId = repository.resolve(ref);

                if (objectId == null) {
                    throw new GitInvalidRefException(format("ref %s is not exist", ref));
                }

                RevCommit commit = walk.parseCommit(objectId);

                tag.setObjectId(commit);
            }

            tag.setName(tagName)
                    .setForceUpdate(force);

            if (isBlank(message)) {
                tag.setAnnotated(false);
            } else {
                tag.setAnnotated(true);
                tag.setMessage(message);
            }

            tag.call();

            walk.dispose();
        }
    }

    @Override
    public void deleteTag(Workspace ws, String tagName) throws GitAPIException {
        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = Git.wrap(repository)) {
            git.tagDelete().setTags(tagName).call();
        }
    }

    @Override
    public void reset(Workspace ws, String ref, ResetType resetType) throws GitAPIException {
        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = Git.wrap(repository)) {
            git.reset()
                    .setMode(ResetCommand.ResetType.valueOf(resetType.name()))
                    .setRef(ref)
                    .call();
        }
    }

    private String getFullRefName(Workspace ws, String name) throws GitAPIException {
        if (name == null) {
            return null;
        }

        String branch = getFullBranch(ws, name);
        if (branch != null) {
            return branch;
        }

        String tag = getFullTag(ws, name);
        if (tag != null) {
            return tag;
        }

        return null;
    }

    private String getFullBranch(Workspace ws, String branch) throws GitAPIException {
        if (branch == null) {
            return null;
        }
        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = Git.wrap(repository)) {
            List<Ref> refs = git.branchList().call();
            for (Ref ref : refs) {
                String b = shortenRefName(ref.getName());
                if (branch.equals(b)) {
                    return ref.getName();
                }
            }

            return null;
        }

    }

    private String getFullTag(Workspace ws, String tag) throws GitAPIException {
        if (tag == null) {
            return null;
        }

        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = Git.wrap(repository)) {
            List<Ref> refs = git.tagList().call();
            for (Ref ref : refs) {
                String t = shortenRefName(ref.getName());
                if (tag.equals(t)) {
                    return ref.getName();
                }
            }

            return null;
        }
    }

    private List<RevCommit> getUnmergedCommits(Workspace ws, ObjectId head, RevWalk walk) throws IOException, GitAPIException {
        RevCommit commit = walk.parseCommit(head);

        walk.markStart(commit);

        List<RevCommit> commits = Lists.newArrayList();
        for (RevCommit rev : walk) {
            if (this.remoteContains(ws, ObjectId.toString(rev.getId()))) {
                break;
            }

            commits.add(rev);
        }

        return commits;
    }

    private boolean remoteContains(Workspace ws, String commitId) throws GitAPIException {
        Repository repository = getRepository(ws.getSpaceKey());

        try (Git git = Git.wrap(repository)) {
            List<Ref> refs = git.branchList()
                    .setContains(commitId)
                    .setListMode(ListBranchCommand.ListMode.REMOTE)
                    .call();

            return !refs.isEmpty();
        }
    }

    public List<DiffEntry> getDiffEntryForCommit(Workspace ws, String commitId) throws IOException, GitAPIException {
        Repository repository = getRepository(ws.getSpaceKey());

        ObjectId oldTree = repository.resolve(commitId + "^^{tree}");
        ObjectId newTree = repository.resolve(commitId + "^{tree}");

        if (newTree == null) {
            throw new GitInvalidRefException(format("invalid git ref %s", commitId));
        }

        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
        CanonicalTreeParser newTreeIter = new CanonicalTreeParser();

        try (ObjectReader reader = repository.newObjectReader();
            Git git = Git.wrap(repository)) {

            if (oldTree != null) {
                oldTreeIter.reset(reader, oldTree);
            }

            newTreeIter.reset(reader, newTree);

            List<org.eclipse.jgit.diff.DiffEntry> diffs = git.diff()
                    .setNewTree(newTreeIter)
                    .setOldTree(oldTreeIter)
                    .call();

            List<DiffEntry> diffEntries = Lists.newArrayList();
            for (org.eclipse.jgit.diff.DiffEntry diff : diffs) {
                DiffEntry diffEntry = new DiffEntry();

                if (diff.getChangeType() == org.eclipse.jgit.diff.DiffEntry.ChangeType.ADD) {
                    diffEntry.setChangeType(DiffEntry.ChangeType.ADD);
                } else if (diff.getChangeType() == org.eclipse.jgit.diff.DiffEntry.ChangeType.COPY) {
                    diffEntry.setChangeType(DiffEntry.ChangeType.COPY);
                } else if (diff.getChangeType() == org.eclipse.jgit.diff.DiffEntry.ChangeType.DELETE) {
                    diffEntry.setChangeType(DiffEntry.ChangeType.DELETE);
                } else if (diff.getChangeType() == org.eclipse.jgit.diff.DiffEntry.ChangeType.MODIFY) {
                    diffEntry.setChangeType(DiffEntry.ChangeType.MODIFY);
                } else if (diff.getChangeType() == org.eclipse.jgit.diff.DiffEntry.ChangeType.RENAME) {
                    diffEntry.setChangeType(DiffEntry.ChangeType.RENAME);
                }

                diffEntry.setOldPath(diff.getOldPath());
                diffEntry.setNewPath(diff.getNewPath());

                diffEntries.add(diffEntry);
            }

            return diffEntries;
        }
    }

    private AbstractTreeIterator prepareTreeParser(Repository repository, String ref) throws IOException {
        if ("~~staged~~".equals(ref)) {
            return new DirCacheIterator(DirCache.read(repository));
        } else if ("~~unstaged~~".equals(ref)) {
            return new FileTreeIterator(repository);
        }

        try (RevWalk walk = new RevWalk(repository)) {
            ObjectId commitObjectId = repository.resolve(ref);
            if (commitObjectId == null) {
                throw new GitInvalidRefException(format("invalid git ref %s", ref));
            }

            log.debug("ref: {}, commit id: {}", ref, commitObjectId.toString());

            RevCommit commit = walk.parseCommit(commitObjectId);
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader objectReader = repository.newObjectReader()) {
                treeParser.reset(objectReader, tree.getId());
            }

            return treeParser;
        }
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }

    @Override
    public boolean checkIgnore(InputStream in, String path, boolean isDir) {
        IgnoreNode ignoreNode = new IgnoreNode();
        try {
            ignoreNode.parse(in);
        } catch (IOException e) {
            return false;
        }
        return ignoreNode.isIgnored(path, isDir) == IgnoreNode.MatchResult.IGNORED;
    }

    @Override
    public void onApplicationEvent(WorkspaceStatusEvent event) {
        String spaceKey = event.getSpaceKey();
        if (event instanceof WorkspaceOfflineEvent) {
            invalidateRepository(spaceKey);
        } else if (event instanceof WorkspaceDeleteEvent) {
            invalidateRepository(spaceKey);
        }
    }
}
