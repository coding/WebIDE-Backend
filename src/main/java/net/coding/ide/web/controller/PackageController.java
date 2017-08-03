package net.coding.ide.web.controller;

import net.coding.ide.dto.Package;
import net.coding.ide.service.PackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * Created by tan on 09/03/2017.
 */
@RequestMapping("/packages")
@RestController
public class PackageController {

    @Autowired
    private PackageService packageService;

    @RequestMapping(value = "/{name}/{version}/{fileName:.+}", method = RequestMethod.GET, produces = "text/plain")
    public String readPackageFile(@PathVariable String name,
                                  @PathVariable String version,
                                  @PathVariable String fileName) throws IOException {
        return packageService.readPackageFile(name, version, fileName);
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public List<Package> packages() {
        return packageService.findAll();
    }
}
