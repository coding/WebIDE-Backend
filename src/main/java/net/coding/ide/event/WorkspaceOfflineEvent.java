/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.event;

/**
 * Created by phy on 2015/1/30.
 */
public class WorkspaceOfflineEvent extends WorkspaceStatusEvent {

    public WorkspaceOfflineEvent(Object source, String spaceKey) {
        super(source, spaceKey);
    }
}
