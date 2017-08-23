/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.event;

import lombok.Data;
import org.springframework.context.ApplicationEvent;

/**
 * Created by phy on 2015/1/30.
 */
@Data
public class WorkspaceStatusEvent {

    private String spaceKey;


    public WorkspaceStatusEvent(String spaceKey){
        this.spaceKey = spaceKey;
    }
}
