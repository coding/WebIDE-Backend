/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.web.message;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Created by vangie on 15/2/5.
 */
@Component
public class OnlineWorkspaceStore {

    private Multimap<String, String> onlineWorkspaceSessions = HashMultimap.create();

    public Collection<String> getSessions(String spaceKey) {
        return onlineWorkspaceSessions.get(spaceKey);
    }

    public void removeSession(String spaceKey, String sessionId) {
        onlineWorkspaceSessions.remove(spaceKey, sessionId);
    }

    public void addSession(String spaceKey, String sessionId) {
        onlineWorkspaceSessions.put(spaceKey, sessionId);
    }

    public boolean isEmpty(String spaceKey) {
        return !onlineWorkspaceSessions.containsKey(spaceKey) || onlineWorkspaceSessions.get(spaceKey).isEmpty();
    }


}
