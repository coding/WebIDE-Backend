/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.web.message;

import com.fatboyindustrial.gsonjodatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.coding.ide.event.*;
import net.coding.ide.model.FileInfo;
import net.coding.ide.model.Workspace;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.*;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Created by vangie on 15/2/4.
 */
@Slf4j
@Component
public class EventExchange {

    @Autowired
    private WebSocketSessionStore webSocketSessionStore;

    @Autowired
    private OnlineWorkspaceStore onlineWorkspaceStore;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private Gson gson = Converters.registerDateTime(new GsonBuilder()).create();

    @EventListener
    public void onSessionConnected(SessionConnectEvent event) {
        Message msg = event.getMessage();
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(msg);
        String sessionId = accessor.getSessionId();

        String spaceKey = (String) webSocketSessionStore.getAttribute(sessionId, "spaceKey");

        log.debug("Session connect: spaceKey => {}, sessionId => {} ", spaceKey, sessionId);

        if (spaceKey == null) {
            return;
        }

        boolean isToBeOnline = onlineWorkspaceStore.isEmpty(spaceKey);
        onlineWorkspaceStore.addSession(spaceKey, sessionId);

        if (isToBeOnline) {
            eventPublisher.publishEvent(new WorkspaceOnlineEvent(spaceKey));
        }
    }

    @EventListener
    public void onSessionDisConnected(SessionDisconnectEvent event) {
        Message msg = event.getMessage();
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(msg);
        String sessionId = accessor.getSessionId();

        String spaceKey = (String) webSocketSessionStore.getAttribute(sessionId, "spaceKey");

        log.debug("Session disconnect: spaceKey => {}, sessionId => {} ", spaceKey, sessionId);

        if (spaceKey == null) {
            return;
        }

        onlineWorkspaceStore.removeSession(spaceKey, sessionId);
        boolean isToBeOffline = onlineWorkspaceStore.isEmpty(spaceKey);

        if (isToBeOffline) {
            eventPublisher.publishEvent(new WorkspaceOfflineEvent(spaceKey));
        }

    }

    @EventListener
    public void onFileChanged(FileChangeEvent event) {
        Workspace ws = event.getWorkspace();
        String spaceKey = ws.getSpaceKey();
        FileInfo fileInfo = event.getFileInfo();

        JsonObject jsonObj = new JsonObject();
        jsonObj.add("fileInfo", gson.toJsonTree(fileInfo));

        if (event instanceof FileCreateEvent) {
            jsonObj.addProperty("changeType", "create");
        } else if (event instanceof FileModifyEvent) {
            jsonObj.addProperty("changeType", "modify");
        } else if (event instanceof FileDeleteEvent) {
            jsonObj.addProperty("changeType", "delete");
        }

        if (fileInfo.getLastModified() != null) {
            jsonObj.addProperty("lastModified", fileInfo.getLastModified().getMillis());
        }

        log.debug("send file change event: {}", event);
        simpMessagingTemplate.convertAndSend("/topic/ws/" + spaceKey + "/change", jsonObj);
    }

    @EventListener
    public void onGitCheckout(GitCheckoutEvent event) {
        Workspace ws = event.getWorkspace();
        String spaceKey = ws.getSpaceKey();

        String branch = event.getBranch();

        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("branch", branch);

        simpMessagingTemplate.convertAndSend("/topic/git/" + spaceKey + "/checkout", jsonObj);
    }

    @EventListener
    public void onWorkspaceDeleted(WorkspaceDeleteEvent event) {
        String spaceKey = event.getSpaceKey();

        simpMessagingTemplate.convertAndSend("/topic/ws/" + spaceKey + "/delete", (Object) null);
    }
}
