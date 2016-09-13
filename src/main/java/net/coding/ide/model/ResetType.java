/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;

/**
 * Created by tan on 16-4-11.
 */
public enum ResetType {
    /**
     * Just change the ref, the index and workdir are not changed.
     */
    SOFT,

    /**
     * Change the ref and the index, the workdir is not changed.
     */
    MIXED,

    /**
     * Change the ref, the index and the workdir
     */
    HARD
}
