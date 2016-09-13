/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.git;

import org.eclipse.jgit.transport.CredentialItem;

import java.io.File;

/**
 * Created by vangie on 14/12/30.
 */
public class KnownHosts extends CredentialItem {

    private File knownHostsFile;


    public KnownHosts() {
        super(null, false);
    }

    @Override
    public void clear() {

    }

    public File getValue() {
        return knownHostsFile;
    }

    public void setValue(File knowHostsFile) {
        this.knownHostsFile = knowHostsFile;
    }
}
