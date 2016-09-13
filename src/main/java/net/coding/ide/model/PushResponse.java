/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;

import lombok.Data;

import java.util.List;

@Data
public class PushResponse {
    public enum Status {
        AWAITING_REPORT,
        NON_EXISTING,
        NOT_ATTEMPTED,
        OK,
        REJECTED_NODELETE,
        REJECTED_NONFASTFORWARD,
        REJECTED_OTHER_REASON,
        REJECTED_REMOTE_CHANGED,
        UP_TO_DATE
    }

    private boolean nothingToPush;
    private boolean ok;
    private List<Update> updates;

    @Data
    public static class Update {
        private String remoteRefName;
        private String localRefName;
        private Status status;
        private String message;
    }
}
