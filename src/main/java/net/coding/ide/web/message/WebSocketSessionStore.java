/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.web.message;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by vangie on 15/2/28.
 */
@Component
public class WebSocketSessionStore {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<String, WebSocketSession>();

    public void addSession(String id, WebSocketSession session){
        sessions.put(id, session);
    }

    public WebSocketSession getSession(String id){
        return sessions.get(id);
    }

    public WebSocketSession removeSession(String id){
        return sessions.remove(id);
    }

    public Object getAttribute(String id, String key){
        WebSocketSession session = sessions.get(id);
        if(session == null){
            return null;
        }
        return session.getAttributes().get(key);
    }

    public Object setAttribute(String id, String key, Object value) {
        WebSocketSession session = sessions.get(id);
        if(session == null){
            return null;
        }
        return session.getAttributes().put(key, value);
    }


}
