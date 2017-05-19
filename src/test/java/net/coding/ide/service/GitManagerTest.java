/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.service;

import com.google.common.cache.LoadingCache;
import com.google.common.io.Files;
import lombok.SneakyThrows;
import net.coding.ide.model.*;
import net.coding.ide.model.RepositoryState;
import net.coding.ide.model.exception.GitInvalidPathException;
import net.coding.ide.model.exception.GitInvalidRefException;
import net.coding.ide.model.exception.GitOperationException;
import net.coding.ide.repository.WorkspaceRepository;
import net.coding.ide.utils.DataPopulator;
import net.coding.ide.utils.RepositoryHelper;
import net.coding.ide.web.mapping.PersonIdentConverter;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.junit.JGitTestUtil;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.AuthorRevFilter;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static net.coding.ide.model.DiffEntry.ChangeType.*;
import static net.coding.ide.model.MergeResponse.Status.CONFLICTING;
import static net.coding.ide.model.RebaseResponse.RebaseTodoLine.Action.FIXUP;
import static net.coding.ide.model.RebaseResponse.RebaseTodoLine.Action.REWORD;
import static net.coding.ide.model.RebaseResponse.RebaseTodoLine.Action.SQUASH;
import static net.coding.ide.model.RebaseResponse.Status.*;
import static net.coding.ide.service.GitManagerImpl.*;
import static net.coding.ide.utils.FilesUtils.createTempDirectoryAndDeleteOnExit;
import static net.coding.ide.utils.RepositoryHelper.createTestBranch;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Created by vangie on 14/12/29.
 */
public class GitManagerTest extends BaseServiceTest {

    @InjectMocks
    private GitManagerImpl gitMgr;

    @Mock
    private WorkspaceRepository wsRepo;

    @Mock
    private WorkspaceManager wsMgr;

    private File testSpaceHome;

    @Mock
    private LoadingCache<String, Repository> repoCache;

    @Mock
    private ApplicationEventPublisher publisher;

    private DataPopulator dataPopulator = new DataPopulator();

    private Repository repository;

    private Workspace ws;

    @Spy
    private ModelMapper mapper = new ModelMapper();

    @Before
    public void setUp() throws IOException, GitAPIException, ExecutionException {
        testSpaceHome = createTempDirectoryAndDeleteOnExit("codingSpaceKeys");

        ws = new Workspace(dataPopulator.populateWorkspaceEntity(), testSpaceHome);

        repository = RepositoryHelper.createRepository(ws.getWorkingDir());

        when(repoCache.get(ws.getSpaceKey())).thenReturn(repository);

        Mockito.reset(wsRepo);

        ReflectionTestUtils.setField(gitMgr, "repoCache", repoCache);

        when(repoCache.get(ws.getSpaceKey())).thenReturn(repository);

        mapper.addConverter(new PersonIdentConverter());

        // init repo
        initRepo(repository);
    }

    private void initRepo(Repository repository) throws IOException, GitAPIException {
        Git git = new Git(repository);

        ws.write("README.md", "This is readme", false, true, false);
        ws.write("README2.md", "this is readme2", false, true, false);

        git.add().addFilepattern("README.md").call();
        git.add().addFilepattern("README2.md").call();

        git.commit().setMessage("create readme").call();

        // create test branch
        createTestBranch(repository, "branch1", (g, branch) -> {
            ws.write("README.md", "branch1", false, true, false);

            g.add().addFilepattern("README.md").call();
            g.commit().setMessage(branch).call();
        });

        createTestBranch(repository, "branch2", (g, branch) -> {
            ws.write("README.md", "branch2", false, true, false);

            g.add().addFilepattern("README.md").call();
            g.commit().setMessage(branch).call();
        });
    }

    @Test
    @Ignore
    public void testMergeWithConflics() throws IOException, GitAPIException, ExecutionException, GitOperationException {

        gitMgr.checkout(ws, "branch1", null);

        MergeResponse mergeResponse = gitMgr.merge(ws, "branch2");

        assertEquals(mergeResponse.isSuccess(), false);
        assertEquals(mergeResponse.getFailingPaths().size(), 1);
        assertEquals(mergeResponse.getFailingPaths().get(0), "README.md");
        assertEquals(mergeResponse.getStatus(), CONFLICTING);
    }

    @Test
    public void testQueryConflictFile() throws Exception {

        testMergeWithConflics();

        String readme = "README.md";

        ConflictFile conflictFile = gitMgr.queryConflictFile(ws, "README.md", false);

        assertEquals(conflictFile.getBase(), "This is readme");
        assertEquals(conflictFile.getLocal(), "branch1");
        assertEquals(conflictFile.getRemote(), "branch2");

        assertEquals(true, ws.exists(readme + CONFLIX_FILE_BASE_SUFFIX));
        assertEquals(true, ws.exists(readme + CONFLIX_FILE_LOCAL_SUFFIX));
        assertEquals(true, ws.exists(readme + CONFLIX_FILE_REMOTE_SUFFIX));
    }

    @Test
    public void testQueryConflictFileWithDeletedByOurs() throws Exception {
        createTestBranch(repository, "testBranch", (g, branch) -> {
            ws.write("README.md", "testBranch", false, true, false);

            g.add().addFilepattern("README.md").call();
            g.commit().setMessage(branch).call();
        });

        try (Git git = Git.wrap(repository)) {
            ws.remove("README.md", false);

            git.commit().setAll(true).setMessage("delete readme").call();
        }

        gitMgr.merge(ws, "testBranch");

        ConflictFile conflictFile = gitMgr.queryConflictFile(ws, "README.md", false);

        assertEquals(conflictFile.getBase(), "This is readme");
        assertEquals(conflictFile.getLocal(), "");
        assertEquals(conflictFile.getRemote(), "testBranch");
    }

    @Test
    public void testQueryConflictFileWithDeleteFileAfterMerge() throws Exception {
        testMergeWithConflics();

        ws.remove("README.md", true);

        String readme = "README.md";

        ConflictFile conflictFile = gitMgr.queryConflictFile(ws, "README.md", false);

        assertEquals(conflictFile.getBase(), "This is readme");
        assertEquals(conflictFile.getLocal(), "branch1");
        assertEquals(conflictFile.getRemote(), "branch2");

        assertEquals(true, ws.exists(readme + CONFLIX_FILE_BASE_SUFFIX));
        assertEquals(true, ws.exists(readme + CONFLIX_FILE_LOCAL_SUFFIX));
        assertEquals(true, ws.exists(readme + CONFLIX_FILE_REMOTE_SUFFIX));
    }

    @Test
    public void testQueryConflictFileForAbsolutePath() throws Exception {

        testMergeWithConflics();

        String readme = "/README.md";

        ConflictFile conflictFile = gitMgr.queryConflictFile(ws, readme, false);

        assertEquals(conflictFile.getBase(), "This is readme");
        assertEquals(conflictFile.getLocal(), "branch1");
        assertEquals(conflictFile.getRemote(), "branch2");

        assertEquals(true, ws.exists(readme + CONFLIX_FILE_BASE_SUFFIX));
        assertEquals(true, ws.exists(readme + CONFLIX_FILE_LOCAL_SUFFIX));
        assertEquals(true, ws.exists(readme + CONFLIX_FILE_REMOTE_SUFFIX));
    }

    @Test
    public void testLogWithRootDirectory() throws GitAPIException, IOException {
        try (Git git = Git.wrap(repository)) {

            // test for size
            PageRequest request = new PageRequest(0, 10);

            List<GitLog> gitLogs = gitMgr.log(ws, new String[]{"HEAD"}, new String[]{"/"}, null, null, null, request);

            assertEquals(1, gitLogs.size());

            gitLogs = gitMgr.log(ws, new String[]{"HEAD"}, new String[]{"/"}, null, null, null, request);

            assertEquals(1, gitLogs.size());
        }
    }

    @Test
    public void testLogWithoutStash() throws IOException, GitAPIException, GitOperationException {
        testStashCreate();

        PageRequest request = new PageRequest(0, 10);
        List<GitLog> logs = gitMgr.log(ws, null, null, null, null, null, request);

        assertEquals(3, logs.size());
    }

    @Test
    public void testLogWithPathAndPage() throws IOException, GitAPIException {
        String filename = "README.md";

        try (Git git = Git.wrap(repository)) {
            for (int i=1; i<=10; i++) {
                String message = String.format("modified readme %d times", i);
                writeFileAndCommit(git, "README.md", message, message);
            }

            // test for size
            PageRequest request = new PageRequest(0, 1);

            List<GitLog> gitLogs = gitMgr.log(ws, null, new String[]{filename}, null, null, null, request);

            assertEquals(1, gitLogs.size());

            // test for size greeter than exist
            request = new PageRequest(0, 14);

            gitLogs = gitMgr.log(ws, new String[] {"HEAD"}, new String[]{filename}, null, null, null, request);

            assertEquals(11, gitLogs.size());

            // test for pages
            request = new PageRequest(2, 2);

            gitLogs = gitMgr.log(ws, new String[] {"HEAD"}, new String[]{filename}, null, null, null, request);


            assertEquals(2, gitLogs.size());
            assertEquals(7, gitLogs.get(0).getShortName().length());
            assertEquals(7, gitLogs.get(1).getShortName().length());

            // test for page too large
            request = new PageRequest(20, 2);

            gitLogs = gitMgr.log(ws, null, new String[]{filename}, null, null, null, request);

            assertEquals(0, gitLogs.size());
        }
    }

    @Test
    public void testLogWithRef() throws IOException, GitAPIException {
        PageRequest request = new PageRequest(0, 100);

        try (Git git = Git.wrap(repository)) {
            reCreateCleanRepo();

            RevCommit master1 = writeFileAndCommit(git, "master1", "master1", "master1");
            RevCommit master2 = writeFileAndCommit(git, "master2", "master2", "master2");

            git.checkout().setCreateBranch(true).setName("branch1").call();
            git.checkout().setCreateBranch(true).setName("branch2").call();

            git.checkout().setName("branch1").call();
            RevCommit branch1 = writeFileAndCommit(git, "branch1", "branch1", "branch1");

            git.checkout().setName("branch2").call();
            RevCommit branch2 = writeFileAndCommit(git, "branch2", "branch2", "branch2");

            List<GitLog> logs = gitMgr.log(ws, new String[]{"branch1"}, null, null, null, null, request);

            assertEquals(3, logs.size());
            assertEquals(branch1.name(), logs.get(0).getName());
            assertEquals(master2.name(), logs.get(1).getName());
            assertEquals(master1.name(), logs.get(2).getName());

            assertEquals(master2.getName(), logs.get(0).getParents()[0]); // master2 is branch1's parents
            assertEquals(master1.getName(), logs.get(1).getParents()[0]); // master1 is master2's parents
            assertEquals(0, logs.get(2).getParents().length); // master 1 no parents
        }
    }

    @Test
    public void testLogWithDateFilter() throws IOException, GitAPIException, InterruptedException {
        PageRequest request = new PageRequest(0, 100);

        try (Git git = Git.wrap(repository)) {
            reCreateCleanRepo();

            RevCommit master1 = writeFileAndCommit(git, "master1", "master1", "master1");

            Thread.sleep(1000);

            RevCommit master2 = writeFileAndCommit(git, "master2", "master2", "master2");

            Thread.sleep(1000);

            RevCommit master3 = writeFileAndCommit(git, "master3", "master3", "master3");

            List<GitLog> logs = gitMgr.log(ws, null, null, null, null, 1000L * master2.getCommitTime(), request);

            assertEquals(2, logs.size());
            assertEquals(master2.name(), logs.get(0).getName());
            assertEquals(master1.name(), logs.get(1).getName());

            logs = gitMgr.log(ws, null, null, null, 1000L * master2.getCommitTime(), null, request);

            assertEquals(2, logs.size());
            assertEquals(master3.name(), logs.get(0).getName());
            assertEquals(master2.name(), logs.get(1).getName());

            logs = gitMgr.log(ws, null, null, null, 1000L * master2.getCommitTime(), 1000L * master2.getCommitTime(), request);

            assertEquals(1, logs.size());
            assertEquals(master2.name(), logs.get(0).getName());
        }
    }

    @Test
    public void testLogWithUserFilter() throws IOException, GitAPIException, InterruptedException {
        PageRequest request = new PageRequest(0, 100);

        try (Git git = Git.wrap(repository)) {
            reCreateCleanRepo();

            RevCommit user1 = writeFileAndCommitWithAuthor(git, "user1", "user1@coding.ent", "master1", "master1", "master1");

            Thread.sleep(1000);

            RevCommit user2 = writeFileAndCommitWithAuthor(git, "user2", "user2@coding.ent", "master2", "master2", "master2");

            Thread.sleep(1000);

            RevCommit user3 = writeFileAndCommitWithAuthor(git, "user3", "user3@coding.ent", "master3", "master3", "master3");

            List<GitLog> logs = gitMgr.log(ws, null, null, new String[]{"user1"}, null, null, request);// test regular text

            assertEquals(1, logs.size());
            assertEquals(user1.name(), logs.get(0).getName());


            logs = gitMgr.log(ws, null, null, new String[]{"user[12]"}, null, null, request); // test regex text

            assertEquals(2, logs.size());
            assertEquals(user2.name(), logs.get(0).getName());
            assertEquals(user1.name(), logs.get(1).getName());

            // test combine author filter
            logs = gitMgr.log(ws, null, null,
                    new String[] {"user2", "user3"},
                    null,
                    null,
                    request);

            assertEquals(2, logs.size());
            assertEquals(user3.name(), logs.get(0).getName());
            assertEquals(user2.name(), logs.get(1).getName());
        }
    }

    private void reCreateCleanRepo() throws IOException {
        ws.remove(".git", true);
        repository.create();
    }

    @Test
    public void testLogAll() throws IOException, GitAPIException {
        PageRequest request = new PageRequest(0, 100);

        try (Git git = Git.wrap(repository)) {
            ws.remove(".git", true);
            repository.create();

            RevCommit master1 = writeFileAndCommit(git, "master1", "master1", "master1");
            RevCommit master2 = writeFileAndCommit(git, "master2", "master2", "master2");

            git.checkout().setCreateBranch(true).setName("branch1").call();
            git.checkout().setCreateBranch(true).setName("branch2").call();

            git.checkout().setName("branch1").call();
            RevCommit branch1 = writeFileAndCommit(git, "branch1", "branch1", "branch1");

            git.checkout().setName("branch2").call();
            RevCommit branch2 = writeFileAndCommit(git, "branch2", "branch2", "branch2");

            List<GitLog> logs = gitMgr.log(ws, null, null, null, null, null, request);

            assertEquals(4, logs.size());
            assertEquals(branch2.name(), logs.get(0).getName());
            assertEquals(branch1.name(), logs.get(1).getName());
            assertEquals(master2.name(), logs.get(2).getName());
            assertEquals(master1.name(), logs.get(3).getName());

            assertEquals(master2.getName(), logs.get(0).getParents()[0]); // master2 is branch2's parents
            assertEquals(master2.getName(), logs.get(1).getParents()[0]); // master2 is branch1's parents
            assertEquals(master1.getName(), logs.get(2).getParents()[0]); // master1 is master2's parents
            assertEquals(0, logs.get(3).getParents().length); // master 1 no parents
        }
    }


    @Test
    public void testBlame() throws GitAPIException, IOException {
        try (Git git = Git.wrap(repository)) {

            writeFileAndCommit(git, "testBlame", "first commit", "first line");
            writeFileAndCommit(git, "testBlame", "second commit", "first line", "second line");
            writeTrashFile("testBlame", "first line\nmodified line\nsecond line\n");

            List<GitBlame> gitBlames = gitMgr.blame(ws, "testBlame");

            assertEquals(3, gitBlames.size());
            assertNotNull(gitBlames.get(0).getShortName());
            assertNull(gitBlames.get(1).getShortName());
            assertNotNull(gitBlames.get(2).getShortName());
        }
    }

    @Test
    public void testBlameWithRename() throws GitAPIException, IOException {
        try (Git git = Git.wrap(repository)) {

            writeFileAndCommit(git, "testBlame", "first commit", "first line");
            writeFileAndCommit(git, "testBlame", "second commit", "first line", "second line");
            writeTrashFile("testBlame", "first line\nmodified line\nsecond line\n");
            ws.move("testBlame", "renameTestBlame", false);

            git.add().addFilepattern("renameTestBlame").call();
            git.commit().setMessage("rename file").call();

            writeTrashFile("renameTestBlame", "first line\nsecond modified line after rename\nmodified line\nsecond line\n");

            List<GitBlame> gitBlames = gitMgr.blame(ws, "renameTestBlame");

            assertEquals(4, gitBlames.size());

            assertNotNull(gitBlames.get(0).getShortName());
            assertNull(gitBlames.get(1).getShortName());
            assertNotNull(gitBlames.get(2).getShortName());
            assertNotNull(gitBlames.get(3).getShortName());
        }
    }

    @Test
    public void testBlameNotTracked() throws IOException, GitAPIException {
        try (Git git = Git.wrap(repository)) {

            writeTrashFile("testBlame", "first line\n1");

            List<GitBlame> gitBlames = gitMgr.blame(ws, "testBlame");

            assertEquals(2, gitBlames.size());
            assertNull(gitBlames.get(0).getShortName());
            assertNull(gitBlames.get(1).getShortName());
        }
    }

    @Test
    public void testBlameNotExist() throws IOException, GitAPIException {
        try (Git git = Git.wrap(repository)) {

            List<GitBlame> gitBlames = gitMgr.blame(ws, "notExist");

            assertEquals(0, gitBlames.size());
        }
    }

    @Test
    public void testResolveConflictFile() throws Exception {
        testQueryConflictFile();

        String filename = "README.md";

        gitMgr.resolveConflictFile(ws, filename, "resolve conflicts", false);

        assertEquals(ws.read(filename, false), "resolve conflicts");

        assertEquals(gitMgr.status(ws, ws.getRelativePath(filename)), GitStatus.CHANGED);
    }

    @Test
    public void testDeleteConflictFile() throws Exception {
        testQueryConflictFile();

        String readme = "README.md";

        gitMgr.deleteConflictFile(ws, readme);

        assertEquals(false, ws.exists(readme + CONFLIX_FILE_BASE_SUFFIX));
        assertEquals(false, ws.exists(readme + CONFLIX_FILE_LOCAL_SUFFIX));
        assertEquals(false, ws.exists(readme + CONFLIX_FILE_REMOTE_SUFFIX));
    }

    @Test
    @Ignore
    public void testStashCreate() throws IOException, GitAPIException, GitOperationException {
        writeTrashFile("test", "This is readme");

        Git.wrap(repository).add().addFilepattern("test").call();

        gitMgr.createStash(ws, false, null);
    }

    @Test(expected = GitOperationException.class)
    public void testStashCreateWithException() throws GitAPIException, GitOperationException {
        gitMgr.createStash(ws, false, null);
    }

    @Test
    public void testStashApplyWithNoPop() throws GitAPIException, IOException, GitOperationException {
        testStashCreate();

        gitMgr.applyStash(ws, null, false, false);

        assertStashListSize(1);
    }

    @Test(expected = StashApplyFailureException.class)
    public void testStashApplyWithConflict() throws Exception {
        Git git = Git.wrap(repository);

        ws.write("test", "This is readme", false, true, false);

        git.add().addFilepattern("test").call();

        gitMgr.createStash(ws, false, null);

        ws.write("test", "This is readme conflict", false, true, false);

        git.add().addFilepattern("test").call();
        git.commit().setMessage("commit conflit readme").call();

        try {
            gitMgr.applyStash(ws, null, false, false);
        } catch (StashApplyFailureException e) {

            ConflictFile conflictFile = gitMgr.queryConflictFile(ws, "test", false);

            assertEquals(conflictFile.getBase(), "");
            assertEquals(conflictFile.getLocal(), "This is readme conflict");
            assertEquals(conflictFile.getRemote(), "This is readme");

            throw e;
        }
    }

    @Test
    public void testStashApplyWithPop() throws GitAPIException, IOException, GitOperationException {
        testStashCreate();

        gitMgr.applyStash(ws, null, false, true);

        assertStashListSize(0);
    }

    @Test(expected = InvalidRefNameException.class)
    public void testStashApplyWithException() throws GitAPIException {
        gitMgr.applyStash(ws, "stash@{0}", false, false);
    }

    @Test
    public void checkoutStash() throws GitAPIException, IOException, GitOperationException {
        testStashCreate();

        assertStashListSize(1);

        Git git = Git.wrap(repository);

        String stashRef = "stash@{0}";
        String branch = "testCheckoutStash";

        gitMgr.checkoutStash(ws, stashRef, branch);

        assertStashListSize(0);

        assertEquals(branch, this.gitMgr.getBranch(ws));

        Status status = git.status().call();

        assertEquals(1, status.getAdded().size());
        assertEquals(true, status.getAdded().contains("test"));
    }

    @Test
    public void testDropStashWithRef() throws GitAPIException, IOException, GitOperationException {
        testStashCreate();

        assertStashListSize(1);

        gitMgr.dropStash(ws, "stash@{0}");

        assertStashListSize(0);
    }

    @Test
    public void testStashWithUntractked() throws IOException, GitAPIException, GitOperationException {
        try (Git git = Git.wrap(repository)) {
            writeTrashFile("test", "This is readme");
            writeTrashFile("notTractedFile", "this file is untracked");

            git.add().addFilepattern("test").call();

            gitMgr.createStash(ws, false, null);

            Status status = git.status().call();

            assertEquals(0, status.getAdded().size());
            assertEquals(1, status.getUntracked().size());

            gitMgr.applyStash(ws, null, false, false);

            status = git.status().call();

            assertEquals(1, status.getAdded().size());
            assertEquals(1, status.getUntracked().size());
        }
    }

    @Test
    public void testDropAllStash() throws GitAPIException, IOException, GitOperationException, InterruptedException {
        testStashCreate();

        Thread.sleep(1000);

        testStashCreate();

        assertStashListSize(2);

        gitMgr.dropAllStash(ws);

        assertStashListSize(0);
    }

    @Test
    public void testRefs() throws IOException, GitAPIException, GitOperationException {
        testStashCreate();

        gitMgr.createBranch(ws, "remoteBranch");

        ws.mkdir(".git/refs/remotes");
        ws.move(".git/refs/heads/remoteBranch", ".git/refs/remotes/remoteBranch", true);

        List<GitRef> gitRefs = gitMgr.refs(ws);

        assertEquals(5, gitRefs.size());
        assertEquals("HEAD", gitRefs.get(0).getName());
        assertEquals("refs/heads/branch1", gitRefs.get(1).getName());
        assertEquals("refs/heads/branch2", gitRefs.get(2).getName());
        assertEquals("refs/heads/master", gitRefs.get(3).getName());
        assertEquals("refs/remotes/remoteBranch", gitRefs.get(4).getName());
    }

    @Test
    public void testGetDiffEntryForCommit() throws GitAPIException, IOException, GitOperationException {
        ws.write("test", "This is readme", false, true, false);

        ws.write("README.md", "update readme", false, true, false);

        Git.wrap(repository).add().addFilepattern("test").call();

        ws.write("testUntracked", "testUntracked", false, true, false);

        ws.remove("README2.md", false);

        gitMgr.createStash(ws, false, null);

        // will not contain untracked files diff
        List<DiffEntry> entries = gitMgr.getDiffEntryForCommit(ws, "stash@{0}");

        assertEquals(3, entries.size());
        assertEquals("README.md", entries.get(0).getOldPath());
        assertEquals("README.md", entries.get(0).getNewPath());
        assertEquals(MODIFY, entries.get(0).getChangeType());

        assertEquals("README2.md", entries.get(1).getOldPath());
        assertEquals("/dev/null", entries.get(1).getNewPath());
        assertEquals(DELETE, entries.get(1).getChangeType());

        assertEquals("/dev/null", entries.get(2).getOldPath());
        assertEquals("test", entries.get(2).getNewPath());
        assertEquals(ADD, entries.get(2).getChangeType());
    }

    @Test(expected = GitInvalidRefException.class)
    public void testGetDiffEntryForCommitWithNotExistRef() throws GitAPIException, IOException, GitOperationException {
        gitMgr.getDiffEntryForCommit(ws, "not_exist");
    }

    private void assertStashListSize(int size) throws GitAPIException, IOException, GitOperationException {
        ListStashResponse response = gitMgr.listStash(ws);

        assertEquals(size, response.getStashes().size());
    }

    @Test
    public void testCommitPartialWhenFileIsMissing() throws Exception {
        try (Git git = Git.wrap(repository)) {
            ws.remove("README.md", false);
            ws.write("README2.md", "update readme2", false, true, false);

            Status status = git.status().addPath("README.md").call();

            Assert.assertEquals(false, status.getMissing().isEmpty());

            gitMgr.commit(ws, asList("README.md", "README2.md"), "update readme");

            status = git.status().call();

            assertEquals(true, status.isClean());
        }

    }

    @Test
    public void testCommitPartial() throws IOException, GitAPIException {

        ws.write("README.md", "update Readme", false, true, false);

        ws.write("README2.md", "update Readme2", false, true, false);

        try (Git git = Git.wrap(repository)) {
            git.add().addFilepattern(".").call();

            git.commit().setOnly("README.md").setMessage("commit partial").call();

            Set<String> changed = git.status().call().getChanged();

            assertEquals(1, changed.size());
            assertEquals(true, changed.contains("README2.md"));
        }
    }

    @Test
    public void testCommitPartialWithMultiFile() throws IOException, GitAPIException {

        try (Git git = Git.wrap(repository)) {
            ws.write("test1", "this is test1 file", false, true, false);

            ws.write("test2", "This is test2 file", false, true, false);

            gitMgr.commit(ws, asList("test1", "test2"), "commit all");

            assertEquals(true, git.status().call().isClean());
        }
    }

    @Test
    public void testCommitWithFileEndWithBlank() throws IOException, GitAPIException {
        ws.write("aa ", "aaa with black", false, true, false);
        ws.write("aa aa", "aaa with black", false, true, false);
        ws.write("bb ", "aaa with black", false, true, false);

        this.gitMgr.commit(ws, asList("aa ", "aa aa", "bb "), "test");

        CommitStatus status = this.gitMgr.getStatus(ws);

        assertEquals(true, status.isClean());
    }

    @Test
    public void testCommitPartialWithOnlyMissingFile() throws IOException, GitAPIException {

        try (Git git = Git.wrap(repository)) {
            ws.write("testMissing", "this is file test missing", false, true, false);

            git.add().addFilepattern("testMissing").call();

            ws.remove("testMissing", false);

            assertEquals(true, git.status().call().getMissing().contains("testMissing"));

            gitMgr.commit(ws, asList("testMissing"), "testMissing");

            git.status().call().isClean();
        }
    }

    @Test
    public void testCommitPartialWithOnlyMissingFile1() throws IOException, GitAPIException {

        try (Git git = Git.wrap(repository)) {
            ws.write("README.md", "this is file test missing", false, true, false);

            git.add().addFilepattern("README.md").call();

            ws.remove("README.md", false);

            assertEquals(true, git.status().call().getMissing().contains("README.md"));

            gitMgr.commit(ws, asList("README.md"), "README");

            git.status().call().isClean();
        }
    }

    @Test
    public void testEmptyBlank() throws Exception {
        try (Git git = Git.wrap(repository)) {
            ws.write("testFile", "testFile", false, true, false);

            ws.mkdir("testDir");

            git.add().addFilepattern(".").call();

            CommitStatus status = gitMgr.getStatus(ws);

            assertEquals(1, status.getFiles().size());
        }
    }

    @Test
    public void testRebaseWithFastForward() throws IOException, GitAPIException, GitOperationException {
        try (Git git = Git.wrap(repository)) {

            this.gitMgr.createBranch(ws, "rebaseBranch");
            this.gitMgr.checkout(ws, "rebaseBranch", null);

            ws.write("two", "two", false, true, false);

            git.add().addFilepattern("two").call();

            git.commit().setMessage("rebase_two").call();

            this.gitMgr.checkout(ws, "master", null);

            ws.write("one", "one", false, true, false);

            git.add().addFilepattern("one").call();

            git.commit().setMessage("rebase_one").call();

            this.gitMgr.checkout(ws, "rebaseBranch", null);

            RebaseResponse response = this.gitMgr.rebase(ws, "master", false, false);

            assertEquals(OK, response.getStatus());
        }
    }

    @Test
    public void testRebaseForBranchParamWithFastForward() throws IOException, GitAPIException, GitOperationException {
        try (Git git = Git.wrap(repository)) {

            this.gitMgr.createBranch(ws, "rebaseBranch");
            this.gitMgr.checkout(ws, "rebaseBranch", null);

            ws.write("two", "two", false, true, false);

            git.add().addFilepattern("two").call();

            git.commit().setMessage("rebase_two").call();

            this.gitMgr.checkout(ws, "master", null);

            ws.write("one", "one", false, true, false);

            git.add().addFilepattern("one").call();

            git.commit().setMessage("rebase_one").call();

            RebaseResponse response = this.gitMgr.rebase(ws, "rebaseBranch", "master", false, false);

            assertEquals(OK, response.getStatus());
        }
    }

    @Test(expected = UnmergedPathsException.class)
    public void testRebaseContinueWhenConflict() throws Exception {
        try (Git git = Git.wrap(repository)) {

            this.gitMgr.createBranch(ws, "rebaseBranch");
            this.gitMgr.checkout(ws, "rebaseBranch", null);

            writeFileAndCommit(git, "one", "rebase_one", "one rebase");

            this.gitMgr.checkout(ws, "master", null);

            writeFileAndCommit(git, "one", "one", "one");

            this.gitMgr.checkout(ws, "rebaseBranch", null);

            RebaseResponse response = this.gitMgr.rebase(ws, "master", false, false);

            assertEquals(STOPPED, response.getStatus());

            Status status = git.status().call();

            ConflictFile file = this.gitMgr.queryConflictFile(ws, status.getConflicting().iterator().next(), false);

            assertEquals("one\n", file.getLocal());
            assertEquals("one rebase\n", file.getRemote());

            response = this.gitMgr.operateRebase(ws, RebaseOperation.CONTINUE);

            assertEquals(OK, response.getStatus());
        }
    }

    @Test
    public void testRebaseForConflict() throws Exception {
        try (Git git = Git.wrap(repository)) {

            this.gitMgr.createBranch(ws, "rebaseBranch");
            this.gitMgr.checkout(ws, "rebaseBranch", null);

            writeFileAndCommit(git, "rebase1", "rebase1", "rebase1");
            writeFileAndCommit(git, "rebase2", "rebase2", "rebase2");

            this.gitMgr.checkout(ws, "master", null);

            writeFileAndCommit(git, "rebase1", "master1", "master1");
            writeFileAndCommit(git, "rebase2", "master2", "master2");

            this.gitMgr.checkout(ws, "rebaseBranch", null);

            RebaseResponse response = this.gitMgr.rebase(ws, "master", false, false);

            assertEquals(STOPPED, response.getStatus());

            Status status = git.status().call();

            ConflictFile file = this.gitMgr.queryConflictFile(ws, status.getConflicting().iterator().next(), false);

            assertEquals("master1\n", file.getLocal());
            assertEquals("rebase1\n", file.getRemote());

            this.gitMgr.resolveConflictFile(ws, "rebase1", "resolve1", false);

            response = this.gitMgr.operateRebase(ws, RebaseOperation.CONTINUE);

            assertEquals(STOPPED, response.getStatus());

            status = git.status().call();

            file = this.gitMgr.queryConflictFile(ws, status.getConflicting().iterator().next(), false);

            assertEquals("master2\n", file.getLocal());
            assertEquals("rebase2\n", file.getRemote());

            this.gitMgr.resolveConflictFile(ws, "rebase2", "resolve2", false);

            response = this.gitMgr.operateRebase(ws, RebaseOperation.CONTINUE);

            assertEquals(OK, response.getStatus());
        }
    }

    @Test
    public void testRebaseResolveWhenConflict() throws Exception {
        try (Git git = Git.wrap(repository)) {

            this.gitMgr.createBranch(ws, "rebaseBranch");
            this.gitMgr.checkout(ws, "rebaseBranch", null);

            writeFileAndCommit(git, "one", "rebase_one", "rebase one");

            this.gitMgr.checkout(ws, "master", null);

            writeFileAndCommit(git, "one", "one", "one");

            this.gitMgr.checkout(ws, "rebaseBranch", null);

            RebaseResponse response = this.gitMgr.rebase(ws, "master", false, false);

            assertEquals(STOPPED, response.getStatus());

            writeTrashFile("one", "resolve conflict");

            git.add().addFilepattern("one").call();

            response = this.gitMgr.operateRebase(ws, RebaseOperation.CONTINUE);

            assertEquals(OK, response.getStatus());
        }
    }

    @Test
    public void testRebaseResolveWhenMultiConflict() throws Exception {
        try (Git git = Git.wrap(repository)) {

            this.gitMgr.createBranch(ws, "rebaseBranch");
            this.gitMgr.checkout(ws, "rebaseBranch", null);

            writeFileAndCommit(git, "one", "rebase_one", "rebase one");

            writeFileAndCommit(git, "two", "rebase_two", "rebase two");

            this.gitMgr.checkout(ws, "master", null);

            writeFileAndCommit(git, "one", "one", "one");
            writeFileAndCommit(git, "two", "two", "two");

            this.gitMgr.checkout(ws, "rebaseBranch", null);

            // start rebase

            RebaseResponse response = this.gitMgr.rebase(ws, "master", false, false);

            assertEquals(STOPPED, response.getStatus());

            // resolve first conflict

            writeTrashFile("one", "resolve conflict one");

            git.add().addFilepattern("one").call();

            response = this.gitMgr.operateRebase(ws, RebaseOperation.CONTINUE);
            assertEquals(STOPPED, response.getStatus());

            // resolve second conflict

            writeTrashFile("two", "resolve conflict two");

            git.add().addFilepattern("two").call();

            response = this.gitMgr.operateRebase(ws, RebaseOperation.CONTINUE);
            assertEquals(OK, response.getStatus());
        }
    }

    @Test
    public void testRebaseWithInteractive() throws Exception {
        try (Git git = Git.wrap(repository)) {

            this.gitMgr.createBranch(ws, "rebaseBranch");
            this.gitMgr.checkout(ws, "rebaseBranch", null);

            writeFileAndCommit(git, "one", "rebase_one", "rebase one");

            writeFileAndCommit(git, "two", "rebase_two", "rebase two");

            this.gitMgr.checkout(ws, "master", null);

            writeFileAndCommit(git, "one", "one", "one");
            writeFileAndCommit(git, "two", "two", "two");

            this.gitMgr.checkout(ws, "rebaseBranch", null);

            // start rebase
            RebaseResponse response = this.gitMgr.rebase(ws, "master", true, false);

            assertEquals(RebaseResponse.Status.INTERACTIVE_PREPARED, response.getStatus());

            // update rebase_todo
            List<RebaseResponse.RebaseTodoLine> lines = response.getRebaseTodoLines();
            assertEquals(2, lines.size());

            this.gitMgr.updateRebaseTodo(ws, lines);

            // resolve first conflict

            writeTrashFile("one", "resolve conflict one");

            git.add().addFilepattern("one").call();

            response = this.gitMgr.operateRebase(ws, RebaseOperation.CONTINUE);

            assertEquals(STOPPED, response.getStatus());
            assertEquals(false, response.isSuccess());

            // resolve second conflict

            writeTrashFile("two", "resolve conflict two");

            git.add().addFilepattern("two").call();

            response = this.gitMgr.operateRebase(ws, RebaseOperation.CONTINUE);

            assertEquals(OK, response.getStatus());
            assertEquals(true, response.isSuccess());
        }
    }

    @Test
    public void testRebaseForInteractiveSquash() throws IOException, GitAPIException, GitOperationException {
        try (Git git = Git.wrap(repository)) {

            this.gitMgr.createBranch(ws, "rebase1");
            this.gitMgr.createBranch(ws, "rebase2");
            this.gitMgr.checkout(ws, "rebase1", null);

            writeFileAndCommit(git, "A", "add A", "A");

            this.gitMgr.checkout(ws, "rebase2", null);

            writeFileAndCommit(git, "B", "add B", "B");

            writeFileAndCommit(git, "C", "add C", "C");

            writeFileAndCommit(git, "D", "add D", "D");

            RebaseResponse response = this.gitMgr.rebase(ws, "rebase2", "rebase1", true, false);

            assertEquals(RebaseResponse.Status.INTERACTIVE_PREPARED, response.getStatus());

            // update rebase_todo
            List<RebaseResponse.RebaseTodoLine> lines = response.getRebaseTodoLines();

            lines.get(1).setAction(SQUASH);

            response = this.gitMgr.updateRebaseTodo(ws, lines);

            assertEquals(false, response.isSuccess());

            assertNotNull(response.getMessage());

            response = this.gitMgr.operateRebase(ws, RebaseOperation.CONTINUE);

            assertEquals("INTERACTIVE_EDIT", response.getStatus().name());
            assertEquals("# This is a combination of 2 commits.\n" +
                    "# The first commit's message is:\n" +
                    "add B\n" +
                    "# This is the 2nd commit message:\n" +
                    "add C\n", response.getMessage());

            response = this.gitMgr.operateRebase(ws, RebaseOperation.CONTINUE, "squash B and C");

            assertEquals("OK", response.getStatus().name());

            Iterable<RevCommit> iterable = git.log().call();

            assertGitLogMessageEquals(iterable, "add D", "squash B and C", "add A");
        }
    }

    @Test
    public void testRebaseForInteractiveReword() throws IOException, GitAPIException, GitOperationException {
        try (Git git = Git.wrap(repository)) {

            this.gitMgr.createBranch(ws, "rebase1");
            this.gitMgr.createBranch(ws, "rebase2");
            this.gitMgr.checkout(ws, "rebase1", null);

            writeFileAndCommit(git, "A", "add A", "A");

            this.gitMgr.checkout(ws, "rebase2", null);

            writeFileAndCommit(git, "B", "add B", "B");

            writeFileAndCommit(git, "C", "add C", "C");

            writeFileAndCommit(git, "D", "add D", "D");

            RebaseResponse response = this.gitMgr.rebase(ws, "rebase2", "rebase1", true, false);

            assertEquals(RebaseResponse.Status.INTERACTIVE_PREPARED, response.getStatus());

            // update rebase_todo
            List<RebaseResponse.RebaseTodoLine> lines = response.getRebaseTodoLines();

            lines.get(1).setAction(REWORD);
            lines.get(2).setAction(REWORD);

            response = this.gitMgr.updateRebaseTodo(ws, lines);

            assertEquals(RebaseResponse.Status.INTERACTIVE_EDIT, response.getStatus());
            assertEquals(false, response.isSuccess());
            assertEquals(false, response.isSuccess());
            assertEquals("add C", response.getMessage());

            response = this.gitMgr.operateRebase(ws, RebaseOperation.CONTINUE);

            assertEquals(RebaseResponse.Status.INTERACTIVE_EDIT, response.getStatus());
            assertEquals(false, response.isSuccess());
            assertEquals("add C", response.getMessage());

            response = this.gitMgr.operateRebase(ws, RebaseOperation.CONTINUE, "reword C");

            assertEquals(RebaseResponse.Status.INTERACTIVE_EDIT, response.getStatus());
            assertEquals(false, response.isSuccess());
            assertEquals("add D", response.getMessage());

            response = this.gitMgr.operateRebase(ws, RebaseOperation.CONTINUE, "reword D");

            assertEquals("OK", response.getStatus().name());

            Iterable<RevCommit> iterable = git.log().call();

            assertGitLogMessageEquals(iterable, "reword D", "reword C", "add B", "add A");
        }
    }

    @Test
    public void testRebaseForInteractiveFixup() throws Exception {
        try (Git git = Git.wrap(repository)) {

            this.gitMgr.createBranch(ws, "rebase1");
            this.gitMgr.createBranch(ws, "rebase2");
            this.gitMgr.checkout(ws, "rebase1", null);

            writeFileAndCommit(git, "A", "add A", "A");

            this.gitMgr.checkout(ws, "rebase2", null);

            writeFileAndCommit(git, "B", "add B", "B");

            writeFileAndCommit(git, "C", "add C", "C");

            writeFileAndCommit(git, "D", "add D", "D");

            RebaseResponse response = this.gitMgr.rebase(ws, "rebase2", "rebase1", true, false);

            assertEquals(RebaseResponse.Status.INTERACTIVE_PREPARED, response.getStatus());

            // update rebase_todo
            List<RebaseResponse.RebaseTodoLine> lines = response.getRebaseTodoLines();

            lines.get(1).setAction(FIXUP);

            response = this.gitMgr.updateRebaseTodo(ws, lines);

            assertEquals(RebaseResponse.Status.OK, response.getStatus());
            assertEquals(true, response.isSuccess());

            Iterable<RevCommit> iterable = git.log().call();

            assertGitLogMessageEquals(iterable, "add D", "add B");
        }
    }

    private static void assertGitLogMessageEquals(Iterable iterable, String ...logs) throws GitAPIException {
        Iterator<RevCommit> revs = iterable.iterator();

        for (int i=0; i<logs.length; i++) {
            assertEquals(logs[i], revs.next().getFullMessage());
        }
    }

    @Test
    public void testRebaseWithInteractiveEdit() throws Exception {
        try (Git git = Git.wrap(repository)) {

            this.gitMgr.createBranch(ws, "rebaseBranch");
            this.gitMgr.checkout(ws, "rebaseBranch", null);

            writeFileAndCommit(git, "rebase_one", "rebase_one", "rebase one");

            this.gitMgr.checkout(ws, "master", null);

            writeFileAndCommit(git, "one", "one", "one");

            this.gitMgr.checkout(ws, "rebaseBranch", null);

            // start rebase
            RebaseResponse response = this.gitMgr.rebase(ws, "master", true, false);

            assertEquals(RebaseResponse.Status.INTERACTIVE_PREPARED, response.getStatus());

            // update rebase_todo
            List<RebaseResponse.RebaseTodoLine> lines = response.getRebaseTodoLines();
            lines.get(0).setAction(RebaseResponse.RebaseTodoLine.Action.EDIT);
            assertEquals(1, lines.size());

            response = this.gitMgr.updateRebaseTodo(ws, lines);

            assertEquals(EDIT, response.getStatus());
            assertEquals(false, response.isSuccess());

            response = this.gitMgr.operateRebase(ws, RebaseOperation.CONTINUE);

            assertEquals(INTERACTIVE_EDIT, response.getStatus());
            assertEquals(false, response.isSuccess());

            response = this.gitMgr.operateRebase(ws, RebaseOperation.CONTINUE, "rebase_one_amend");

            assertEquals(OK, response.getStatus());
            assertEquals(true, response.isSuccess());
        }
    }

    @Test
    public void testRebaseWithInteractiveConflictAndEdit() throws Exception {
        try (Git git = Git.wrap(repository)) {

            this.gitMgr.createBranch(ws, "rebaseBranch");
            this.gitMgr.checkout(ws, "rebaseBranch", null);

            writeFileAndCommit(git, "one", "rebase_one", "rebase one");

            this.gitMgr.checkout(ws, "master", null);

            writeFileAndCommit(git, "one", "one", "one");

            this.gitMgr.checkout(ws, "rebaseBranch", null);

            // start rebase
            RebaseResponse response = this.gitMgr.rebase(ws, "master", true, false);

            assertEquals(RebaseResponse.Status.INTERACTIVE_PREPARED, response.getStatus());

            // update rebase_todo
            List<RebaseResponse.RebaseTodoLine> lines = response.getRebaseTodoLines();
            lines.get(0).setAction(RebaseResponse.RebaseTodoLine.Action.EDIT);
            assertEquals(1, lines.size());

            response = this.gitMgr.updateRebaseTodo(ws, lines);

            // if conflict and edit, status is conflict
            assertEquals(STOPPED, response.getStatus());

            // resolve conflict
            git.add().addFilepattern("one").call();

            response = this.gitMgr.operateRebase(ws, RebaseOperation.CONTINUE);

            assertEquals(INTERACTIVE_EDIT, response.getStatus());
            assertEquals(false, response.isSuccess());

            response = this.gitMgr.operateRebase(ws, RebaseOperation.CONTINUE, "rebase_one_amend");

            assertEquals(OK, response.getStatus());
            assertEquals(true, response.isSuccess());
        }
    }

    @Test(expected = UnmergedPathsException.class)
    public void testRebaseWithInteractiveConflictNotResolvedAndEdit() throws Exception {
        try (Git git = Git.wrap(repository)) {

            this.gitMgr.createBranch(ws, "rebaseBranch");
            this.gitMgr.checkout(ws, "rebaseBranch", null);

            writeFileAndCommit(git, "one", "rebase_one", "rebase one");

            this.gitMgr.checkout(ws, "master", null);

            writeFileAndCommit(git, "one", "one", "one");

            this.gitMgr.checkout(ws, "rebaseBranch", null);

            // start rebase
            RebaseResponse response = this.gitMgr.rebase(ws, "master", true, false);

            assertEquals(RebaseResponse.Status.INTERACTIVE_PREPARED, response.getStatus());

            // update rebase_todo
            List<RebaseResponse.RebaseTodoLine> lines = response.getRebaseTodoLines();
            lines.get(0).setAction(RebaseResponse.RebaseTodoLine.Action.EDIT);
            assertEquals(1, lines.size());

            response = this.gitMgr.updateRebaseTodo(ws, lines);

            // if conflict and edit, status is conflict
            assertEquals(STOPPED, response.getStatus());

            this.gitMgr.operateRebase(ws, RebaseOperation.CONTINUE);
        }
    }

    @Test
    public void testStateWithSafe() {
        RepositoryState state = this.gitMgr.state(ws);

        assertEquals(RepositoryState.SAFE, state);
    }

    @Test
    public void testStateWithNoChange() throws GitAPIException, IOException, GitOperationException {
        this.gitMgr.createBranch(ws, "rebase");
        this.gitMgr.checkout(ws, "rebase", null);

        RebaseResponse response = this.gitMgr.rebase(ws, "master", true, false);

        assertEquals(RebaseResponse.Status.INTERACTIVE_PREPARED, response.getStatus());

        RepositoryState state = this.gitMgr.state(ws);

        assertEquals(RepositoryState.SAFE, state);
    }

    @Test
    public void testStateWithConflict() throws Exception {
        try (Git git = Git.wrap(repository)) {

            this.gitMgr.createBranch(ws, "rebaseBranch");
            this.gitMgr.checkout(ws, "rebaseBranch", null);

            writeFileAndCommit(git, "one", "rebase_one", "rebase one");

            this.gitMgr.checkout(ws, "master", null);

            writeFileAndCommit(git, "one", "one", "one");

            this.gitMgr.checkout(ws, "rebaseBranch", null);

            RebaseResponse response = this.gitMgr.rebase(ws, "master", false, false);

            assertEquals(STOPPED, response.getStatus());

            RepositoryState state = this.gitMgr.state(ws);
            assertEquals(RepositoryState.REBASING_MERGE, state);
        }
    }

    @Test
    public void testForResetWithSoft() throws Exception {
        try (Git git = Git.wrap(repository)) {

            writeFileAndCommit(git, "testReset", "testReset", "testReset");

            this.gitMgr.reset(ws, "HEAD~1", ResetType.SOFT);

            Status status = git.status().call();

            assertEquals(1, status.getAdded().size());
            assertEquals(true, status.getAdded().contains("testReset"));
        }
    }

    @Test
    public void testForResetWithMixed() throws Exception {
        try (Git git = Git.wrap(repository)) {

            writeFileAndCommit(git, "testReset", "testReset", "testReset");

            this.gitMgr.reset(ws, "HEAD~1", ResetType.MIXED);

            Status status = git.status().call();

            assertEquals(1, status.getUntracked().size());
            assertEquals(true, status.getUntracked().contains("testReset"));
        }
    }

    @Test
    public void testForResetWithHard() throws Exception {
        try (Git git = Git.wrap(repository)) {

            writeFileAndCommit(git, "testReset", "testReset", "testReset");

            this.gitMgr.reset(ws, "HEAD~1", ResetType.HARD);

            Status status = git.status().call();

            assertTrue(status.isClean());
        }
    }

    @Test
    public void testForReadFileFromRev() throws IOException {

        String content = this.gitMgr.readFileFromRef(ws, "master", "README.md", false);

        assertEquals(content, "This is readme");

        content = this.gitMgr.readFileFromRef(ws, "branch1", "README.md", false);

        assertEquals(content, "branch1");
    }

    @Test
    public void testForReadFileFromRevWithAbsolutePath() throws IOException {

        String content = this.gitMgr.readFileFromRef(ws, "master", "/README.md", false);

        assertEquals(content, "This is readme");

        content = this.gitMgr.readFileFromRef(ws, "branch1", "/README.md", false);

        assertEquals(content, "branch1");
    }

    @Test(expected = GitInvalidPathException.class)
    public void testForReadFileFromRevWithNotExitFile() throws IOException, GitOperationException {
        this.gitMgr.readFileFromRef(ws, "master", "not_exist", false);
    }

    @Test(expected = GitInvalidRefException.class)
    public void testForReadFileFromRevWithNotExistRef() throws IOException, GitOperationException {
        this.gitMgr.readFileFromRef(ws, "not_exist_ref", "README.md", false);
    }

    @Test
    public void testForCreateLightTag() throws GitAPIException, IOException {
        Git git = Git.wrap(repository);

        gitMgr.createTag(ws, "test", null, null, false);

        List<Ref> refs = git.tagList().call();
        assertEquals(1, refs.size());
        assertEquals("refs/tags/test", refs.get(0).getName());
        assertEquals(repository.resolve("HEAD"), refs.get(0).getObjectId());
    }

    @Test
    public void testForCreateAnotationTag() throws GitAPIException, IOException {
        Git git = Git.wrap(repository);

        gitMgr.createTag(ws, "test", null, "test message", false);

        List<Ref> refs = git.tagList().call();
        assertEquals(1, refs.size());
        assertEquals("refs/tags/test", refs.get(0).getName());

        assertNotEquals(repository.resolve("HEAD"), refs.get(0).getObjectId());

        RevWalk walk = new RevWalk(repository);
        RevTag tag = walk.parseTag(refs.get(0).getObjectId());

        assertEquals("test message", tag.getFullMessage());
    }

    @Test
    public void testForCreateDuplicateTagAndFourceUpdate() throws GitAPIException, IOException {
        gitMgr.createTag(ws, "test", null, null, false);
        gitMgr.createTag(ws, "test", "branch1", null, true);
    }

    @Test(expected = JGitInternalException.class)
    public void testForCreateDuplicateTag() throws GitAPIException, IOException {
        gitMgr.createTag(ws, "test", null, null, false);
        gitMgr.createTag(ws, "test", "branch1", null, false);
    }

    @Test
    public void testForCreateTagWithWithRef() throws GitAPIException, IOException {
        gitMgr.createTag(ws, "test", "branch1", null, false);

        Git git = Git.wrap(repository);

        List<Ref> refs = git.tagList().call();
        assertEquals(1, refs.size());
        assertEquals("refs/tags/test", refs.get(0).getName());

        assertEquals(repository.resolve("branch1"), refs.get(0).getObjectId());
    }

    private RevCommit writeFileAndCommitWithAuthor(Git git, String authorName, String email, String fileName, String commitMessage,
                                                   String... lines) throws IOException, GitAPIException {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line);
            sb.append('\n');
        }
        writeTrashFile(fileName, sb.toString());
        git.add().addFilepattern(fileName).call();

        CommitCommand commitCommand = git.commit().setMessage(commitMessage);

        if (authorName != null && email != null) {
            return commitCommand.setAuthor(authorName, email).call();
        } else {
            return commitCommand.call();
        }
    }

    private RevCommit writeFileAndCommit(Git git, String fileName, String commitMessage,
                                         String... lines) throws IOException, GitAPIException {
        return writeFileAndCommitWithAuthor(git, null, null, fileName, commitMessage, lines);
    }

    protected File writeTrashFile(final String name, final String data)
            throws IOException {
        return JGitTestUtil.writeTrashFile(repository, name, data);
    }

    @Test
    @Ignore
    public void testGitClone() throws IOException, GitAPIException, NoSuchFieldException, IllegalAccessException, ExecutionException {

        when(wsRepo.findProjectBySpaceKey(ws.getSpaceKey())).thenReturn(ws.getProject());
        when(wsRepo.findBySpaceKey(ws.getSpaceKey())).thenReturn(ws.getWorkspaceEntity());

        Files.copy(new File("target/test-classes/ssh-keys/user_34380/id_rsa"), new File(ws.getKeyDir(), "id_rsa"));
        Files.copy(new File("target/test-classes/ssh-keys/user_34380/known_hosts"), new File(ws.getKeyDir(), "known_hosts"));

        FileUtils.cleanDirectory(ws.getWorkingDir());

        gitMgr.clone(ws);

        gitMgr.config(ws);
    }

}
