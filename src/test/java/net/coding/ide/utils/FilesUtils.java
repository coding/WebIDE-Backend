package net.coding.ide.utils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by tan on 16-3-15.
 */
public class FilesUtils {

    public static File createTempDirectoryAndDeleteOnExit(String path) {
        try {
            File tmpDir = java.nio.file.Files.createTempDirectory(path).toFile();

            // delete directory when exit jvm
            Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.deleteQuietly(tmpDir)));

            return tmpDir;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
