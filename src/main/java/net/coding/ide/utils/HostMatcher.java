/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.utils;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

/**
 * Created by vangie on 15/1/28.
 */
public class HostMatcher {

    private Pattern hostPattern;

    public HostMatcher(String host) {
        this.hostPattern = buildPattern(host);
    }

    public boolean matches(HttpServletRequest request) {
        String host = request.getHeader("Host");
        if(host == null){
            return false;
        }
        return hostPattern.matcher(host).matches();
    }

    private Pattern buildPattern(String host){
        return Pattern.compile(host.replaceAll("[.]","[.]").replaceAll("[*]",".+"));
    }
}
