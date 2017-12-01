/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.service;

import net.coding.ide.dto.FileDTO;
import net.coding.ide.entity.WorkspaceEntity;
import net.coding.ide.model.FileInfo;
import net.coding.ide.model.FileSearchResultEntry;
import net.coding.ide.model.Workspace;
import net.coding.ide.model.exception.GitCloneAuthFailException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;

/**
 * Created by vangie on 14/11/11.
 */
public interface WorkspaceManager {

    Workspace setup(String spaceKey);

    Workspace createFromUrl(String gitUrl) throws GitCloneAuthFailException;

    void delete(String spaceKey);

    Workspace getWorkspace(String spaceKey);

    List<WorkspaceEntity> list();

    WorkspaceEntity getWorkspaceEntity(String spaceKey);

    FileDTO readFile(Workspace ws, String path, String encoding, boolean base64) throws IOException, GitAPIException, Exception;

    FileInfo getFileInfo(Workspace ws, String path) throws Exception;

    List<FileInfo> listFiles(Workspace ws, String path, boolean order, boolean group) throws Exception;

    List<FileSearchResultEntry> search(Workspace ws, String keyword, boolean includeNonProjectItems) throws IOException;

    boolean isOnline(String spaceKey);

    boolean isDeleted(String spaceKey);

    void setEncoding(Workspace ws, String charSet);
}
