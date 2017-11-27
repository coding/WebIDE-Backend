/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.coding.ide.model.FileInfo;
import net.coding.ide.model.Workspace;

/**
 * Created by phy on 2015/1/30.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FileCreateEvent extends FileChangeEvent {

    public FileCreateEvent(String spaceKey, FileInfo fileInfo) {
        super(spaceKey, fileInfo);
    }
}
