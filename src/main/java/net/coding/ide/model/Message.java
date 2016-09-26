/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;

import lombok.Data;

import java.util.List;

/**
 * Created by tan on 16/8/11.
 */
@Data
public class Message {
    private String name;

    List<Arg> args;

    @Data
    public static class Arg {
        public String id;

        private Integer cols;

        private Integer rows;

        private String output;

        private String input;

        private String cwd;

        private String spaceKey;
    }
}
