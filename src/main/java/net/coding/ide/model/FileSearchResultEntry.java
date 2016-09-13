/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;

import lombok.Data;
import lombok.NonNull;

/**
 * Created by mingshun on 7/23/15.
 */
@Data
public class FileSearchResultEntry {

    @NonNull
    private String path;

    @NonNull
    private String contentType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileSearchResultEntry that = (FileSearchResultEntry) o;

        if (!contentType.equals(that.contentType)) return false;
        if (!path.equals(that.path)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = path.hashCode();
        result = 31 * result + contentType.hashCode();
        return result;
    }
}
