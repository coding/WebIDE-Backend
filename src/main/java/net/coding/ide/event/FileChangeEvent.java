/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.event;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.coding.ide.model.FileInfo;
import net.coding.ide.model.Workspace;

/**
 * Created by phy on 2015/1/30.
 */
@Data
@NoArgsConstructor
public class FileChangeEvent {

    protected Workspace workspace;

    protected FileInfo fileInfo;

    public FileChangeEvent(Workspace source, FileInfo fileInfo){
        this.workspace = source;
        this.fileInfo = fileInfo;
    }

    public int hashCode() {
        return fileInfo.hashCode();
    }
}
