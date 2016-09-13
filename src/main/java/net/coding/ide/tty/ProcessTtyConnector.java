/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.tty;

import com.pty4j.PtyProcess;
import com.pty4j.WinSize;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * Created by tan on 16/8/9.
 */
public class ProcessTtyConnector implements TtyConnector {

    private final InputStreamReader reader;

    private final InputStream inputStream;
    private final OutputStream outputStream;

    private PtyProcess process;

    protected Charset charset;

    public ProcessTtyConnector(PtyProcess process, Charset charset) {
        this.process = process;
        this.charset = charset;
        this.inputStream = process.getInputStream();
        this.outputStream = process.getOutputStream();
        this.reader = new InputStreamReader(inputStream, charset);
    }

    public void write(byte[] bytes) throws IOException {
        outputStream.write(bytes);
        outputStream.flush();
    }

    public void write(String s) throws IOException {
        write(s.getBytes(charset));
    }

    public String read() throws IOException {
        StringBuilder sb = new StringBuilder();

        char[] buf = new char[1024];

        do {
            int len = reader.read(buf, 0, buf.length);

            if (len > 0) {
                sb.append(buf, 0, len);
            }
        } while (reader.ready());

        return sb.toString();
    }

    public void resize(Integer cols, Integer rows) {
        process.setWinSize(new WinSize(cols, rows));
    }

    @Override
    public void close() {
        process.destroy();
        try {
            outputStream.close();
        }
        catch (IOException ignored) { }
        try {
            inputStream.close();
        }
        catch (IOException ignored) { }
    }

    @Override
    public boolean isConnected() {
        return process.isRunning();
    }
}
