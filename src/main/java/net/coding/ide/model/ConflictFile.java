/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;

import lombok.Data;

/**
 * Created by tan on 16-3-15.
 */
@Data
public class ConflictFile {

    private String base;

    private String local;

    private String remote;
}
