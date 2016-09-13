/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.web.message;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Created by vangie on 15/5/24.
 */
public class SpaceKeyHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {

        String sockJsPath = getSockJsPath(((ServletServerHttpRequest)request).getServletRequest());

        String[] pathSegments = StringUtils.tokenizeToStringArray(sockJsPath.substring(1), "/");

        attributes.put("spaceKey", pathSegments[0]);

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // Empty
    }

    private String getSockJsPath(HttpServletRequest servletRequest) {
        String attribute = HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE;
        String path = (String) servletRequest.getAttribute(attribute);
        return ((path.length() > 0) && (path.charAt(0) != '/')) ? "/" + path : path;
    }
}
