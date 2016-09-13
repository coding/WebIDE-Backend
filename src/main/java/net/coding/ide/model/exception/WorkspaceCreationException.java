/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model.exception;

/**
 * Created by vangie on 15/3/9.
 */
public class WorkspaceCreationException extends WorkspaceException {
    public WorkspaceCreationException(String msg) {
        super(msg);
    }

    public WorkspaceCreationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
