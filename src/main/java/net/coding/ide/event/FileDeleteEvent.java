/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.event;

import net.coding.ide.model.FileInfo;
import net.coding.ide.model.Workspace;

/**
 * Created by phy on 2015/1/30.
 */
public class FileDeleteEvent extends FileChangeEvent {

    public FileDeleteEvent(Workspace source, FileInfo fileInfo) {
        super(source, fileInfo);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + 3;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileDeleteEvent)) return false;

        FileDeleteEvent that = (FileDeleteEvent) o;

        if (fileInfo != null ? !fileInfo.equals(that.fileInfo) : that.fileInfo != null) return false;

        return true;
    }

}
