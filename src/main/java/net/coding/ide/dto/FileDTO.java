/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.dto;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created by tan on 16/8/23.
 */
@Data
@RequiredArgsConstructor(staticName = "of")
public class FileDTO {

    @NonNull
    private String path;

    @NonNull
    private String content;

    @NonNull
    private Boolean base64;

    private long lastModified;

    private FileDTO(String path, String content, Boolean base64, long lastModified) {
        this.path = path;
        this.content = content;
        this.base64 = base64;
        this.lastModified = lastModified;
    }

    public static FileDTO of(String path, String content, Boolean base64, long lastModified) {
        return new FileDTO(path, content, base64, lastModified);
    }
}
