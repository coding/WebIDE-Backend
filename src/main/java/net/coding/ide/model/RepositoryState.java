/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;

/**
 * Created by tan on 16-4-11.
 *
 * @see org.eclipse.jgit.lib.RepositoryState
 */
public enum  RepositoryState {
    /** Has no work tree and cannot be used for normal editing. */
    BARE,

    /**
     * A safe state for working normally
     * */
    SAFE,

    /** An unfinished merge. Must resolve or reset before continuing normally
     */
    MERGING,

    /**
     * An merge where all conflicts have been resolved. The index does not
     * contain any unmerged paths.
     */
    MERGING_RESOLVED,

    /** An unfinished cherry-pick. Must resolve or reset before continuing normally
     */
    CHERRY_PICKING,

    /**
     * A cherry-pick where all conflicts have been resolved. The index does not
     * contain any unmerged paths.
     */
    CHERRY_PICKING_RESOLVED,

    /** An unfinished revert. Must resolve or reset before continuing normally
     */
    REVERTING,

    /**
     * A revert where all conflicts have been resolved. The index does not
     * contain any unmerged paths.
     */
    REVERTING_RESOLVED,

    /**
     * An unfinished rebase or am. Must resolve, skip or abort before normal work can take place
     */
    REBASING,

    /**
     * An unfinished rebase. Must resolve, skip or abort before normal work can take place
     */
    REBASING_REBASING,

    /**
     * An unfinished apply. Must resolve, skip or abort before normal work can take place
     */
    APPLY,

    /**
     * An unfinished rebase with merge. Must resolve, skip or abort before normal work can take place
     */
    REBASING_MERGE,

    /**
     * An unfinished interactive rebase. Must resolve, skip or abort before normal work can take place
     */
    REBASING_INTERACTIVE,

    /**
     * Bisecting being done. Normal work may continue but is discouraged
     */
    BISECTING
}
