/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.event;


public class WorkspaceDeleteEvent extends WorkspaceStatusEvent {

    public WorkspaceDeleteEvent(String spaceKey) {
        super(spaceKey);
    }
}
