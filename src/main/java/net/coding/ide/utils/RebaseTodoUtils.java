/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.utils;

import net.coding.ide.model.RebaseResponse;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.RebaseTodoLine;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tan on 16-4-8.
 */
public class RebaseTodoUtils {

    public static List<RebaseResponse.RebaseTodoLine> loadFrom(List<RebaseTodoLine> lines) {
        List<RebaseResponse.RebaseTodoLine> rebaseTodoLines = new ArrayList<>();

        for (RebaseTodoLine line : lines) {
            RebaseResponse.RebaseTodoLine rebaseTodoLine = new RebaseResponse.RebaseTodoLine(
                    line.getAction().name(),
                    line.getCommit().name(),
                    line.getShortMessage());

            rebaseTodoLines.add(rebaseTodoLine);
        }

        return rebaseTodoLines;
    }

    public static List<RebaseTodoLine> parseLines(List<RebaseResponse.RebaseTodoLine> lines) {
        List<RebaseTodoLine> rebaseTodoLines = new ArrayList<>();

        for (RebaseResponse.RebaseTodoLine line : lines) {
            RebaseTodoLine rebaseTodoLine = new RebaseTodoLine(
                    RebaseTodoLine.Action.valueOf(line.getAction().name()),
                    AbbreviatedObjectId.fromString(line.getCommit()),
                    line.getShortMessage());

            rebaseTodoLines.add(rebaseTodoLine);
        }

        return rebaseTodoLines;
    }
}
