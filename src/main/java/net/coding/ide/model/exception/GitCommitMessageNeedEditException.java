package net.coding.ide.model.exception;

/**
 * Created by tan on 28/03/2017.
 */
public class GitCommitMessageNeedEditException extends RuntimeException {
    public GitCommitMessageNeedEditException(String message) {
        super(message);
    }
}
