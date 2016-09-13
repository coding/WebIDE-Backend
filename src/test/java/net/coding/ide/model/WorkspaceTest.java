/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.model;

import com.google.common.collect.Lists;
import net.coding.ide.entity.ProjectEntity;
import net.coding.ide.entity.WorkspaceEntity;
import net.coding.ide.model.exception.WorkspaceIOException;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.io.*;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static net.coding.ide.utils.FilesUtils.createTempDirectoryAndDeleteOnExit;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class WorkspaceTest {

    private Workspace ws;

    private File testSpaceHome;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws IOException {
        testSpaceHome = createTempDirectoryAndDeleteOnExit("codingSpaceKeys");

        WorkspaceEntity wsEntity = new WorkspaceEntity();
        wsEntity.setSpaceKey("qwerty");
        wsEntity.setProject(new ProjectEntity());

        ws = Mockito.spy(new Workspace(wsEntity, testSpaceHome));
    }

    @Test
    public void testReadAndWrite() throws Exception {
        ws.create("a.txt");
        ws.write("a.txt", "hello coding!", false, true, true);

        assertEquals("hello coding!", ws.read("a.txt", false));
    }


    @Test
    public void testReadAndWriteBase64() throws Exception {
        ws.create("a.txt");
        ws.write("a.txt", "aGVsbG8gd29ybGQ=", true, true, true);

        assertEquals("hello world", ws.read("a.txt", false));
    }


    @Test
    public void testCreate() throws Exception {
        ws.create("create.txt");

        File createdFile = new File(ws.getWorkingDir(), "create.txt");

        assertTrue(createdFile.exists());
    }

    @Test
    public void testRemove() throws Exception {
        ws.create("create.txt");
        File createdFile = new File(ws.getWorkingDir(), "create.txt");
        assertTrue(createdFile.exists());

        ws.remove("create.txt", false);

        assertTrue(!createdFile.exists());
    }

    @Test
    public void testRemoveDirectory() throws IOException {
        ws.create("./mk/a.txt");
        File file = new File(ws.getWorkingDir(), "mk/a.txt");
        assertTrue(file.exists());
        ws.remove("./mk", true);
        assertFalse(file.exists());
    }

    @Test
    public void testMove() throws Exception {
        ws.create("./mk/a.txt");
        File fromFile = new File(ws.getWorkingDir(), "mk/a.txt");
        File toFile = new File(ws.getWorkingDir(), "mk1/a.txt");

        ws.move("./mk/a.txt", "./mk1/a.txt", true);

        assertTrue(!fromFile.exists());
        assertTrue(toFile.exists());
    }

    @Test
    public void testCopyDir() throws Exception {
        ws.create("./mv/create.txt");
        File fromFile = new File(ws.getWorkingDir(), "mv");
        File toFile = new File(ws.getWorkingDir(), "cp");

        assertTrue(fromFile.exists());
        assertTrue(!toFile.exists());

        ws.copy("./mv", "./cp", true);

        assertTrue(fromFile.exists());
        assertTrue(toFile.exists());
    }

    @Test(expected = WorkspaceIOException.class)
    public void testCopyDirIfDestExist() throws Exception {
        ws.create("./mv/create.txt");
        ws.create("./cp/a.txt");

        File fromFile = new File(ws.getWorkingDir(), "mv");
        File toFile = new File(ws.getWorkingDir(), "cp");

        assertTrue(fromFile.exists());
        assertTrue(toFile.exists());

        ws.copy("./mv", "./cp", false);
    }

    @Test
    public void testCopyDirIfDestExistAndOverwrite() throws Exception {
        ws.create("./mv/create.txt");
        ws.create("./cp/a.txt");
        File fromFile = new File(ws.getWorkingDir(), "mv");
        File toFile = new File(ws.getWorkingDir(), "cp");

        assertTrue(fromFile.exists());
        assertTrue(toFile.exists());

        ws.copy("./mv", "./cp", true);

        assertTrue(fromFile.exists());
        assertTrue(toFile.exists());
        assertTrue(new File(toFile, "create.txt").exists());
        assertTrue(new File(toFile, "a.txt").exists());
    }

    @Test
    public void testCopyFileToDir() throws IOException {
        ws.create("create.txt");
        ws.mkdir("cp");

        File fromFile = new File(ws.getWorkingDir(), "create.txt");
        File toFile = new File(ws.getWorkingDir(), "cp");

        assertTrue(fromFile.exists());
        assertTrue(toFile.isDirectory());
        assertTrue(toFile.exists());

        ws.copy("./create.txt", "./cp", true);

        assertTrue(fromFile.exists());
        assertTrue(toFile.isDirectory());
        assertTrue(toFile.exists());
    }

    @Test(expected = WorkspaceIOException.class)
    public void testCopyFileToDirIfDestDirExist() throws IOException {
        ws.create("create.txt");
        ws.mkdir("cp/create.txt");

        File fromFile = new File(ws.getWorkingDir(), "create.txt");
        File toFile = new File(ws.getWorkingDir(), "cp");

        assertTrue(fromFile.exists());
        assertTrue(toFile.isDirectory());
        assertTrue(toFile.exists());

        ws.copy("./create.txt", "./cp", false);
    }


    @Test
    public void testCopyFileToDirIfDestDirExistAndOverwrite() throws IOException {
        ws.create("create.txt");
        ws.write("cp/create.txt", "test", false, true, true);

        File fromFile = new File(ws.getWorkingDir(), "create.txt");
        File toFile = new File(ws.getWorkingDir(), "cp");

        assertTrue(fromFile.exists());
        assertTrue(toFile.isDirectory());
        assertTrue(new File(toFile, "create.txt").exists());

        ws.copy("./create.txt", "./cp", true);

        assertTrue(fromFile.exists());
        assertTrue(toFile.isDirectory());
        assertTrue(new File(toFile, "create.txt").exists());
    }

    @Test
    public void testCopyFile() throws Exception {
        ws.create("./mk/a.txt");
        File fromFile = new File(ws.getWorkingDir(), "mk/a.txt");
        File toFile = new File(ws.getWorkingDir(), "cp/a.txt");

        ws.move("./mk/a.txt", "./cp/a.txt", true);

        assertTrue(!fromFile.exists());
        assertTrue(toFile.exists());
    }

    @Test(expected = WorkspaceIOException.class)
    public void testCopyFileIfDestFileExist() throws IOException {
        ws.create("./mk/a.txt");
        ws.create("./cp/a.txt");

        ws.move("./mk/a.txt", "./cp/a.txt", false);
    }

    @Test
    public void testMkdir() throws Exception {
        ws.mkdir("mk");
        File file = new File(ws.getWorkingDir(), "mk");
        assertTrue(file.exists());
    }


    @Test
    public void testGetPath() throws Exception {
        assertEquals(Paths.get(testSpaceHome.getAbsolutePath(), "working-dir/user/tar").toAbsolutePath(), ws.getPath("./user/tar"));
        assertEquals(Paths.get(testSpaceHome.getAbsolutePath(), "working-dir/user/tar").toAbsolutePath(), ws.getPath("./user/././tar"));
        assertEquals(Paths.get(testSpaceHome.getAbsolutePath(), "working-dir/tar").toAbsolutePath(), ws.getPath("./user/../tar"));
        assertEquals(Paths.get(testSpaceHome.getAbsolutePath(), "working-dir/.").toAbsolutePath(), ws.getPath("."));
        assertEquals(Paths.get(testSpaceHome.getAbsolutePath(), "working-dir/.").toAbsolutePath(), ws.getPath("/"));
    }

    @Test
    public void testGetPathForAccessDenied() throws AccessDeniedException {
        exception.expect(AccessDeniedException.class);
        exception.expectMessage(is("./.././src"));

        ws.getPath("./.././src");
    }

    @Test
    public void testGetRelativePath() throws Exception {
        assertEquals(Paths.get("user/tar"), ws.getRelativePath("./user/tar"));
        assertEquals(Paths.get("user/tar"), ws.getRelativePath("user/././tar"));
        assertEquals(Paths.get("tar"), ws.getRelativePath("./user/../tar"));
        assertEquals(Paths.get("."), ws.getRelativePath("."));
        assertEquals(Paths.get("."), ws.getRelativePath("/"));

    }

    @Test
    public void testGetRelativePathPathForAccessDenied() throws AccessDeniedException {
        exception.expect(AccessDeniedException.class);
        exception.expectMessage(is("./.././src"));

        ws.getRelativePath("./.././src");
    }

    @Test
    public void testGetNormalizePath() throws Exception {
        assertEquals(Paths.get("/user/tar"), ws.getNormalizePath("./user/tar"));
        assertEquals(Paths.get("/user/tar"), ws.getNormalizePath("user/tar"));
        assertEquals(Paths.get("/user/tar"), ws.getNormalizePath("user/././tar"));
        assertEquals(Paths.get("/tar"), ws.getNormalizePath("user/../tar"));
        assertEquals(Paths.get("/"), ws.getNormalizePath("/"));
        assertEquals(Paths.get("/"), ws.getNormalizePath("."));
    }

    @Test
    public void testGetNormalizePathForAccessDenied() throws AccessDeniedException {
        exception.expect(AccessDeniedException.class);
        exception.expectMessage(is("./.././src"));

        ws.getNormalizePath("./.././src");
    }


    @Test
    public void testWriteUpload() throws Exception {
        InputStream fileInputStream = new FileInputStream("./src/test/resources/workspace/upload.txt");
        InputStream in = new ByteArrayInputStream(IOUtils.toByteArray(fileInputStream));

        ws.write(".", "upload.txt", in);

        File uploadFile = new File(ws.getWorkingDir(), "upload.txt");
        assertTrue(uploadFile.exists());
    }

    @Test
    public void testCheckLinkOk() throws IOException {
        ws.create("b");
        Files.createSymbolicLink(Paths.get(testSpaceHome.getAbsolutePath(), "working-dir/a"),
                Paths.get(testSpaceHome.getAbsolutePath(), "working-dir/b").toAbsolutePath());

        ws.linkTargetExist(ws.getPath("a"));
    }

    @Test
    public void testCheckLinkForNestedLink() throws IOException {

        ws.create("a");

        Files.createSymbolicLink(Paths.get(ws.getWorkingDir().getAbsolutePath(), "0"),
                Paths.get(ws.getWorkingDir().getAbsolutePath(), "a").toAbsolutePath());

        for (int i = 0; i < 30; i++) {

            Files.createSymbolicLink(Paths.get(ws.getWorkingDir().getAbsolutePath(), "" + (i + 1)),
                    Paths.get(ws.getWorkingDir().getAbsolutePath(), "" + i).toAbsolutePath());

        }

        exception.expect(WorkspaceIOException.class);
        exception.expectMessage("It's a illegal link: too many nested symlinks.");
        ws.linkTargetExist(ws.getPath("30"));
    }

    @Test //不支持访问 workspace 上层目录
    public void testCheckLinkForAccessDenied() throws IOException {

        Files.createSymbolicLink(Paths.get(testSpaceHome.getAbsolutePath(), "working-dir/a"),
                Paths.get(testSpaceHome.getAbsolutePath()).toAbsolutePath());

        assertEquals(false, ws.linkTargetExist(ws.getPath("a")));
    }

    @Test
    public void testCheckLinkForAccessDeniedTryTwoTimes() throws IOException {

        Files.createSymbolicLink(Paths.get(testSpaceHome.getAbsolutePath(), "working-dir/b"),
                Paths.get(testSpaceHome.getAbsolutePath()).toAbsolutePath());

        Files.createSymbolicLink(Paths.get(testSpaceHome.getAbsolutePath(), "working-dir/a"),
                Paths.get(testSpaceHome.getAbsolutePath(), "working-dir/b").toAbsolutePath());

        assertEquals(false, ws.linkTargetExist(ws.getPath("a")));
    }

    @Test
    public void testCheckLinkForFileNotExist() throws IOException {

        Files.createSymbolicLink(Paths.get(ws.getWorkingDir().getAbsolutePath(), "a"),
                Paths.get(ws.getWorkingDir().getAbsolutePath(), "b").toAbsolutePath());

        Assert.assertEquals(ws.linkTargetExist(ws.getPath("a")), false);
    }

    @Test
    public void testSearch() throws IOException {

        ws.create("c/a.txt");
        ws.create("a/c.txt");
        ws.create("A1.txt");
        ws.create("a.txt");
        ws.create("b.wma");

        List<String> expected = Lists.newArrayList("/c/a.txt", "/A1.txt", "/b.wma", "/a.txt");
        List<String> actual = ws.search("a", true);

        assertTrue(actual.containsAll(expected) && expected.containsAll(actual));
    }

    @Test
    public void testWildcardSearch() throws IOException {
        ws.create("abc.cc");
        ws.create("abc.cxx");
        ws.create("abc.c");
        ws.create("vvvabc.c");
        ws.create("bc.c");
        ws.create("vvvbc.c");

        List<String> expected = Lists.newArrayList("/abc.cc", "/abc.cxx", "/abc.c", "/vvvabc.c");
        List<String> actual = ws.search("abc.c*", true);

        assertTrue(actual.containsAll(expected) && expected.containsAll(actual));
    }

    @Test
    public void testSearchOnIgnore() throws IOException {
        ws.create(".gitignore");
        ws.write(".gitignore", "a.cxx\n*.java\ntmp/\n**/build/asserts", false, true, true);
        ws.create("a.css");
        ws.create("a.cxx");
        ws.create("b.java");
        ws.create("k/c.java");
        ws.create("tmp/a.css");
        ws.create("build/asserts/a.css");
        ws.create("debug/build/asserts/a.css");

        ws.create("dev/.gitignore");
        ws.write("dev/.gitignore", "*.css", false, true, true);
        ws.create("dev/a.css");
        ws.create("dev/a.m");

        List<String> expected = Lists.newArrayList("/a.css", "/dev/a.m");
        List<String> actual = ws.search("a", false);

        assertTrue(actual.containsAll(expected) && expected.containsAll(actual));
    }

    @Test
    public void testPack() throws IOException, CompressorException {
        ws.create("c/a.txt");
        ws.create("a/c.txt");
        ws.create("中文文件名.txt");
        ws.create("a.txt");
        ws.create("b.wma");

        ws.write("a.txt","111",false,true,true);


        File targetFile = new File("./target/ws.tar.gz");

        try (OutputStream out = new FileOutputStream(targetFile)) {
            ws.pack("/", out);
        }

        assertEntry(targetFile, "a/c.txt");
        assertEntry(targetFile, "中文文件名.txt");

        targetFile.delete();
    }

    private static void assertEntry(File file, String name) {
        boolean exist = false;

        try (TarArchiveInputStream tar = new TarArchiveInputStream(
                new GzipCompressorInputStream(
                                new FileInputStream(file)))) {


            ArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                if (entry.getName().equals(name)) {
                    exist = true;
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        if (!exist) {
            fail("zip entry " + name + " not exist!");
        }

    }

}
