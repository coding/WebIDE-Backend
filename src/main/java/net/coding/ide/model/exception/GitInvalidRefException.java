/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model.exception;

/**
 * Created by mingshun on 15-9-14.
 */
public class GitInvalidRefException extends RuntimeException {
    public GitInvalidRefException(String message) {
        super(message);
    }
}
