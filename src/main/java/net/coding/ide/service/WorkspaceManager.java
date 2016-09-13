/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.service;

import net.coding.ide.entity.WorkspaceEntity;
import net.coding.ide.model.FileInfo;
import net.coding.ide.model.FileSearchResultEntry;
import net.coding.ide.model.Workspace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;

/**
 * Created by vangie on 14/11/11.
 */
public interface WorkspaceManager {

    Workspace setup(String spaceKey);

    Workspace createFromUrl(String gitUrl);

    void delete(String spaceKey);

    Workspace getWorkspace(String spaceKey);

    List<WorkspaceEntity> list();

    WorkspaceEntity getWorkspaceEntity(String spaceKey);

    FileInfo getFileInfo(Workspace ws, String path) throws Exception;

    List<FileInfo> listFiles(Workspace ws, String path, boolean order, boolean group) throws Exception;

    List<FileSearchResultEntry> search(Workspace ws, String keyword, boolean includeNonProjectItems) throws IOException;

    boolean isOnline(String spaceKey);

    boolean isDeleted(String spaceKey);

    void setEncoding(Workspace ws, String charSet);
}
