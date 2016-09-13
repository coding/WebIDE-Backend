/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.service;

import net.coding.ide.model.Key;
import net.coding.ide.model.Workspace;

import java.io.File;
import java.io.IOException;

/**
 * Created by vangie on 14/12/24.
 */
public interface KeyManager {

    boolean isKeyExist();

    boolean isKeyExist(Workspace ws);

    Key generateKey() throws IOException;

    void copyToWorkspace(Workspace ws);

    File getPrivateKeyFile(Workspace ws);

    File getPublicKeyFile(Workspace ws);

    File getKnownHostsFile(Workspace ws);

    Key getKey();

}
