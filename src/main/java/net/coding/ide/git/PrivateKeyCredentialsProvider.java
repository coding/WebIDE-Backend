/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.git;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

import java.io.File;

/**
 * Created by vangie on 14/12/30.
 */
public class PrivateKeyCredentialsProvider extends CredentialsProvider {

    private File identityFile;
    private File knownHostsFile;

    public PrivateKeyCredentialsProvider(File identityFile, File knownHostsFile) {
        this.identityFile = identityFile;
        this.knownHostsFile = knownHostsFile;
    }

    @Override
    public boolean isInteractive() {
        return false;
    }

    @Override
    public boolean supports(CredentialItem... items) {
        for (CredentialItem i : items) {
            if (i instanceof Identity) {
                continue;
            } else if (i instanceof KnownHosts) {
                continue;
            } else {
                return false;
            }

        }
        return true;
    }

    @Override
    public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
        for (CredentialItem i : items) {
            if (i instanceof Identity) {
                ((Identity) i).setValue(identityFile);
                continue;
            }
            if (i instanceof KnownHosts) {
                ((KnownHosts) i).setValue(knownHostsFile);
                continue;
            }
            throw new UnsupportedCredentialItem(uri, i.getClass().getName()
                    + ":" + i.getPromptText()); //$NON-NLS-1$
        }
        return true;
    }
}
