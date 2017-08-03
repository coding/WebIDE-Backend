package net.coding.ide.service;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import net.coding.ide.dto.Package;
import net.coding.ide.model.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service
public class PackageServiceImpl implements PackageService {

    private Path packagesDir;

    private Long lastModified;

    private Gson gson = new Gson();

    public PackageServiceImpl(@Value("${PACKAGE_HOME}") File packagesDir) {
        this.packagesDir = packagesDir.toPath();
    }

    private Package toPackage(Path path) {
        JsonReader reader = null;
        try {
            reader = new JsonReader(new FileReader(path.toFile()));
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            Package p = gson.fromJson(json.get("meta"), Package.class);
            p.setRequirement(Package.Requirement.Required);
            return p;
        } catch (Exception e) {
            return null;
        }
    }

    private List<Package> packages = Lists.newArrayList();

    public synchronized List<Package> findAll() {

        try {
            if (lastModified == null
                    || lastModified.longValue() != packagesDir.toFile().lastModified()) {
                lastModified = packagesDir.toFile().lastModified();


                packages = Files.walk(packagesDir)
                        .filter(path -> path.toFile().getName().endsWith("manifest.json"))
                        .map(this::toPackage)
                        .filter(p -> p != null)
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            return Lists.newArrayList();
        }

        return packages;
    }

    public String readPackageFile(String name, String version, String file) throws IOException {

        Path path = new File(packagesDir.toFile(), format("%s/%s/%s", name, version, file)).toPath();

        if ( ! path.toFile().exists() ) {
            throw new NotFoundException(format("file %s could not be found", path.toFile().getAbsolutePath()));
        }

        byte[] bytes = Files.readAllBytes(path);

        return new String(bytes);
    }
}
