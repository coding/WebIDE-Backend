/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.web.controller;

import net.coding.ide.entity.WorkspaceEntity;
import net.coding.ide.model.Workspace;
import net.coding.ide.model.exception.WorkspaceMissingException;
import net.coding.ide.service.WorkspaceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

import java.beans.PropertyEditorSupport;

/**
 * Created by vangie on 15/3/7.
 */
@ControllerAdvice
public class WorkspaceAdvice {

    @Autowired
    private WorkspaceManager wsMgr;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Workspace.class, new WorkspaceEditor(wsMgr));
    }

    public static class WorkspaceEditor extends PropertyEditorSupport {

        private final WorkspaceManager wsMgr;

        public WorkspaceEditor(WorkspaceManager wsMgr) {
            this.wsMgr = wsMgr;
        }

        @Override
        public void setAsText(String spaceKey) throws IllegalArgumentException {
            WorkspaceEntity wsEntity = wsMgr.getWorkspaceEntity(spaceKey);

            if (wsEntity == null) {
                throw new WorkspaceMissingException(String.format("workspace '%s' is not found.", spaceKey));
            }

            Workspace ws = wsMgr.getWorkspace(spaceKey);

            if (ws == null) {
                ws = wsMgr.setup(spaceKey);
            }

            this.setValue(ws);
        }
    }
}
