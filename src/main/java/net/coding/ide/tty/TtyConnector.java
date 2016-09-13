/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.tty;

import java.io.IOException;

/**
 * Created by tan on 16/8/9.
 */
public interface TtyConnector {

    void write(byte[] bytes) throws IOException;

    void write(String s) throws IOException;

    String read() throws IOException;

    void close();

    void resize(Integer cols, Integer rows);

    boolean isConnected();
}
