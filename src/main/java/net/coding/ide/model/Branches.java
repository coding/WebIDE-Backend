/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by tan on 16/8/23.
 */
@Data
@RequiredArgsConstructor(staticName = "of")
public class Branches {

    @NonNull
    private String current;

    @NonNull
    private List<String> local;

    @NonNull
    private List<String> remote;
}
