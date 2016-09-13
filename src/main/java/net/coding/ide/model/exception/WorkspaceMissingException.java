/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model.exception;

/**
 * Created by vangie on 15/3/9.
 */
public class WorkspaceMissingException extends WorkspaceException {

    public WorkspaceMissingException(String msg) {
        super(msg);
    }

    public WorkspaceMissingException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
