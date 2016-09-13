/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.config;

import com.fatboyindustrial.gsonjodatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.coding.ide.web.message.SpringfoxJsonToGsonAdapter;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.filter.OrderedHttpPutFormContentFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.filter.HttpPutFormContentFilter;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import springfox.documentation.spring.web.json.Json;

import java.util.List;

/**
 * Created by vangie on 14/11/12.
 */
@Configuration
@EnableSpringDataWebSupport
@ComponentScan(basePackages = "net.coding.ide.web")
public class WebConfig extends WebMvcConfigurerAdapter {

    @Value("#{'${ALLOWED_ORIGINS}'.split(',')}")
    private String[] allowedOrigins;

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        GsonHttpMessageConverter converter = new GsonHttpMessageConverter();
        converter.setGson(gson());
        converters.add(converter);
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    private Gson gson() {
        final GsonBuilder builder = new GsonBuilder();
        // support swagger json
        builder.registerTypeAdapter(Json.class, new SpringfoxJsonToGsonAdapter());

        return Converters.registerDateTime(builder).create();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("POST", "GET", "PUT", "OPTIONS", "DELETE")
                .maxAge(3600)
                .allowedHeaders("X-Requested-With", "X-Credentials", "X-Sharding-Group", "X-Space-Key", "content-type")
                .exposedHeaders("Requests-Auth", "Requests-Auth-Url", "Requests-Auth-Return-Url")
                .allowCredentials(true);
    }
}
