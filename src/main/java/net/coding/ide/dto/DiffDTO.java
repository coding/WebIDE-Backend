/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.dto;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Created by tan on 16/8/23.
 */
@Data
@RequiredArgsConstructor(staticName = "of")
public class DiffDTO {

    @NonNull
    private String diff;

}
