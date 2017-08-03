package net.coding.ide.service;


import net.coding.ide.dto.Package;

import java.io.IOException;
import java.util.List;

public interface PackageService {

    List<Package> findAll();

    String readPackageFile(String name, String version, String file) throws IOException;
}
