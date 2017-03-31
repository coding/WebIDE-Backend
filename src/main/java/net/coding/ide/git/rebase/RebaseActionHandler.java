package net.coding.ide.git.rebase;

import net.coding.ide.model.RebaseResponse;
import net.coding.ide.model.exception.GitCommitMessageNeedEditException;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.RebaseTodoLine;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.util.List;

public interface RebaseActionHandler {

    String DONE = "done"; //$NON-NLS-1$

    /**
     * @see RebaseCommand#MESSAGE_SQUASH
     */
    String MESSAGE_SQUASH = "message-squash"; //$NON-NLS-1$

    /**
     * @see RebaseCommand#MESSAGE_FIXUP
     */
    String MESSAGE_FIXUP = "message-fixup"; //$NON-NLS-1$

    /**
     * @see RebaseCommand#MESSAGE
     */
    String MESSAGE = "message"; //$NON-NLS-1$

    RebaseCommand.InteractiveHandler handler = new RebaseCommand.InteractiveHandler() { // stopped for edit message when squash and reword
        @Override
        public void prepareSteps(List<RebaseTodoLine> steps) {
            // does nothing
        }

        @Override
        public String modifyCommitMessage(String commit) {
            throw new GitCommitMessageNeedEditException(commit);
        }
    };

    RebaseResponse process(Repository repository, String message) throws GitAPIException, IOException;

    RebaseResponse extractMessage(Repository repository) throws IOException;

    RebaseTodoLine.Action getAction();
}