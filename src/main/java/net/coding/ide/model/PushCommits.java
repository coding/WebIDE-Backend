/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;

import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;

/**
 * Created by mingshun on 3/22/15.
 */
@Data
public class PushCommits {
    private String localRef;
    private String remote;
    private String remoteRef;
    private List<Commit> commits;

    public void addCommit(Commit commit) {
        if (this.commits == null) {
            this.commits = Lists.newArrayList();
        }

        this.commits.add(commit);
    }

    @Data
    public static class Commit {
        private String shortMessage;
        private String fullMessage;
        private String sha;
        private List<DiffEntry> diffEntries;
    }
}
