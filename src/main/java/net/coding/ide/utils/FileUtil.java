/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.utils;

import com.google.common.collect.Maps;
import org.apache.tika.Tika;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.util.Map;

/**
 * Created by phy on 2015/1/27.
 */
public abstract class FileUtil{
    private static MimetypesFileTypeMap mfm = new MimetypesFileTypeMap();

    private static Map<String, String> contentTypeMap = Maps.newHashMap();

    private static Tika tika = new Tika();

    static {
        contentTypeMap.put(".gitignore", "text/plain");
        contentTypeMap.put(".bowerrc", "text/plain");
        contentTypeMap.put(".editorconfig", "text/plain");
        contentTypeMap.put(".gitattributes", "text/plain");
        contentTypeMap.put("Rakefile", "text/x-extension-rake");
        contentTypeMap.put("Dockerfile", "text/x-extension-docker");
        contentTypeMap.put("Gemfile", "text/x-ruby-bundler-gemfile");
        contentTypeMap.put("Gemfile.lock", "text/x-ruby-bundler-gemfile-lock");
    }

    public static String getContentType(File file) {

        if (!file.isDirectory()){
            String filename = file.getName();

            if (contentTypeMap.containsKey(filename)) {
                return contentTypeMap.get(filename);
            } else if (filename.indexOf('.') != -1){
                String contentType =  mfm.getContentType(filename);
                if (!contentType.equals("application/octet-stream")){
                    return contentType;
                } else return detectContentTypeByContent(file);
            } else{
                return detectContentTypeByContent(file);
            }
        } else {
            return null;
        }

    }

    private static String detectContentTypeByContent(File file) {
        if (file.length() == 0){
            return "text/plain";
        } else {
            try {
                return tika.detect(file);
            } catch (Exception e) {
                return "application/octet-stream";
            }
        }

    }
}
