/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by tan on 16/8/4.
 */
@Data
@NoArgsConstructor
public class Key {
    private String privateKey;

    private String publicKey;

    private String fingerprint;

    public Key(String privateKey, String publicKey, String fingerprint) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.fingerprint = fingerprint;
    }
}
