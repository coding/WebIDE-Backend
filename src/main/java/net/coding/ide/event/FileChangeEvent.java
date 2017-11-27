/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.coding.ide.model.FileInfo;
import net.coding.ide.model.Workspace;

/**
 * Created by phy on 2015/1/30.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class FileChangeEvent {

    protected String spaceKey;

    protected FileInfo fileInfo;

    public FileChangeEvent(String spaceKey, FileInfo fileInfo) {
        this.spaceKey = spaceKey;
        this.fileInfo = fileInfo;
    }
}
