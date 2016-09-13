/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.git;

import org.eclipse.jgit.transport.CredentialItem;

import java.io.File;

/**
 * Created by vangie on 14/12/30.
 */
public class Identity extends CredentialItem {


    private File identityFile;


    public Identity() {
        super(null, false);
    }

    @Override
    public void clear() {

    }

    public File getValue() {
        return identityFile;
    }

    public void setValue(File identityFile) {
        this.identityFile = identityFile;
    }
}
