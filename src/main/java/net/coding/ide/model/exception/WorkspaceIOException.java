/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model.exception;

/**
 * Created by mingshun on 15-9-23.
 */
public class WorkspaceIOException extends RuntimeException {

    public WorkspaceIOException(String message) {
        super(message);
    }

    public WorkspaceIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public WorkspaceIOException(Throwable cause) {
        super(cause);
    }
}
