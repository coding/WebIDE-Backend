/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static springfox.documentation.builders.RequestHandlerSelectors.withClassAnnotation;

/**
 * Created by tan on 16/8/25.
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket petApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(withClassAnnotation(RestController.class))
                .build()
                .pathMapping("/")
                .enableUrlTemplating(true)
                .apiInfo(apiInfo())
                .ignoredParameterTypes(
                        HttpServletRequest.class,
                        HttpServletResponse.class,
                        HttpSession.class,
                        Pageable.class,
                        Errors.class
                );
    }

    private ApiInfo apiInfo() {
        ApiInfo apiInfo = new ApiInfo(
                "WebIDE REST API",
                "WebIDE Rest Api document",
                "1.0",
                "https://coding.net",
                "CODING",
                "BSD",
                "http://www.freebsd.org/copyright/freebsd-license.html"
        );
        return apiInfo;
    }
}
