/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;


import lombok.Data;
import org.joda.time.DateTime;


/**
 * Created by vangie on 14/11/11.
 */
@Data
public class FileInfo {

    private String path;  // 包含 name

    private String name;

    private boolean isDir;

    private long size;

    private DateTime lastModified;

    private DateTime lastAccessed;

    private String contentType;

    private GitStatus gitStatus;

    private boolean isSymbolicLink;

    private String target;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileInfo)) return false;

        FileInfo fileInfo = (FileInfo) o;

        if (isDir != fileInfo.isDir) return false;
        if (path != null ? !path.equals(fileInfo.path) : fileInfo.path != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + (isDir ? 1 : 0);
        return result;
    }
}
