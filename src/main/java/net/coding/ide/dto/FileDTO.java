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

    private String content;

    private Boolean base64;

    private Long lastModified;

    private String encoding;

    private FileDTO(String path, String content, String encoding, Boolean base64, Long lastModified) {
        this.path = path;
        this.content = content;
        this.base64 = base64;
        this.lastModified = lastModified;
        this.encoding = encoding;
    }

    public static FileDTO of(String path, String content, String encoding, Boolean base64, Long lastModified) {
        return new FileDTO(path, content, encoding, base64, lastModified);
    }

    public static FileDTO of(String path, String content, Boolean base64) {
        return new FileDTO(path, content, null, base64, null);
    }

    public static FileDTO of(String path, String content, String encoding, Boolean base64) {
        return new FileDTO(path, content, encoding, base64, null);
    }

}
