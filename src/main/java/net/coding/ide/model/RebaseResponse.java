/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.RebaseResult;

import java.util.List;

/**
 * Created by tan on 16-4-8.
 */
@Data
public class RebaseResponse {

    /**
     * @see RebaseResult.Status
     */
    public enum Status {
        /**
         * Rebase was successful, HEAD points to the new commit
         */
        OK,

        /**
         * Aborted; the original HEAD was restored
         */
        ABORTED,

        /**
         * Stopped due to a conflict; must either abort or resolve or skip
         */
        STOPPED,

        /**
         * Stopped for editing in the context of an interactive rebase
         *
         * @since 3.2
         */
        EDIT,

        /**
         * Failed; the original HEAD was restored
         */
        FAILED,

        /**
         * The repository contains uncommitted changes and the rebase is not a
         * fast-forward
         *
         * @since 3.2
         */
        UNCOMMITTED_CHANGES,

        /**
         * Conflicts: checkout of target HEAD failed
         */
        CONFLICTS,

        /**
         * Already up-to-date
         */
        UP_TO_DATE,

        /**
         * Fast-forward, HEAD points to the new commit
         */
        FAST_FORWARD,

        /**
         * Continue with nothing left to commit (possibly want skip).
         *
         * @since 2.0
         */
        NOTHING_TO_COMMIT,

        /**
         * Interactive rebase has been prepared
         * @since 3.2
         */
        INTERACTIVE_PREPARED,

        /**
         * Applying stash resulted in conflicts
         *
         * @since 3.2
         */
        STASH_APPLY_CONFLICTS,

        /**
         * need amend
         */
        INTERACTIVE_EDIT
    }

    @Data
    public static class RebaseTodoLine {

        public static enum Action {
            /** Use commit */
            PICK,

            /** Use commit, but edit the commit message */
            REWORD,

            /** Use commit, but stop for amending */
            EDIT,

            /** Use commit, but meld into previous commit */
            SQUASH,

            /** like "squash", but discard this commit's log message */
            FIXUP,

            /**
             * A comment in the file. Also blank lines (or lines containing only
             * whitespaces) are reported as comments
             */
            COMMENT
        }

        private Action action;

        private String commit;

        private String shortMessage;

        public RebaseTodoLine(String action, String commit, String shortMessage) {
            this.action = Action.valueOf(action);
            this.commit = commit;
            this.shortMessage = shortMessage;
        }
    }

    private boolean success;

    private Status status;

    private String message;

    private List<RebaseTodoLine> rebaseTodoLines;

    public RebaseResponse() {

    }

    public RebaseResponse(RebaseResult rebaseResult) {
        this.success = rebaseResult.getStatus().isSuccessful();
        this.status = Status.valueOf(rebaseResult.getStatus().name());
    }

    public RebaseResponse(boolean success, Status status) {
        this.success = success;
        this.status = status;
    }
}
