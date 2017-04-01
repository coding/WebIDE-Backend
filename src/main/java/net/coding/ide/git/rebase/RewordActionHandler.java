package net.coding.ide.git.rebase;

import net.coding.ide.model.RebaseResponse;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.RebaseTodoLine;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.IOException;
import java.util.List;

import static net.coding.ide.utils.RebaseStateUtils.getRebasePath;

/**
 * Created by tan on 28/03/2017.
 */
public class RewordActionHandler implements RebaseActionHandler {

    @Override
    public RebaseTodoLine.Action getAction() {
        return RebaseTodoLine.Action.REWORD;
    }

    @Override
    public RebaseResponse process(Repository repository, String message) throws GitAPIException, IOException {

        try (Git git = Git.wrap(repository)) {
            git.commit()
                    .setMessage(message)
                    .setAmend(true)
                    .setNoVerify(true)
                    .call();

            RebaseResult result = git.rebase()
                    .setOperation(RebaseCommand.Operation.SKIP)
                    .runInteractively(handler)
                    .call();

            return new RebaseResponse(result);
        }
    }

    @Override
    public RebaseResponse extractMessage(Repository repository) throws IOException {
        List<RebaseTodoLine> rebaseTodoLines = repository.readRebaseTodo(getRebasePath(repository, DONE), false);
        // the last rebase_todo_line
        RebaseTodoLine line = rebaseTodoLines.get(rebaseTodoLines.size() - 1);

        try (RevWalk walk = new RevWalk(repository)) {
            ObjectReader or = repository.newObjectReader();
            RevCommit commitToPick = walk.parseCommit(or.resolve(line.getCommit()).iterator().next());

            String oldMessage = commitToPick.getFullMessage();

            RebaseResponse response = new RebaseResponse(false, RebaseResponse.Status.INTERACTIVE_EDIT);
            response.setMessage(oldMessage);

            return response;
        }
    }
}
