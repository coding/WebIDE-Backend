/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.service;

import net.coding.ide.model.*;
import net.coding.ide.model.exception.GitOperationException;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Created by vangie on 14/12/29.
 */
public interface GitManager {

    GitStatus status(Workspace ws, Path path) throws Exception;

    boolean clone(Workspace ws) throws IOException, GitAPIException;

    void config(Workspace ws) throws IOException;

    CommitStatus getStatus(Workspace ws) throws GitAPIException;

    MergeResponse merge(Workspace ws, String branch) throws GitAPIException, IOException;

    void createStash(Workspace ws, String message) throws GitAPIException, GitOperationException;

    void applyStash(Workspace ws, String stashRef, boolean applyIndex, boolean pop) throws GitAPIException;

    CheckoutResponse checkoutStash(Workspace ws, String stashRef, String branch) throws IOException, GitAPIException, GitOperationException;

    ListStashResponse listStash(Workspace ws) throws GitAPIException;

    void dropStash(Workspace ws, String stashRef) throws GitAPIException;

    void dropAllStash(Workspace ws) throws GitAPIException;

    ConflictFile queryConflictFile(Workspace ws, String path, boolean base64) throws Exception;

    void deleteConflictFile(Workspace ws, String path) throws Exception;

    void resolveConflictFile(Workspace ws, String path, String content, boolean base64) throws Exception;

    List<String> commit(Workspace ws, List<String> files, String message) throws GitAPIException, IOException;

    List<String> commitAll(Workspace ws, String message) throws GitAPIException, IOException;

    String diff(Workspace ws, String path, String oldRef, String newRef) throws IOException, GitAPIException;

    void sync(Workspace ws) throws GitAPIException;

    PushCommits getPushCommits(Workspace ws) throws IOException, GitAPIException, GitOperationException;

    PushCommits getPushCommits(Workspace ws, String branch) throws IOException, GitAPIException, GitOperationException;

    PushResponse push(Workspace ws) throws GitAPIException, IOException, GitOperationException;

    PushResponse push(Workspace ws, String ref) throws GitAPIException, IOException, GitOperationException;

    PushResponse pushAll(Workspace ws) throws GitAPIException, IOException, GitOperationException;

    boolean pull(Workspace ws) throws GitAPIException, IOException;

    CheckoutResponse checkout(Workspace ws, String name, String startPoint) throws GitAPIException, IOException, GitOperationException;

    void fetch(Workspace ws) throws GitAPIException;

    void fetch(Workspace ws, boolean prune) throws GitAPIException;

    /**
     * Retrieve the current branch that HEAD points to.
     *
     * @return branch name
     */
    String getBranch(Workspace ws) throws IOException;

    List<String> getLocalBranches(Workspace ws) throws GitAPIException;

    List<String> getRemoteBranches(Workspace ws) throws GitAPIException;

    Branches getBranches(Workspace ws) throws GitAPIException, IOException;

    void createBranch(Workspace ws, String branchName) throws GitAPIException;

    void deleteBranch(Workspace ws, String branchName) throws GitAPIException, IOException, GitOperationException;

    boolean hasBranch(Workspace ws, String branch) throws GitAPIException;

    List<String> getTags(Workspace ws) throws GitAPIException;

    void createTag(Workspace ws, String tagName, String ref, String message, boolean force) throws GitAPIException, IOException;

    void deleteTag(Workspace ws, String tagName) throws GitAPIException;

    boolean checkIgnore(InputStream in, String path, boolean isDir);

    List<DiffEntry> getDiffEntryForCommit(Workspace ws, String commitId) throws IOException, GitAPIException;

    RebaseResponse rebase(Workspace ws, String upstream, boolean interactive, boolean preserve) throws GitAPIException, IOException, GitOperationException;

    RebaseResponse rebase(Workspace ws, String branch, String upstream, boolean interactive, boolean preserve) throws GitAPIException, IOException, GitOperationException;

    RebaseResponse updateRebaseTodo(Workspace ws, List<RebaseResponse.RebaseTodoLine> lines) throws IOException, GitOperationException, GitAPIException;

    RebaseResponse operateRebase(Workspace ws, RebaseOperation operation) throws GitAPIException, IOException;

    RebaseResponse operateRebase(Workspace ws, RebaseOperation operation, String message) throws GitAPIException, IOException;

    RepositoryState state(Workspace ws);

    String readFileFromRef(Workspace ws, String ref, String path, boolean base64) throws IOException;

    void reset(Workspace ws, String ref, ResetType resetType) throws GitAPIException;
}
