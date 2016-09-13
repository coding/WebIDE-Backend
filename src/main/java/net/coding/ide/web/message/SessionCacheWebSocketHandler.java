/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.web.message;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * Created by vangie on 15/2/27.
 *
 */
@Slf4j
public class SessionCacheWebSocketHandler extends WebSocketHandlerDecorator {
    private WebSocketSessionStore webSocketSessionStore;

    public SessionCacheWebSocketHandler(WebSocketHandler delegate, WebSocketSessionStore store) {
        super(delegate);
        this.webSocketSessionStore = store;
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.debug("after WebSocket connection established, http session id => {}, spaceKey => {}",
                session.getAttributes().get(HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME),
                session.getAttributes().get("spaceKey"));

        webSocketSessionStore.addSession(session.getId(), session);
        super.afterConnectionEstablished(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.debug("after WebSocket connection closed");

        super.afterConnectionClosed(session, closeStatus);
        webSocketSessionStore.removeSession(session.getId());
    }

}
