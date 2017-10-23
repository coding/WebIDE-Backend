/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.web.controller;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import net.coding.ide.dto.BranchDTO;
import net.coding.ide.dto.DiffDTO;
import net.coding.ide.dto.FileDTO;
import net.coding.ide.model.*;
import net.coding.ide.model.exception.GitOperationException;
import net.coding.ide.model.exception.NotFoundException;
import net.coding.ide.service.GitManager;
import net.coding.ide.service.WorkspaceManager;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.filter.AuthorRevFilter;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;
import org.eclipse.jgit.revwalk.filter.OrRevFilter;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.util.StringUtils.commaDelimitedListToStringArray;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@Slf4j
@RestController
@RequestMapping(value = "/git", produces = "application/json; charset=utf-8")
public class GitController {
    @Autowired
    private GitManager gitMgr;

    @Autowired
    private WorkspaceManager wsMgr;

    @GetMapping("{spaceKey}")
    public CommitStatus status(@PathVariable("spaceKey") Workspace ws) throws GitAPIException {

        log.debug("Git status for spaceKey => {}", ws.getSpaceKey());

        return gitMgr.getStatus(ws);
    }

    @PostMapping(value = "{spaceKey}/commits", params = "files[]")
    public List<FileInfo> commit(@PathVariable("spaceKey") Workspace ws,
                                 @RequestParam(value = "files[]") String files,
                                 @RequestParam String message) throws Exception {

        log.debug("Git commit for spaceKey => {}, files[] => {}, message => {}", ws.getSpaceKey(), files, message);

        List<String> fileList = Arrays.asList(commaDelimitedListToStringArray(files));

        List<String> commitFiles = gitMgr.commit(ws, fileList, message);

        List<FileInfo> fileInfos = Lists.newArrayList();
        for (String file : commitFiles) {
            fileInfos.add(wsMgr.getFileInfo(ws, file));
        }

        return fileInfos;
    }

    @PostMapping("{spaceKey}/commits")
    public List<FileInfo> commitAll(@PathVariable("spaceKey") Workspace ws,
                                    @RequestParam String message) throws Exception {

        log.debug("Git commit all for spaceKey => {}, message => {}", ws.getSpaceKey(), message);

        List<String> commitFiles = gitMgr.commitAll(ws, message);

        List<FileInfo> fileInfos = Lists.newArrayList();
        for (String file : commitFiles) {
            fileInfos.add(wsMgr.getFileInfo(ws, file));
        }

        return fileInfos;
    }


    @GetMapping(value = "{spaceKey}/commits", params = {"oldRef", "newRef"})
    public DiffDTO diff(@PathVariable("spaceKey") Workspace ws,
                        @RequestParam String path,
                        @RequestParam String oldRef,
                        @RequestParam String newRef) throws IOException, GitAPIException {

        log.debug("Git diff for spaceKey => {}, path => {}, oldRef => {}, newRef => {}", ws.getSpaceKey(),
                path, oldRef, newRef);

        String diff = gitMgr.diff(ws, path, oldRef, newRef);

        return DiffDTO.of(diff);
    }

    @ResponseStatus(NO_CONTENT)
    @PostMapping("{spaceKey}/submodules/sync")
    public void sync(@PathVariable("spaceKey") Workspace ws) throws GitAPIException {

        log.debug("Git sync for spaceKey => {}", ws.getSpaceKey());

        gitMgr.sync(ws);
    }

    @GetMapping("{spaceKey}/push")
    public PushCommits getPushCommits(@PathVariable("spaceKey") Workspace ws) throws IOException, GitAPIException, GitOperationException {

        log.debug("Get git push commits for spaceKey => {}", ws.getSpaceKey());

        return gitMgr.getPushCommits(ws);
    }

    /**
     * Response format:
     * {
     *      "code": 0,
     *      "result": {
     *      "nothingToPush": false,
     *      "ok": false,
     *      "updates": [
     *          {"remoteRefName": "refs/heads/master",
     *          "status": "OK"},
     *          {"remoteRefName": "refs/heads/edge",
     *          "status": "REJECTED_NONFASTFORWARD"},
     *          {"remoteRefName": "refs/heads/dev",
     *          "status": "REJECTED_OTHER_REASON",
     *          "message": "..."}
     *      ]
     * }
     */
    @PostMapping("{spaceKey}/push")
    public PushResponse push(@PathVariable("spaceKey") Workspace ws,
                             @RequestParam(required = false) String ref) throws GitAPIException, IOException, GitOperationException {

        log.debug("Git push for spaceKey => {}, ref => {}", ws.getSpaceKey(), ref);

        PushResponse response;
        if (ref == null) {
            response = gitMgr.push(ws);
        } else {
            response = gitMgr.push(ws, ref);
        }

        return response;
    }

    @PostMapping(value = "{spaceKey}/push", params = "all")
    public PushResponse pushAll(@PathVariable("spaceKey") Workspace ws) throws GitAPIException, IOException, GitOperationException {

        log.debug("Git push all for spaceKey => {}", ws.getSpaceKey());

        return gitMgr.pushAll(ws);
    }

    @PostMapping("{spaceKey}/pull")
    public ResponseEntity pull(@PathVariable("spaceKey") Workspace ws) throws GitAPIException, IOException {

        log.debug("Git pull for spaceKey => {}", ws.getSpaceKey());

        if (gitMgr.pull(ws)) {
            return new ResponseEntity<>(NO_CONTENT);
        } else {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * response notes:
     *      code -> 0, success
     *      code -> 1, failure, see {@code data.status}
     *      code -> -1, other errors
     */
    @PostMapping("{spaceKey}/checkout")
    public CheckoutResponse checkout(@PathVariable("spaceKey") Workspace ws,
                                     @RequestParam String name,
                                     @RequestParam(required = false) String startPoint) throws GitAPIException, IOException, GitOperationException {

        log.debug("Git checkout for spaceKey => {}, name => {}, startPoint => {}", ws.getSpaceKey(), name, startPoint);

        return gitMgr.checkout(ws, name, startPoint);
    }

    /**
     * 显示合并冲突文件
     */
    @GetMapping("{spaceKey}/conflicts")
    public ConflictFile queryConflictFile(@PathVariable("spaceKey") Workspace ws,
                                          @RequestParam String path,
                                          @RequestParam(defaultValue = "false") boolean base64) throws Exception {

        return gitMgr.queryConflictFile(ws, path, base64);
    }


    @DeleteMapping("{spaceKey}/conflicts")
    public ResponseEntity deleteConflictFile(@PathVariable("spaceKey") Workspace ws,
                                             @RequestParam String path) throws Exception {

        gitMgr.deleteConflictFile(ws, path);

        return new ResponseEntity(NO_CONTENT);
    }

    @PostMapping("{spaceKey}/conflicts")
    public ResponseEntity resolveConflictFile(@PathVariable("spaceKey") Workspace ws,
                                              @RequestParam String path,
                                              @RequestParam String content,
                                              @RequestParam(defaultValue = "false") boolean base64) throws Exception {

        gitMgr.resolveConflictFile(ws, path, content, base64);

        return new ResponseEntity(NO_CONTENT);
    }

    @PostMapping("{spaceKey}/merge")
    public MergeResponse merge(@PathVariable("spaceKey") Workspace ws,
                               @RequestParam String name) throws GitAPIException, IOException {

        return gitMgr.merge(ws, name);
    }

    @PostMapping("{spaceKey}/fetch")
    public ResponseEntity fetch(@PathVariable("spaceKey") Workspace ws,
                                @RequestParam(defaultValue = "true") boolean prune) throws GitAPIException {

        log.debug("Git fetch for spaceKey => {}", ws.getSpaceKey());

        gitMgr.fetch(ws, prune);

        return new ResponseEntity(NO_CONTENT);
    }

    @GetMapping("{spaceKey}/tags")
    public List<String> getTags(@PathVariable("spaceKey") Workspace ws) throws GitAPIException {
        List<String> tags = gitMgr.getTags(ws);

        return tags;
    }

    @GetMapping("{spaceKey}/branch")
    public BranchDTO getBranch(@PathVariable("spaceKey") Workspace ws) throws IOException {

        log.debug("Git get branch for spaceKey => {}", ws.getSpaceKey());

        return BranchDTO.of(gitMgr.getBranch(ws));
    }

    @GetMapping("{spaceKey}/branches")
    public Branches getBranches(@PathVariable("spaceKey") Workspace ws) throws GitAPIException, IOException {

        log.debug("Git get branches for spaceKey => {}", ws.getSpaceKey());

        return gitMgr.getBranches(ws);
    }

    @PostMapping("{spaceKey}/branches")
    public Branches createBranch(@PathVariable("spaceKey") Workspace ws,
                                 @RequestParam String branchName) throws GitAPIException, IOException, GitOperationException {

        log.debug("Git create branch for spaceKey => {}, branchName => {}", ws.getSpaceKey(), branchName);

        gitMgr.createBranch(ws, branchName);

        gitMgr.checkout(ws, branchName, null);

        return gitMgr.getBranches(ws);
    }


    @DeleteMapping(value = "{spaceKey}/branches")
    public Branches deleteBranch(@PathVariable("spaceKey") Workspace ws,
                                 @RequestParam String branchName) throws GitAPIException, IOException, GitOperationException {
        log.debug("Git delete branch for spaceKey => {}, branchName => {}", ws.getSpaceKey(), branchName);

        if (!gitMgr.hasBranch(ws, branchName)) {
            throw new NotFoundException(String.format("Ref %s not found", branchName));
        }

        gitMgr.deleteBranch(ws, branchName);

        return gitMgr.getBranches(ws);
    }

    @PostMapping("{spaceKey}/stash")
    public ResponseEntity createStash(@PathVariable("spaceKey") Workspace ws,
                                      @RequestParam String message,
                                      @RequestParam(defaultValue = "false") boolean includeUntracked) throws GitAPIException, GitOperationException {
        log.debug("Git delete branch for spaceKey => {}, includeUntracked => {}, message => {}",
                ws.getSpaceKey(), message);

        gitMgr.createStash(ws, includeUntracked, message);

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @GetMapping("{spaceKey}/read")
    public FileDTO read(@PathVariable("spaceKey") Workspace ws,
                        @RequestParam String ref,
                        @RequestParam String path,
                        @RequestParam String encoding,
                        @RequestParam(defaultValue = "false") boolean base64) throws IOException {

        final String finalEncoding = StringUtils.isBlank(encoding) ?  ws.getEncoding() : encoding;

        String content = gitMgr.readFileFromRef(ws, ref, path, finalEncoding, base64);

        return FileDTO.of(path, content, base64);
    }

    @RequestMapping(value = "{spaceKey}/logs", method = GET)
    public List<GitLog> log(@PathVariable("spaceKey") Workspace ws,
                            @RequestParam(required = false, name = "path") String[] paths,
                            @RequestParam(required = false, name = "ref") String[] refs,
                            @RequestParam(required = false, name = "author") String[] authors,
                            @RequestParam(required = false) Long since,
                            @RequestParam(required = false) Long until,
                            Pageable pageable) throws GitAPIException, IOException {

        return gitMgr.log(ws,
                refs,
                paths,
                authors,
                since,
                until,
                pageable);
    }

    @RequestMapping(value = "{spaceKey}/refs", method = GET)
    public List<GitRef> refs(@PathVariable("spaceKey") Workspace ws) throws GitAPIException, IOException {
        return gitMgr.refs(ws);
    }

    @RequestMapping(value = "{spaceKey}/blame", method = GET)
    public List<GitBlame> blame(@PathVariable("spaceKey") Workspace ws,
                                @RequestParam String path) throws AccessDeniedException, GitAPIException {
        return gitMgr.blame(ws, path);
    }

    @GetMapping(value = "{spaceKey}/commits", params = "ref")
    public List<DiffEntry> getDiffEntryForCommit(@PathVariable("spaceKey") Workspace ws,
                                                 @RequestParam String ref) throws IOException, GitAPIException {

        return gitMgr.getDiffEntryForCommit(ws, ref);
    }

    @RequestMapping(value = "{spaceKey}/stash/apply", method = POST)
    public ResponseEntity applyStash(@PathVariable("spaceKey") Workspace ws,
                                     @RequestParam String stashRef,
                                     @RequestParam(defaultValue = "false") Boolean pop,
                                     @RequestParam(defaultValue = "false") Boolean applyIndex) throws GitAPIException {
        log.debug("Git delete branch for spaceKey => {}, stashRef => {}, pop => {}, applyIndex => {}",
                ws.getSpaceKey(), stashRef, pop, applyIndex);

        gitMgr.applyStash(ws, stashRef, applyIndex, pop);

        return new ResponseEntity<>(NO_CONTENT);
    }

    @PostMapping("{spaceKey}/stash/checkout")
    public CheckoutResponse checkoutStash(@PathVariable("spaceKey") Workspace ws,
                                          @RequestParam String stashRef,
                                          @RequestParam String branch) throws GitAPIException, IOException, GitOperationException {

        return gitMgr.checkoutStash(ws, stashRef, branch);
    }

    @DeleteMapping("{spaceKey}/stash")
    public ResponseEntity dropStash(@PathVariable("spaceKey") Workspace ws,
                                    @RequestParam(required = false) String stashRef,
                                    @RequestParam(defaultValue = "false") Boolean all) throws GitAPIException {
        log.debug("Git delete branch for spaceKey => {}, stashRef => {}",
                ws.getSpaceKey(), stashRef);

        if (!all) {
            gitMgr.dropStash(ws, stashRef);
        } else {
            gitMgr.dropAllStash(ws);
        }

        return new ResponseEntity(NO_CONTENT);
    }

    @GetMapping("{spaceKey}/stash")
    public ListStashResponse listStash(@PathVariable("spaceKey") Workspace ws) throws GitAPIException {
        log.debug("Git list stash for spaceKey => {}", ws.getSpaceKey());

        return gitMgr.listStash(ws);
    }

    @PostMapping("{spaceKey}/rebase")
    public RebaseResponse rebase(@PathVariable("spaceKey") Workspace ws,
                                 @RequestParam(required = false) String branch,
                                 @RequestParam String upstream,
                                 @RequestParam(defaultValue = "true") Boolean interactive,
                                 @RequestParam(defaultValue = "false") Boolean preserve) throws GitAPIException, IOException, GitOperationException {

        RebaseResponse response;

        if (isBlank(branch)) {
            response = gitMgr.rebase(ws, upstream, interactive, preserve);
        } else {
            response = gitMgr.rebase(ws, branch, upstream, interactive, preserve);
        }

        return response;
    }

    @PostMapping("{spaceKey}/rebase/update")
    public RebaseResponse updateRebaseTodo(@PathVariable("spaceKey") Workspace ws,
                                           @RequestBody List<RebaseResponse.RebaseTodoLine> lines) throws IOException, GitAPIException, GitOperationException {

        return gitMgr.updateRebaseTodo(ws, lines);
    }

    @PostMapping("{spaceKey}/rebase/operate")
    public RebaseResponse operateRebase(@PathVariable("spaceKey") Workspace ws,
                                        @RequestParam RebaseOperation operation,
                                        @RequestParam(required = false) String message) throws GitAPIException, IOException {
        if (isNotBlank(message)) {
            return gitMgr.operateRebase(ws, operation, message);
        } else {
            return gitMgr.operateRebase(ws, operation);
        }
    }

    @GetMapping(value = "{spaceKey}", params = "state")
    public RepositoryState state(@PathVariable("spaceKey") Workspace ws) {
        return gitMgr.state(ws);
    }

    @PostMapping("{spaceKey}/reset")
    public ResponseEntity reset(@PathVariable("spaceKey") Workspace ws,
                                @RequestParam String ref,
                                @RequestParam ResetType resetType) throws GitAPIException {
        gitMgr.reset(ws, ref, resetType);

        return new ResponseEntity<>(NO_CONTENT);
    }

    @PostMapping("{spaceKey}/tags")
    public ResponseEntity tag(@PathVariable("spaceKey") Workspace ws,
                              @RequestParam String tagName,
                              @RequestParam(required = false) String ref,
                              @RequestParam(required = false) String message,
                              @RequestParam(defaultValue = "false") Boolean force) throws GitAPIException, IOException {

        gitMgr.createTag(ws, tagName, ref, message, force);

        return new ResponseEntity(NO_CONTENT);
    }
}
