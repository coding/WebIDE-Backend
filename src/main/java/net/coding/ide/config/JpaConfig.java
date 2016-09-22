/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Created by vangie on 14/12/4.
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories("net.coding.ide.repository")
@EntityScan("net.coding.ide.entity")
@EnableTransactionManagement
public class JpaConfig {

    /**
     * ConfigurationProperties not support spel
     * @see <a href="http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-spel-conditions>Spring Boot Reference</a>
     */
    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${CODING_IDE_HOME}")
    private String codingIdeHome;

    @PostConstruct
    public void init() throws IOException {

        File file = new File(codingIdeHome);

        if (!file.exists()) {
            Files.createDirectories(file.toPath());
        }
    }

    @Bean
    public DataSource dataSource() {
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName(driverClassName);
        dataSourceBuilder.url(url);
        dataSourceBuilder.type(HikariDataSource.class);
        return dataSourceBuilder.build();
    }


}
