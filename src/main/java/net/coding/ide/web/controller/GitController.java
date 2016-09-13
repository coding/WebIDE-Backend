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
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
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

    @RequestMapping(value = "{spaceKey}", method = GET)
    public CommitStatus status(@PathVariable("spaceKey") Workspace ws) throws GitAPIException {

        log.debug("Git status for spaceKey => {}", ws.getSpaceKey());

        return gitMgr.getStatus(ws);
    }

    @RequestMapping(value = "{spaceKey}", method = POST)
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

    @RequestMapping(value = "{spaceKey}", method = POST, params = "all")
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

    @RequestMapping(value = "{spaceKey}/diff", method = GET)
    public DiffDTO diff(@PathVariable("spaceKey") Workspace ws,
                        @RequestParam String path,
                        @RequestParam String oldRef,
                        @RequestParam String newRef) throws IOException, GitAPIException {

        log.debug("Git diff for spaceKey => {}, path => {}, oldRef => {}, newRef => {}", ws.getSpaceKey(),
                path, oldRef, newRef);

        String diff = gitMgr.diff(ws, path, oldRef, newRef);

        return DiffDTO.of(diff);
    }

    @RequestMapping(value = "{spaceKey}/sync", method = POST)
    public ResponseEntity sync(@PathVariable("spaceKey") Workspace ws) throws GitAPIException {

        log.debug("Git sync for spaceKey => {}", ws.getSpaceKey());

        gitMgr.sync(ws);

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "{spaceKey}/push", method = GET)
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
    @RequestMapping(value = "{spaceKey}/push", method = POST)
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

    @RequestMapping(value = "{spaceKey}/push", method = POST, params = "all")
    public PushResponse pushAll(@PathVariable("spaceKey") Workspace ws) throws GitAPIException, IOException, GitOperationException {

        log.debug("Git push all for spaceKey => {}", ws.getSpaceKey());

        return gitMgr.pushAll(ws);
    }

    @RequestMapping(value = "{spaceKey}/pull", method = POST)
    public ResponseEntity pull(@PathVariable("spaceKey") Workspace ws) throws GitAPIException, IOException {

        log.debug("Git pull for spaceKey => {}", ws.getSpaceKey());

        boolean successful = gitMgr.pull(ws);

        if (successful) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
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
    @RequestMapping(value = "{spaceKey}/checkout", method = POST)
    public CheckoutResponse checkout(@PathVariable("spaceKey") Workspace ws,
                               @RequestParam String name,
                               @RequestParam(required = false) String startPoint) throws GitAPIException, IOException, GitOperationException {

        log.debug("Git checkout for spaceKey => {}, name => {}, startPoint => {}", ws.getSpaceKey(), name, startPoint);

        return gitMgr.checkout(ws, name, startPoint);
    }

    /**
     * 显示合并冲突文件
     */
    @RequestMapping(value = "{spaceKey}/conflicts", method = GET)
    public ConflictFile queryConflictFile(@PathVariable("spaceKey") Workspace ws,
                                          @RequestParam String path,
                                          @RequestParam(defaultValue = "false") boolean base64) throws Exception {

        return gitMgr.queryConflictFile(ws, path, base64);
    }

    @RequestMapping(value = "{spaceKey}/conflicts", method = DELETE)
    public ResponseEntity deleteConflictFile(@PathVariable("spaceKey") Workspace ws,
                                             @RequestParam String path) throws Exception {

        gitMgr.deleteConflictFile(ws, path);

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "{spaceKey}/conflicts", method = POST)
    public ResponseEntity resolveConflictFile(@PathVariable("spaceKey") Workspace ws,
                                              @RequestParam String path,
                                              @RequestParam String content,
                                              @RequestParam(defaultValue = "false") boolean base64) throws Exception {

        gitMgr.resolveConflictFile(ws, path, content, base64);

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "{spaceKey}/merge", method = POST)
    public MergeResponse merge(@PathVariable("spaceKey") Workspace ws,
                            @RequestParam String name) throws GitAPIException, IOException {

        return gitMgr.merge(ws, name);
    }

    @RequestMapping(value = "{spaceKey}/fetch", method = POST)
    public ResponseEntity fetch(@PathVariable("spaceKey") Workspace ws,
                                @RequestParam(defaultValue = "true") boolean prune) throws GitAPIException {

        log.debug("Git fetch for spaceKey => {}", ws.getSpaceKey());

        gitMgr.fetch(ws, prune);

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "{spaceKey}/tags", method = GET)
    public List<String> getTags(@PathVariable("spaceKey") Workspace ws) throws GitAPIException {
        List<String> tags = gitMgr.getTags(ws);

        return tags;
    }

    @RequestMapping(value = "{spaceKey}/branch", method = GET)
    public BranchDTO getBranch(@PathVariable("spaceKey") Workspace ws) throws IOException {

        log.debug("Git get branch for spaceKey => {}", ws.getSpaceKey());

        return BranchDTO.of(gitMgr.getBranch(ws));
    }

    @RequestMapping(value = "{spaceKey}/branches", method = GET)
    public Branches getBranches(@PathVariable("spaceKey") Workspace ws) throws GitAPIException, IOException {

        log.debug("Git get branches for spaceKey => {}", ws.getSpaceKey());

        return gitMgr.getBranches(ws);
    }

    @RequestMapping(value = "{spaceKey}/branches", method = POST)
    public Branches createBranch(@PathVariable("spaceKey") Workspace ws,
                                 @RequestParam String branchName) throws GitAPIException, IOException, GitOperationException {

        log.debug("Git create branch for spaceKey => {}, branchName => {}", ws.getSpaceKey(), branchName);

        gitMgr.createBranch(ws, branchName);

        gitMgr.checkout(ws, branchName, null);

        return gitMgr.getBranches(ws);
    }

    @RequestMapping(value = "{spaceKey}/branches/{branchName}", method = DELETE)
    public Branches deleteBranch(@PathVariable("spaceKey") Workspace ws,
                                 @PathVariable String branchName) throws GitAPIException, IOException, GitOperationException {

        log.debug("Git delete branch for spaceKey => {}, branchName => {}", ws.getSpaceKey(), branchName);

        if (!gitMgr.hasBranch(ws, branchName)) {
            throw new NotFoundException(String.format("Ref %s not found", branchName));
        }

        gitMgr.deleteBranch(ws, branchName);

        return gitMgr.getBranches(ws);
    }

    @RequestMapping(value = "{spaceKey}/stash", method = POST)
    public ResponseEntity createStash(@PathVariable("spaceKey") Workspace ws,
                                      @RequestParam String message) throws GitAPIException, GitOperationException {
        log.debug("Git delete branch for spaceKey => {}, includeUntracked => {}, message => {}",
                ws.getSpaceKey(), message);

        gitMgr.createStash(ws, message);

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "{spaceKey}/read", method = GET)
    public FileDTO read(@PathVariable("spaceKey") Workspace ws,
                        @RequestParam String ref,
                        @RequestParam String path,
                        @RequestParam(defaultValue = "false") boolean base64) throws IOException {
        String content = gitMgr.readFileFromRef(ws, ref, path, base64);

        return FileDTO.of(path, content, base64);
    }

    @RequestMapping(value = "{spaceKey}/diff", method = GET, params = "ref")
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

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "{spaceKey}/stash/checkout", method = POST)
    public CheckoutResponse checkoutStash(@PathVariable("spaceKey") Workspace ws,
                                          @RequestParam String stashRef,
                                          @RequestParam String branch) throws GitAPIException, IOException, GitOperationException {

        return gitMgr.checkoutStash(ws, stashRef, branch);
    }

    @RequestMapping(value = "{spaceKey}/stash", method = DELETE)
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

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "{spaceKey}/stash", method = GET)
    public ListStashResponse listStash(@PathVariable("spaceKey") Workspace ws) throws GitAPIException {
        log.debug("Git list stash for spaceKey => {}", ws.getSpaceKey());

        return gitMgr.listStash(ws);
    }

    @RequestMapping(value = "{spaceKey}/rebase", method = POST)
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

    @RequestMapping(value = "{spaceKey}/rebase/update", method = POST)
    public RebaseResponse updateRebaseTodo(@PathVariable("spaceKey") Workspace ws,
                                           @RequestBody List<RebaseResponse.RebaseTodoLine> lines) throws IOException, GitAPIException, GitOperationException {

        return gitMgr.updateRebaseTodo(ws, lines);
    }

    @RequestMapping(value = "{spaceKey}/rebase/operate", method = POST)
    public RebaseResponse operateRebase(@PathVariable("spaceKey") Workspace ws,
                                    @RequestParam RebaseOperation operation,
                                    @RequestParam(required = false) String message) throws GitAPIException, IOException {
        if (isNotBlank(message)) {
            return gitMgr.operateRebase(ws, operation, message);
        } else {
            return gitMgr.operateRebase(ws, operation);
        }
    }

    @RequestMapping(value = "{spaceKey}", method = GET, params = "state")
    public RepositoryState state(@PathVariable("spaceKey") Workspace ws) {
        return gitMgr.state(ws);
    }

    @RequestMapping(value = "{spaceKey}/reset", method = POST)
    public ResponseEntity reset(@PathVariable("spaceKey") Workspace ws,
                                @RequestParam String ref,
                                @RequestParam ResetType resetType) throws GitAPIException {
        gitMgr.reset(ws, ref, resetType);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "{spaceKey}/tags", method = POST)
    public ResponseEntity tag(@PathVariable("spaceKey") Workspace ws,
                              @RequestParam String tagName,
                              @RequestParam(required = false) String ref,
                              @RequestParam(required = false) String message,
                              @RequestParam(defaultValue = "false") Boolean force) throws GitAPIException, IOException {

        gitMgr.createTag(ws, tagName, ref, message, force);

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }
}

