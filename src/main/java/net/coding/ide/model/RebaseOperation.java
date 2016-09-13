/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;

/**
 * Created by tan on 16-4-8.
 */
public enum RebaseOperation {
    /**
     * Continues after a conflict resolution
     */
    CONTINUE,
    /**
     * Skips the "current" commit
     */
    SKIP,
    /**
     * Aborts and resets the current rebase
     */
    ABORT,
    /**
     * Starts processing steps
     * @since 3.2
     */
    PROCESS_STEPS
}
