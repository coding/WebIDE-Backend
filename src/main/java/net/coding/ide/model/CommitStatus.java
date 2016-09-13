/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

public class CommitStatus {
    /**
     * uncommitted files with their git status
     */
    @Getter
    private List<File> files;

    /**
     * {@code true} if no differences exist between the working-tree, the index, and the current HEAD
     */
    @Getter
    private boolean clean;

    public CommitStatus(boolean clean) {
        this.clean = clean;
        files = new ArrayList<>();
    }

    public void putFile(String name, GitStatus status) {
        files.add(new File(name, status));
    }

    public File getFile(String name) {
        for (File file : files) {
            if (file.getName().equals(name)) {
                return file;
            }
        }
        return null;
    }

    @Data
    public class File {
        private String name;
        private GitStatus status;

        public File(String name, GitStatus status) {
            this.name = name;
            this.status = status;
        }
    }
}
