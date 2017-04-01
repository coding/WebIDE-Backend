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

import static net.coding.ide.utils.RebaseStateUtils.getRebaseFile;
import static org.eclipse.jgit.lib.RebaseTodoLine.Action.EDIT;

/**
 * Created by tan on 28/03/2017.
 */
public class EditActionHandler implements RebaseActionHandler {

    @Override
    public RebaseTodoLine.Action getAction() {
        return EDIT;
    }

    @Override
    public RebaseResponse process(Repository repository, String message) throws GitAPIException {
        try (Git git = Git.wrap(repository)) {
            git.commit()
                    .setAll(true)
                    .setAmend(true)
                    .setNoVerify(true)
                    .setMessage(message)
                    .call();

            RebaseResult result = git.rebase()
                    .setOperation(RebaseCommand.Operation.CONTINUE)
                    .runInteractively(handler)
                    .call();

            // 如果 conflict and edit，amend 后 continue 会返回 NOTHING_TO_COMMIT
            // so skip this commit
            if (result.getStatus().equals(RebaseResult.Status.NOTHING_TO_COMMIT)) {
                result = git.rebase().setOperation(RebaseCommand.Operation.SKIP).call();
            }

            return new RebaseResponse(result);
        }
    }

    @Override
    public RebaseResponse extractMessage(Repository repository) throws IOException {
        File messageFile = getRebaseFile(repository, MESSAGE);

        RebaseResponse response = new RebaseResponse(false, RebaseResponse.Status.INTERACTIVE_EDIT);

        if (messageFile.exists()) {
            response.setMessage(new String(IO.readFully(messageFile)));
        }

        return response;
    }
}
