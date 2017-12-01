/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.config;

import com.fatboyindustrial.gsonjodatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.coding.ide.web.message.SpringfoxJsonToGsonAdapter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;
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
        converters.add(new ResourceHttpMessageConverter());

        GsonHttpMessageConverter converter = new GsonHttpMessageConverter();

        converter.setGson(gson());
        converters.add(converter);
    }

    @Bean
    public ModelMapper modelMapper(List<Converter> converters) {
        ModelMapper mapper = new ModelMapper();

        converters.stream().forEach(mapper::addConverter);

        return mapper;
    }

    private Gson gson() {
        final GsonBuilder builder = new GsonBuilder();
        // support swagger json
        builder.registerTypeAdapter(Json.class, new SpringfoxJsonToGsonAdapter());

        return Converters.registerDateTime(builder).create();
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        super.addViewControllers(registry);
        registry.addViewController("/ws/**").setViewName("forward:/workspace.html");
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

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(true);
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseSuffixPatternMatch(false);
        configurer.setUseRegisteredSuffixPatternMatch(false);
    }
}
