/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;


public enum GitStatus {

    CONFLICTION, // the file is conflicting

    ADDED,      // the staged file that has been added
    CHANGED,    // the staged file that has been modified
    REMOVED,    // the staged file that has been deleted

    MODIFIED,   // the unstaged file that has been modified
    MISSING,    // the unstaged file that has been deleted

    UNTRACKED,  // the file has not been tracked

    IGNORED,    // the file has been ignored

    CLEAN,       // the file has not been changed

    NONE        // the directory git status is NONE
}
