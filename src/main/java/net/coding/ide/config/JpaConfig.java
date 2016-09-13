/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Created by vangie on 14/12/4.
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories("net.coding.ide.repository")
@EntityScan("net.coding.ide.entity")
@EnableTransactionManagement
public class JpaConfig {

}
