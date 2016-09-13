/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model.exception;

/**
 * Created by vangie on 15/7/21.
 */
public class GitCloneAuthFailException extends RuntimeException {

    public GitCloneAuthFailException(String message, Throwable cause) {
        super(message, cause);
    }
}
