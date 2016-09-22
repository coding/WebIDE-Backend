/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

/**
 * Created by vangie on 16/1/18.
 */
@SpringBootApplication
public class Application {

    @Value("${PTY_LIB_FOLDER}")
    private String ptyLibFolder;

    public static void main(String[] args) throws URISyntaxException {
        SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    public void init() throws IOException {
        System.setProperty("PTY_LIB_FOLDER", ptyLibFolder);
    }
}
