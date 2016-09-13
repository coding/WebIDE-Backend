/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.service;

import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import net.coding.ide.model.Key;
import net.coding.ide.model.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.setPosixFilePermissions;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static net.coding.ide.utils.KeyUtils.fingerprint;
import static net.coding.ide.utils.KeyUtils.keyToString;
import static org.apache.commons.lang.SystemUtils.IS_OS_WINDOWS;

/**
 * Created by vangie on 14/12/24.
 */
@Slf4j
@Service
public class KeyManagerImpl extends BaseService implements KeyManager {
    private final static String CODING_PUB_KEY = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDHOWdwLpkos2CLli6DFvQ36yQE6Pe" +
            "/PtFp3XwyirfZCIoGWnedaWI8zkJWVCs0wgOB9/urFepTDfV2wN49KGy1sl2/CCDEH2K/zeoEAZlTcBrhU17bwg1yMHCyJ7IM+zdLzIt" +
            "DEKYjgoWqVdUGK1dXQQlwt7GP4W7HqffelQQoVxOMoZ5N50MzD+nvV4y8iq0KwDQNy62iU4hui9ajCSVUDLu/06ucd5IojSI9keRIYAX" +
            "vQf52TJ5EbvoBggp9RhjuWNEG8IhnPP6rzPS11Ocmwg/HsP8xOKL28AeDBAh6B6MEBDtlyp5Yfu9cwZJ9CFtU/x5fHFPtANmgIphAfwN1";

    private final static String PUBLICKEY = "publicKey";

    private final static String PRIVATEKEY = "privateKey";

    private final static String FINGERPRINT = "fingerPrint";

    @Value("${USERNAME}")
    private String username;

    @Autowired
    private ConfigService configService;

    public boolean isKeyExist() {
        return configService.getByKey(PUBLICKEY) != null;
    }

    @Override
    public boolean isKeyExist(Workspace ws) {
        return getPrivateKeyFile(ws).exists() && getPublicKeyFile(ws).exists();
    }

    public Key generateKey() throws IOException {
        KeyPairGenerator generator = null;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
            // or: generator = KeyPairGenerator.getInstance("DSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        generator.initialize(2048);
        KeyPair keyPair = generator.genKeyPair();
        final String publicKey = keyToString((RSAPublicKey) keyPair.getPublic(), username);
        final String privateKey = keyToString((RSAPrivateKey) keyPair.getPrivate());
        final String fingerprint = fingerprint(publicKey);

        Key key = new Key(privateKey, publicKey, fingerprint);

        saveOrUpdateKey(key);

        return key;
    }

    @Transactional
    private void saveOrUpdateKey(Key key) {
        configService.setCfg(PUBLICKEY, PUBLICKEY, key.getPublicKey());
        configService.setCfg(PRIVATEKEY, PRIVATEKEY, key.getPrivateKey());
        configService.setCfg(FINGERPRINT, FINGERPRINT, key.getFingerprint());
    }

    public Key getKey() {
        Key key = new Key();

        key.setPrivateKey(configService.getValue(PRIVATEKEY));
        key.setPublicKey(configService.getValue(PUBLICKEY));
        key.setFingerprint(configService.getValue(FINGERPRINT));

        return key;
    }

    @Override
    public void copyToWorkspace(Workspace ws) {
        try {
            if (!isKeyExist()) generateKey();

            Key key = getKey();

            Files.write(key.getPublicKey(), getPublicKeyFile(ws), defaultCharset());
            Files.write(key.getPrivateKey(), getPrivateKeyFile(ws), defaultCharset());
            Files.write(format("coding.net %s\n", CODING_PUB_KEY), getKnownHostsFile(ws), defaultCharset());
            
            //ignore on windows.
            if (!IS_OS_WINDOWS) {
                //using PosixFilePermission to set file permissions 600
                setPosixFilePermissions(getPrivateKeyFile(ws).toPath(), newHashSet(OWNER_READ, OWNER_WRITE));
            }
        } catch (IOException e) {
            log.error("copy key to workspace failed", e);
        }
    }
    @Override
    public File getPrivateKeyFile(Workspace ws) {
        return new File(ws.getKeyDir(), "id_rsa");
    }

    @Override
    public File getPublicKeyFile(Workspace ws) {
        return new File(ws.getKeyDir(), "id_rsa.pub");
    }

    @Override
    public File getKnownHostsFile(Workspace ws) {
        return new File(ws.getKeyDir(), "known_hosts");
    }
}
