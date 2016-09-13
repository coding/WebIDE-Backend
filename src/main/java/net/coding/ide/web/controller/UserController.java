/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.web.controller;

import net.coding.ide.dto.KeyDTO;
import net.coding.ide.dto.UserDTO;
import net.coding.ide.model.Key;
import net.coding.ide.service.KeyManager;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * Created by vangie on 15/7/21.
 */

@RestController
@RequestMapping(value = "/user", produces = APPLICATION_JSON_VALUE)
public class UserController {

    @Autowired
    private KeyManager keyMgr;

    @Autowired
    private ModelMapper mapper;

    @Value("${USERNAME}")
    private String username;

    @Value("${AVATAR}")
    private String avatar;

    @RequestMapping(method = GET, params = "public_key")
    public KeyDTO publicKey() throws IOException {

        Key key;

        if (!keyMgr.isKeyExist()) {
            key = keyMgr.generateKey();
        } else {
            key = keyMgr.getKey();
        }

        return mapper.map(key, KeyDTO.class);
    }

    @RequestMapping(value = "", method = GET)
    public UserDTO currentUser() {
        return UserDTO.of(username, avatar);
    }
}
