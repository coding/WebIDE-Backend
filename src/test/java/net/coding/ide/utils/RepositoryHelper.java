/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.utils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class RepositoryHelper {

    public static Repository createRepository(File repo) throws IOException, GitAPIException {
        if (!repo.exists()) {
            Files.createDirectory(repo.toPath());
        }

        Repository repository = FileRepositoryBuilder.create(new File(repo, ".git"));
        repository.create();

        return repository;
    }

    @FunctionalInterface
    public interface BranchAction {
        void apply(Git git, String branch) throws IOException, GitAPIException;
    }

    public static void createTestBranch(Repository repository, String branch, BranchAction action) throws IOException, GitAPIException {
        Git git = new Git(repository);

        String origin = repository.getBranch();

        git.checkout().setName(branch).setCreateBranch(true).call();

        action.apply(git, branch);

        git.checkout().setName(origin).call();
    }
}
