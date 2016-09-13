/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;

import lombok.Data;

/**
 * Created by mingshun on 3/23/15.
 */
@Data
public class DiffEntry {
    private ChangeType changeType;
    private String oldPath;
    private String newPath;

    public enum ChangeType {
        ADD,
        COPY,
        DELETE,
        MODIFY,
        RENAME
    }
}