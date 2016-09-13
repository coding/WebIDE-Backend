/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model.exception;

/**
 * Created by vangie on 15/6/3.
 */
public class WorkspaceDeletedException extends WorkspaceMissingException {

    public WorkspaceDeletedException(String msg) {
        super(msg);
    }

    public WorkspaceDeletedException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
