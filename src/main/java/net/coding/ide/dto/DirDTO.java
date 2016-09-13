/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.dto;

import lombok.Data;
import lombok.NonNull;

/**
 * Created by tan on 16/8/23.
 */
@Data(staticConstructor = "of")
public class DirDTO {

    @NonNull
    private String spaceKey;

    @NonNull
    private String path;
}
