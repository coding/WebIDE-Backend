/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.tty;

import com.google.gson.Gson;
import com.pty4j.PtyProcess;
import lombok.extern.log4j.Log4j;
import net.coding.ide.model.Message;
import org.atmosphere.socketio.SocketIOException;
import org.atmosphere.socketio.SocketIOSessionOutbound;
import org.atmosphere.socketio.transport.SocketIOPacketImpl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by tan on 16/8/11.
 */
@Log4j
public class TerminalEmulator extends Thread {

    Charset charset = Charset.forName("UTF-8");

    private TtyConnector ttyConnector;

    private String termId;

    private SocketIOSessionOutbound outbound;

    private Gson gson = new Gson();

    public TerminalEmulator(String termId, SocketIOSessionOutbound outbound, String[]command, Map<String, String> envs, String workingDirectory) throws IOException {
        PtyProcess pty = PtyProcess.exec(command, envs, workingDirectory);// working dir

        this.termId = termId;
        this.outbound = outbound;
        this.ttyConnector = new ProcessTtyConnector(pty, charset);
    }

    public void run() {
        while (!Thread.interrupted() && ttyConnector.isConnected()) {
            try {
                String s = ttyConnector.read();

                Message message = new Message();
                message.setName("shell.output");

                Message.Arg a = new Message.Arg();
                a.setOutput(s);
                a.setId(termId);

                message.setArgs(Arrays.asList(a));

                outbound.sendMessage(new SocketIOPacketImpl(SocketIOPacketImpl.PacketType.EVENT, gson.toJson(message)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ttyConnector.close();

        log.info("pty exit success");
    }

    public void write(String msg) throws IOException {
        ttyConnector.write(msg);
    }

    public void resize(Integer cols, Integer rows) {
        ttyConnector.resize(cols, rows);
    }

    public void close() {
        interrupt();

        Message message = new Message();
        message.setName("shell.exit");

        Message.Arg a = new Message.Arg();
        a.setId(termId);

        message.setArgs(Arrays.asList(a));

        try {
            outbound.sendMessage(new SocketIOPacketImpl(SocketIOPacketImpl.PacketType.EVENT, gson.toJson(message)));
        } catch (SocketIOException e) {
            log.error("TerminalEmulator occurs error when closing", e);
        }
    }
}
