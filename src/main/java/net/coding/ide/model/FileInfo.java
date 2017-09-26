/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;


import lombok.Data;
import lombok.EqualsAndHashCode;
import org.joda.time.DateTime;


/**
 * Created by vangie on 14/11/11.
 */
@Data
@EqualsAndHashCode(of = {"path", "isDir"})
public class FileInfo {

    private String path;  // 包含 name

    private String name;

    private boolean isDir;

    private Integer directoriesCount;

    private Integer filesCount;

    private long size;

    private DateTime lastModified;

    private DateTime lastAccessed;

    private String contentType;

    private GitStatus gitStatus;

    private boolean isSymbolicLink;

    private boolean readable = true;

    private boolean writable = true;

    private String target;

}
