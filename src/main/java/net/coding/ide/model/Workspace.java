/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.BaseEncoding;
import lombok.Getter;
import lombok.Setter;
import net.coding.ide.entity.ProjectEntity;
import net.coding.ide.entity.WorkspaceEntity;
import net.coding.ide.model.exception.WorkspaceCreationException;
import net.coding.ide.model.exception.WorkspaceDeletingException;
import net.coding.ide.model.exception.WorkspaceIOException;
import net.coding.ide.utils.WildcardMatcher;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.ignore.IgnoreNode;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

import static com.google.common.io.Files.toByteArray;

/**
 * Created by vangie on 14/11/10.
 */
public class Workspace {

    @Getter
    private String spaceKey;

    private Path workingDir;
    private Path baseDir;
    private Path keyDir;

    @Getter
    private WorkspaceEntity WorkspaceEntity;

    @Getter
    private ProjectEntity project;

    @Getter
    private String sshUrl;

    /**
     * In the Linux kernel, the limit on the number of nested symlinks is 40.
     * While in Mac OS X, it is limited to 32.
     */
    private static final int LIMIT_NESTED_SYMLINK = 30;

    @Getter
    @Setter
    private String encoding;

    private Map<String, IgnoreNode> ignoreNodes = Maps.newHashMap();

    public Workspace(WorkspaceEntity wsEntity, File baseDir) {
        // the default character encoding is utf-8;
        this.encoding = wsEntity.getEncoding() == null ? "UTF-8" : wsEntity.getEncoding();
        this.WorkspaceEntity = wsEntity;
        this.project = wsEntity.getProject();
        this.spaceKey = wsEntity.getSpaceKey();
        this.sshUrl = project.getSshUrl();
        this.baseDir = baseDir.toPath();
        this.workingDir = new File(baseDir, "working-dir").toPath().normalize().toAbsolutePath();
        this.keyDir = new File(baseDir, "ssh-key").toPath();

        try {
            Files.createDirectories(this.keyDir);
        } catch (IOException e) {
            throw new WorkspaceCreationException("Can not create keyDir: " + this.keyDir.toString(), e);
        }

        try {
            Files.createDirectories(this.workingDir);
        } catch (IOException e) {
            throw new WorkspaceCreationException("Can not create workingDir: " + this.workingDir.toString(), e);
        }
    }

    public static void purge(File baseDir) {
        try {
            FileUtils.deleteDirectory(baseDir);
        } catch (Exception e) {
            throw new WorkspaceDeletingException("Can not delete " + baseDir.toString(), e);
        }
    }

    public String read(String path, boolean base64) throws IOException {
        Path p = this.getPath(path);

        if (Files.isSymbolicLink(p) && !linkTargetExist(p)) {
            throw new WorkspaceIOException("It's a illegal link: target file not exist.");
        }

        try {
            byte[] content = toByteArray(p.toFile());

            if (base64) {
                return BaseEncoding.base64().encode(content);
            }

            return new String(content, getEncoding());
        } catch (FileNotFoundException e) {
            throw new WorkspaceIOException(path + " not found", e);
        }

    }

    public InputStream getInputStream(String path) throws AccessDeniedException {
        Path p = this.getPath(path);

        try {
            return new FileInputStream(p.toFile());
        } catch (FileNotFoundException e) {
            throw new WorkspaceIOException(path + " not found", e);
        }
    }

    public void pack(String path, OutputStream out) throws AccessDeniedException {
        Path p = this.getPath(path);

        try (TarArchiveOutputStream tarOut = new TarArchiveOutputStream(
                new GzipCompressorOutputStream(
                        new BufferedOutputStream(out)))) {
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

            Files.walk(p)
                    .filter(pp -> !Files.isDirectory(pp))
                    .forEach(pp -> {
                        try {

                            Path rp = workingDir.relativize(pp.normalize());

                            TarArchiveEntry entry = new TarArchiveEntry(pp.toFile(), rp.toString());

                            tarOut.putArchiveEntry(entry);

                            IOUtils.copy(new FileInputStream(pp.toFile()), tarOut);

                            tarOut.closeArchiveEntry();

                        } catch (IOException e) {
                            throw new WorkspaceIOException("pack " + path + " error", e);
                        }
                    });

            tarOut.flush();
            tarOut.finish();
        } catch (IOException e) {
            throw new WorkspaceIOException("pack " + path + " error", e);
        }
    }

    /*
    返回用户眼中的 normalize absolute path.
     */
    public Path getNormalizePath(String path) throws AccessDeniedException {
        return Paths.get("/" + getRelativePath(path).normalize());
    }

    /*
    return the normalize absolute path.
     */
    public Path getPath(String path) throws AccessDeniedException {
        Path p = resolve(path);

        if (workingDir.relativize(p).normalize().startsWith("../")) {
            throw new AccessDeniedException(path);
        }

        return p;
    }


    /*
    return the normalize relative path.
     */
    public Path getRelativePath(String path) throws AccessDeniedException {
        Path relative = workingDir.relativize(resolve(path));

        if (relative.normalize().startsWith("../")) {
            throw new AccessDeniedException(path);
        }

        return relative;
    }

    private Path resolve(String path) {
        if (path == null) {
            path = "/";
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        // let the path start with "./".
        path = "." + path;

        Path normalizePath = workingDir.resolve(path).normalize();

        if (normalizePath.equals(workingDir.normalize())) {
            //处理根目录的时候，后面要加个 "/." ，否则，根目录的 FileInfo.getName() 值是 "working-dir",而不是 ".";而 "working-dir"对用户应该是不可见的。
            return Paths.get(normalizePath + "/.").toAbsolutePath();
        } else {
            return normalizePath.toAbsolutePath();
        }
    }

    public File getWorkingDir() {
        return workingDir.toFile();
    }

    public void cleanWorkingDir() {
        try {
            FileUtils.cleanDirectory(workingDir.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File getKeyDir() {
        return keyDir.toFile();
    }

    public void write(String path, String content, boolean base64, boolean override, boolean createParent) throws IOException {
        Path p = this.getPath(path);

        if (Files.isSymbolicLink(p) && !linkTargetExist(p)) {
            throw new WorkspaceIOException("It's a illegal link: target file not exist.");
        }

        if (createParent) {
            Path parent = p.getParent();
            createDirectories(parent);
        }

        String decodeContent = content;

        if (base64) {
            decodeContent = new String(BaseEncoding.base64().decode(content));
        }

        BufferedWriter bw = null;

        try {
            bw = Files.newBufferedWriter(p, Charset.forName(getEncoding()));

            if (override) {
                bw.write(decodeContent);
            } else {
                bw.append(decodeContent);
            }

        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }


    }

    public void create(String path) throws IOException {
        Path p = this.getPath(path);
        createDirectories(p.getParent());
        if (!Files.exists(p)) {
            Files.createFile(p);
        } else {
            throw new WorkspaceIOException(path + " is already exists!");
        }

    }

    public boolean exists(String path) throws IOException {
        Path p = this.getPath(path);
        return Files.exists(p);
    }

    public void mustExisted(String path) throws IOException {
        if (!exists(path)) {
            throw new WorkspaceIOException(path + " not found");
        }
    }

    public void remove(String path, boolean recursive) throws IOException {
        Path p = this.getPath(path);
        boolean isDir = p.toFile().isDirectory();
        if (!isDir) {
            Files.delete(p);
        } else {
            if (recursive) {
                FileUtils.deleteDirectory(p.toFile());
            } else {
                throw new WorkspaceIOException("can not delete " + path + " (it is a directory!)");
            }
        }

    }

    public void move(String from, String to, boolean force) throws IOException {
        Path fromPath = this.getPath(from);
        Path toPath = this.getPath(to);
        boolean fromFileExists = new File(fromPath.toString()).exists();
        boolean toFileExists = new File(toPath.toString()).exists();
        boolean fromFileIsDir = new File(fromPath.toString()).isDirectory();
        boolean toFileIsDir = new File(toPath.toString()).isDirectory();
        if (fromFileExists) {
            if (!toFileExists) {
                createDirectories(toPath.getParent());
                Files.move(fromPath, toPath);
            } else if (toFileIsDir) {
                throw new WorkspaceIOException(to + " is a directory, and is already exist!");
            } else if (fromFileIsDir) {
                throw new WorkspaceIOException("directory can not be moved to a file!");
            } else if (force) {
                Files.delete(toPath);
                Files.move(fromPath, toPath);
            } else {
                throw new WorkspaceIOException(to + " already exist! try use force.");
            }
        } else {
            throw new WorkspaceIOException(from + " no such file found!");
        }

    }

    public void copy(String from, String to, boolean force) throws IOException {
        Path fromPath = this.getPath(from);
        Path toPath = this.getPath(to);

        boolean fromFileExists = new File(fromPath.toString()).exists();
        boolean toFileExists = new File(toPath.toString()).exists();
        boolean fromFileIsDir = new File(fromPath.toString()).isDirectory();
        boolean toFileIsDir = new File(toPath.toString()).isDirectory();


        if (fromFileExists) {
            if (fromFileIsDir // copy 目录, 且目标目录已存在
                    && toFileExists
                    && toFileIsDir
                    && !force) {
                throw new WorkspaceIOException(to + " is a directory, and is already exist!");
            }

            if (!fromFileIsDir // copy 文件到目录, 且目标文件已存在
                    && toFileExists
                    && toFileIsDir
                    && !force) {

                File destFile = new File(toPath.toFile(), fromPath.toFile().getName());

                if (destFile.exists()) {
                    throw new WorkspaceIOException(to + " file already exist");
                }
            }

            if (!toFileIsDir // copy 文件, 且目标已经存在
                    && !toFileIsDir
                    && toFileExists
                    && !force) {
                throw new WorkspaceIOException(to + " file already exist!");
            }

            try {
                if (fromFileIsDir) {
                    FileUtils.copyDirectory(fromPath.toFile(), toPath.toFile());
                } else {
                    if (toFileIsDir) {
                        FileUtils.copyFileToDirectory(fromPath.toFile(), toPath.toFile());
                    } else {
                        FileUtils.copyFile(fromPath.toFile(), toPath.toFile());
                    }
                }
            } catch (Exception e) {
                throw new WorkspaceIOException(e);
            }
        } else {
            throw new WorkspaceIOException(from + " no such file found!");
        }

    }

    public void mkdir(String path) throws IOException {
        Path p = this.getPath(path);
        if (new File(p.toString()).exists()) {
            throw new WorkspaceIOException(path + " directory already exists!");
        } else {
            createDirectories(p);
        }
    }

    public void write(String path, String fileName, InputStream in) throws IOException {
        Path p = this.getPath(path);

        if (Files.isSymbolicLink(p) && !linkTargetExist(p)) {
            throw new WorkspaceIOException("It's a illegal link: target file not exist.");
        }

        if (!Files.exists(p)) {
            this.mkdir(path);
        }

        Path destFilePath = p.resolve(fileName);


        OutputStream out = new FileOutputStream(destFilePath.toFile());


        IOUtils.copy(in, out);
        in.close();
        out.close();
    }

    private void createDirectories(Path path) throws IOException {
        if (path == null) return;
        File file = path.toFile();
        if (file.exists() || Files.isSymbolicLink(path)) return;

        createDirectories(path.getParent());

        Files.createDirectory(path);
    }

    /**
     * 检查一个软链接是否是合法的。
     * 访问了根目录之外的文件；或者链接层数超过 LIMIT_NESTED_SYMLINK，都视为不合法，抛出异常。
     *
     * @return true if link is valid and target exist; false if target is not exist
     */
    public boolean linkTargetExist(Path startPath) throws IOException {
        int tryTimes = LIMIT_NESTED_SYMLINK;
        Path nextPath = startPath;
        while (tryTimes > 0) {
            Path symPath = Files.readSymbolicLink(nextPath);

            if (!symPath.isAbsolute()) {
                nextPath = nextPath.getParent().resolve(symPath).normalize().toAbsolutePath();
            } else {
                nextPath = symPath.normalize();
            }

            if (this.workingDir.relativize(nextPath).startsWith("../")) {
                return false;
            }

            if (!Files.exists(nextPath, LinkOption.NOFOLLOW_LINKS)) {
                return false;
            }

            if (!Files.isSymbolicLink(nextPath)) {
                return true;
            }

            tryTimes--;
        }

        throw new WorkspaceIOException("It's a illegal link: too many nested symlinks.");
    }

    public List<String> search(final String keyword, final boolean includeNonProjectItems) throws IOException {
        final List<String> result = Lists.newArrayList();

        final Path gitDir = workingDir.resolve(".git");

        Files.walkFileTree(workingDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.startsWith(gitDir)) {
                    return FileVisitResult.CONTINUE;
                }

                if (!attrs.isRegularFile()) {
                    return FileVisitResult.CONTINUE;
                }

                if (!includeNonProjectItems && isIgnored(file)) {
                    return FileVisitResult.CONTINUE;
                }

                String name = file.getName(file.getNameCount() - 1).toString();
                if (WildcardMatcher.match(name, keyword)) {
                    result.add("/" + workingDir.relativize(file).toString());
                }

                return FileVisitResult.CONTINUE;
            }
        });

        return result;
    }

    private boolean isIgnored(Path file) throws IOException {
        file = workingDir.relativize(file);
        int count = file.getNameCount();

        for (int i = count - 1; i >= 0; i--) {
            Path parent = i > 0 ? file.subpath(0, i) : Paths.get("");
            Path rest = file.subpath(i, count);

            if (!ignoreNodes.containsKey(parent.toString())) {
                File gitignoreFile = workingDir.resolve(Paths.get(parent.toString(), ".gitignore")).toFile();

                if (gitignoreFile.exists()) {
                    try (FileInputStream in = new FileInputStream(gitignoreFile)) {
                        IgnoreNode ignoreNode = new IgnoreNode();
                        ignoreNode.parse(in);
                        ignoreNodes.put(parent.toString(), ignoreNode);
                    }
                } else {
                    ignoreNodes.put(parent.toString(), new IgnoreNode());
                }

            }

            IgnoreNode ignoreNode = ignoreNodes.get(parent.toString());
            if (ignoreNode.isIgnored(rest.toString(), false) == IgnoreNode.MatchResult.IGNORED) {
                return true;
            }

        }

        return false;
    }

}
