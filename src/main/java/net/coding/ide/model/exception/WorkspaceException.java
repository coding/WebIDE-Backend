/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model.exception;

/**
 * Created by vangie on 15/6/3.
 */
public class WorkspaceException extends RuntimeException {

    public WorkspaceException() {

    }

    public WorkspaceException(String msg) {
        super(msg);
    }

    public WorkspaceException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
