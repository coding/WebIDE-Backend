/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.utils;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.coding.ide.service.KeyManagerImpl;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.function.Function;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Splitter.fixedLength;
import static com.google.common.hash.Hashing.md5;
import static com.google.common.io.BaseEncoding.base16;
import static com.google.common.io.BaseEncoding.base64;
import static java.lang.String.format;

/**
 * Created by tan on 16-2-26.
 */
@Slf4j
public class KeyUtils {
    /**
     * href="http://tools.ietf.org/html/draft-friedl-secsh-fingerprint-00" >spec</a>
     *
     * @return hex fingerprint ex. {@code 2b:a9:62:95:5b:8b:1d:61:e0:92:f7:03:10:e9:db:d9}
     */
    public static String fingerprint(String publicKey) {
        String[] contents = StringUtils.split(publicKey, " ");
        byte[] contentBase64 = base64().decode(contents[1]);
        HashCode hc = md5().hashBytes(contentBase64);
        return on(":").join(fixedLength(2).split(base16().lowerCase().encode(hc.asBytes())));
    }

    public static String keyToString(RSAPublicKey pubKey, String username) throws IOException {
        ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(byteOs);

        List<byte[]> bytesList = Lists.newArrayList(
                "ssh-rsa".getBytes(),
                pubKey.getPublicExponent().toByteArray(),
                pubKey.getModulus().toByteArray()
        );

        for (byte[] bytes : bytesList) {
            dos.writeInt(bytes.length);
            dos.write(bytes);
        }

        dos.close();

        String publicKeyEncoded = new String(Base64.encodeBase64(byteOs.toByteArray()));

        return format("ssh-rsa %s %s@WebIDE", publicKeyEncoded, username);
    }

    public static String keyToString(RSAPrivateKey privateKey) {
        CharArrayWriter caw = new CharArrayWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(caw);
        try {
            pemWriter.writeObject(privateKey);
        } catch (IOException e) {
            log.error("", e);
        } finally {
            try {
                pemWriter.close();
            } catch (IOException e) {
                log.error("", e);
            }
        }
        return caw.toString().trim();
    }

    public static boolean isKeyExist(JsonArray keys, String key, Function<JsonObject, String> keyExtractor) {

        if (key == null) return false;

        for (JsonElement k : keys) {
            if (key.equals(keyExtractor.apply(k.getAsJsonObject()))) {
                return true;
            }
        }

        return false;
    }
}
