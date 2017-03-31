package net.coding.ide.utils;

import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.IO;
import org.eclipse.jgit.util.RawParseUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by tan on 28/03/2017.
 *
 * @see RebaseCommand.RebaseState
 */
public class RebaseStateUtils {

    /**
     * The name of the "rebase-apply" folder for non-interactive rebases.
     */
    private static final String REBASE_APPLY = "rebase-apply"; //$NON-NLS-1$

    /**
     * The name of the "rebase-merge" folder for interactive rebases.
     */
    private static final String REBASE_MERGE = "rebase-merge"; //$NON-NLS-1$

    public static final File getRebaseStateDir(Repository repository) {
        File file = null;

        File rebaseApply = new File(repository.getDirectory(), REBASE_APPLY);
        if (rebaseApply.exists()) {
            file = rebaseApply;
        } else {
            file = new File(repository.getDirectory(), REBASE_MERGE);
        }

        return file;
    }

    public static File getRebaseFile(Repository repository, String name) {
        return new File(getRebaseStateDir(repository), name);
    }

    public static String readFile(File directory, String fileName)
            throws IOException {
        byte[] content = IO.readFully(new File(directory, fileName));
        // strip off the last LF
        int end = RawParseUtils.prevLF(content, content.length);
        return RawParseUtils.decode(content, 0, end + 1);
    }

    public static String getRebasePath(Repository repository, String name) {
        return (getRebaseStateDir(repository).getName() + "/" + name); //$NON-NLS-1$
    }

    public static void createFile(Repository repository, String name, String content) throws IOException {
        createFile(getRebaseStateDir(repository), name, content);
    }

    public static void createFile(File parentDir, String name,
                                   String content)
            throws IOException {
        File file = new File(parentDir, name);
        FileOutputStream fos = new FileOutputStream(file);
        try {
            fos.write(content.getBytes(Constants.CHARACTER_ENCODING));
            fos.write('\n');
        } finally {
            fos.close();
        }
    }
}
