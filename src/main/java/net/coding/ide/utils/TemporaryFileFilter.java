/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.utils;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.coding.ide.model.FileInfo;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by mingshun on 15-8-26.
 */
public class TemporaryFileFilter {
    private static final Pattern TEMPORARY_FILE_PATTERNS[] = {
            Pattern.compile("^\\..+\\.sw[a-z]$"),
            Pattern.compile("^\\.sw[a-z]$")
    };

    public static List<FileInfo> filter(List<FileInfo> fileInfos) {
        return Lists.newArrayList(Iterables.filter(fileInfos, new Predicate<FileInfo>() {

            @Override
            public boolean apply(FileInfo fileInfo) {
                String name = fileInfo.getName();
                for (Pattern pattern : TEMPORARY_FILE_PATTERNS) {
                    if (pattern.matcher(name).matches()) {
                        return false;
                    }
                }
                return true;
            }

        }));
    }
}
