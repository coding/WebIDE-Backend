/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.config;

import com.fatboyindustrial.gsonjodatime.Converters;
import com.google.gson.GsonBuilder;
import net.coding.ide.web.message.GsonMessageConverter;
import net.coding.ide.web.message.SessionCacheWebSocketHandler;
import net.coding.ide.web.message.SpaceKeyHandshakeInterceptor;
import net.coding.ide.web.message.WebSocketSessionStore;
import org.atmosphere.cpr.AtmosphereServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.List;

/**
 * Created by vangie on 15/1/30.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {

    @Value("#{'${ALLOWED_ORIGINS}'.split(',')}")
    private String[] allowedOrigins;

    @Value("${SPACE_HOME}")
    private String spaceHome;

    @Autowired
    private WebSocketSessionStore webSocketSessionStore;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/sockjs")
                .addInterceptors(new SpaceKeyHandshakeInterceptor())
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(handler -> new SessionCacheWebSocketHandler(handler, webSocketSessionStore));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app")
                .enableSimpleBroker("/queue", "/topic");
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        messageConverters.add(new StringMessageConverter());
        messageConverters.add(new ByteArrayMessageConverter());

        GsonMessageConverter gsonMessageConverter = new GsonMessageConverter();
        gsonMessageConverter.setGson(Converters.registerDateTime(new GsonBuilder()).create());
        messageConverters.add(gsonMessageConverter);

        return false;
    }

    @Bean // for socket.io
    public ServletRegistrationBean servletRegistrationBean() {
        ServletRegistrationBean bean = new ServletRegistrationBean(new AtmosphereServlet(), "/coding-ide-tty1/*");

        bean.addInitParameter("socketio-transport", "websocket");
        bean.addInitParameter("socketio-timeout", "25000");
        bean.addInitParameter("socketio-heartbeat", "15000");
        bean.addInitParameter("socketio-suspendTime", "30000");
        bean.addInitParameter("org.atmosphere.cpr.sessionSupport", "true");
        bean.addInitParameter("SPACE_HOME", spaceHome);
        bean.setLoadOnStartup(100);
        bean.setAsyncSupported(true);

        return bean;
    }
}

