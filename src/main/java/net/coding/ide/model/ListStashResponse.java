/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tan on 16-3-17.
 */
@Data
public class ListStashResponse {

    private List<Stash> stashes = new ArrayList<>();

    public void addStash(Stash stash) {
        this.stashes.add(stash);
    }

    @Data
    public static class Stash {
        private String name;

        private String message;

        private String rev;

        public Stash(String rev, String name, String message) {
            this.rev = rev;
            this.name = name;
            this.message = message;
        }
    }
}
