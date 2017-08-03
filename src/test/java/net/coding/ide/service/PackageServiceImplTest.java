package net.coding.ide.service;

import net.coding.ide.dto.Package;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static net.coding.ide.utils.FilesUtils.createTempDirectoryAndDeleteOnExit;
import static org.junit.Assert.assertEquals;

public class PackageServiceImplTest {


    private Path packages = createTempDirectoryAndDeleteOnExit("packages").toPath();

    private PackageServiceImpl packageService = new PackageServiceImpl(packages.toFile());

    @Test
    public void testFindNoneExistPackages() {
        List<Package> packages = packageService.findAll();
        assertEquals(0, packages.size());
    }

    @Test
    public void testFindExistPackages() throws IOException {

        File package1 = new File(packages.toFile(), "temporary/0.0.1-alpha.03/manifest.json");

        createPackage(package1, "{\n" +
                "    \"meta\": {\n" +
                "        \"name\": \"temporary\",\n" +
                "        \"version\": \"0.0.1-alpha.03\",\n" +
                "        \"description\": \"WebIDE-Plugin-Temporary for Coding WebIDE\",\n" +
                "        \"author\": \"candy\",\n" +
                "        \"displayName\": \"temporary\"\n" +
                "    }\n" +
                "}");

        File package2 = new File(packages.toFile(), "env/0.0.1-alpha06/manifest.json");

        createPackage(package2, "{\n" +
                "    \"meta\": {\n" +
                "        \"name\": \"env\",\n" +
                "        \"version\": \"0.0.1-alpha06\",\n" +
                "        \"description\": \"WebIDE-Plugin-Env for Coding WebIDE\",\n" +
                "        \"author\": \"candy\",\n" +
                "        \"displayName\": \"Environment\"\n" +
                "    }\n" +
                "}");

        List<Package> packages = packageService.findAll();

        assertEquals(2, packages.size());
    }


    private void createPackage(File manifest, String content) throws IOException {

        Files.createDirectories(new File(manifest.getParent()).toPath());

        Files.write(manifest.toPath(), content.getBytes());
    }

}