/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.tty;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.coding.ide.model.Message;
import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.socketio.SocketIOSessionOutbound;
import org.atmosphere.socketio.cache.SocketIOBroadcasterCache;
import org.atmosphere.socketio.cpr.SocketIOAtmosphereHandler;
import org.atmosphere.socketio.cpr.SocketIOAtmosphereInterceptor;
import org.atmosphere.socketio.transport.DisconnectReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;

/**
 * Simple SocketIOAtmosphereHandler that implements the logic to build a
 * SocketIO Chat application.
 */
@Slf4j
@AtmosphereHandlerService(
        path = "/*",
        supportSession = true,
        broadcasterCache = UUIDBroadcasterCache.class,
        interceptors = SocketIOAtmosphereInterceptor.class)
public class SocketIOHandler extends SocketIOAtmosphereHandler {
    private final Gson gson = new Gson();
    private ConcurrentMap<String, TerminalEmulator> connectors = new ConcurrentHashMap<>();
    private Multimap<String, String> sessions = HashMultimap.create();

    public void onConnect(AtmosphereResource r, SocketIOSessionOutbound outbound) throws IOException {
        log.debug("onConnect");
        outbound.sendMessage("0{\"sid\":\"" + outbound.getSessionId() + "\",\"upgrades\":[],\"pingInterval\":25000,\"pingTimeout\":60000}");
    }

    public void onMessage(AtmosphereResource r, SocketIOSessionOutbound outbound, String message) {
        if (outbound == null || message == null || message.length() == 0) {
            return;
        }

        try {
            Message msg = gson.fromJson(message, Message.class);

            String name = msg.getName();

            String spaceHome = r.getAtmosphereConfig().getInitParameter("SPACE_HOME", null);

            if (spaceHome == null) {
                log.error("SocketIOHandler on message error: SPACE_HOME must be setted in env");
                return;
            }

            Message.Arg arg = msg.getArgs().get(0);

            switch (name) {
                case "term.open":
                    itermOpen(outbound, spaceHome, arg);
                    break;
                case "term.input":
                    itermInput(outbound, arg);
                    break;
                case "term.resize":
                    itermResize(outbound, arg);
                    break;
                case "term.close":
                    itermClose(outbound, arg);
                    break;
                default:
                    System.out.println("command " + name + " not found.");
                    break;
            }
        } catch (Exception e) {
            log.error("SocketIOHandler onMessage error, message => {}, error => {}", message, e.getMessage());
        }
    }

    private void itermClose(SocketIOSessionOutbound outbound, Message.Arg arg) {
        String key = makeConnectorKey(outbound.getSessionId(), arg.getId());

        TerminalEmulator emulator = connectors.get(key);
        if (emulator != null) {
            emulator.close();
        }

        connectors.remove(key);
        sessions.remove(outbound.getSessionId(), arg.getId());
    }

    private void itermResize(SocketIOSessionOutbound outbound, Message.Arg arg) {
        String key = makeConnectorKey(outbound.getSessionId(), arg.getId());
        TerminalEmulator emulator = connectors.get(key);

        if (emulator != null) {
            emulator.resize(arg.getCols(), arg.getRows());
        }
    }

    private String getWorkdingDir(String spaceHome, String spaceKey) {
        return format("%s/%s/%s", spaceHome, spaceKey, "/working-dir");
    }

    private String makeConnectorKey(String session, String termId) {
        return session + "-" + termId;
    }

    public void itermOpen(SocketIOSessionOutbound outbound, String spaceHome, Message.Arg arg) {
        try {
            Map<String, String> envs = Maps.newHashMap(System.getenv());


            String shell = envs.get("SHELL");

            if (shell == null) {
                shell = "/bin/bash";
            }

            String[] command = new String[]{shell, "--login"};
            envs.put("TERM", "xterm");

            TerminalEmulator emulator = new TerminalEmulator(arg.getId(),
                    outbound,
                    command,
                    envs,
                    getWorkdingDir(spaceHome, arg.getSpaceKey()));

            String key = makeConnectorKey(outbound.getSessionId(), arg.getId());

            connectors.put(key, emulator);
            sessions.put(outbound.getSessionId(), arg.getId());

            emulator.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void itermInput(SocketIOSessionOutbound outbound, Message.Arg arg) throws IOException {
        String key = makeConnectorKey(outbound.getSessionId(), arg.getId());
        TerminalEmulator emulator = connectors.get(key);

        if (emulator != null) {
            connectors.get(key).write(arg.getInput());
        }
    }

    public void onDisconnect(AtmosphereResource r, SocketIOSessionOutbound outbound, DisconnectReason reason) {
        log.debug("ondisconnect, reason is :" + reason);

        String sessionId = outbound.getSessionId();

        Collection<String> termIds = sessions.get(sessionId);

        for (String termId : termIds) {
            String key = makeConnectorKey(sessionId, termId);

            TerminalEmulator emulator = connectors.get(key);
            if (emulator != null) {
                emulator.close();
                connectors.remove(key);
            }
        }

        sessions.removeAll(sessionId);
    }
}
