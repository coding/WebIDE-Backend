package net.coding.ide.git.rebase;

import net.coding.ide.model.RebaseResponse;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.RebaseTodoLine;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.IO;

import java.io.File;
import java.io.IOException;

import static net.coding.ide.utils.RebaseStateUtils.createFile;
import static net.coding.ide.utils.RebaseStateUtils.getRebaseFile;

/**
 * Created by tan on 28/03/2017.
 */
public class SquashActionHandler implements RebaseActionHandler {

    @Override
    public RebaseTodoLine.Action getAction() {
        return RebaseTodoLine.Action.SQUASH;
    }

    @Override
    public RebaseResponse process(Repository repository, String message) throws GitAPIException, IOException {

        try (Git git = Git.wrap(repository)) {
            git.commit()
                    .setMessage(stripCommentLines(message))
                    .setAmend(true).setNoVerify(true).call();

            getRebaseFile(repository, MESSAGE_SQUASH).delete();
            getRebaseFile(repository, MESSAGE_FIXUP).delete();

            createFile(repository, MESSAGE, message);

            RebaseResult result = git.rebase()
                    .setOperation(RebaseCommand.Operation.SKIP)
                    .runInteractively(handler)
                    .call();

            return new RebaseResponse(result);
        }
    }

    @Override
    public RebaseResponse extractMessage(Repository repository) throws IOException {
        File messageFile = getRebaseFile(repository, MESSAGE_SQUASH);
        RebaseResponse response = new RebaseResponse(false, RebaseResponse.Status.INTERACTIVE_EDIT);

        if (messageFile.exists()) {
            response.setMessage(new String(IO.readFully(messageFile)));
        }

        return response;
    }

    /**
     * @param commitMessage
     *
     * @return
     *
     * @see RebaseCommand#stripCommentLines
     */
    private static String stripCommentLines(String commitMessage) {
        StringBuilder result = new StringBuilder();
        for (String line : commitMessage.split("\n")) { //$NON-NLS-1$
            if (!line.trim().startsWith("#")) //$NON-NLS-1$
                result.append(line).append("\n"); //$NON-NLS-1$
        }
        if (!commitMessage.endsWith("\n")) //$NON-NLS-1$
            result.deleteCharAt(result.length() - 1);
        return result.toString();
    }
}
