/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;

import lombok.Data;

/**
 * Created by tan on 16/4/25.
 */
@Data
public class User {

    private Long id;

    private String email;

    private String password;

    private String name;

    private String globalKey;

    private String avatar;

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
