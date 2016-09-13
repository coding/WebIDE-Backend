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
public class WorkspaceStatusEvent extends ApplicationEvent {

    private String spaceKey;


    public WorkspaceStatusEvent(Object source, String spaceKey){
        super(source);
        this.spaceKey = spaceKey;
    }
}
