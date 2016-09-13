/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tan on 16-3-11.
 */
@Data
public class MergeResponse {

    public enum Status {
        FAST_FORWARD,
        FAST_FORWARD_SQUASHED,
        ALREADY_UP_TO_DATE,
        FAILED,
        MERGED,
        MERGED_SQUASHED,
        MERGED_SQUASHED_NOT_COMMITTED,
        CONFLICTING,
        ABORTED,
        MERGED_NOT_COMMITTED,
        NOT_SUPPORTED,
        CHECKOUT_CONFLICT
    }

    private boolean success;

    private Status status;

    private List<String> failingPaths;

    public void addFailingPath(String failingPath) {
        if (this.failingPaths == null) {
            this.failingPaths = new ArrayList<String>();
        }

        this.failingPaths.add(failingPath);
    }
}
