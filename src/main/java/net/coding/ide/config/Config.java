/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.config;

import net.coding.ide.Application;
import net.coding.ide.git.MultiUserSshSessionFactory;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.request.RequestContextListener;

import java.util.regex.Pattern;

/**
 * Created by vangie on 14/11/12.
 */

@Configuration
@ComponentScan(basePackages = "net.coding.ide", excludeFilters = {
        @Filter(type = FilterType.ANNOTATION, value = Controller.class),
        @Filter(type = FilterType.ANNOTATION, value = Repository.class),
        @Filter(type = FilterType.CUSTOM, value = Config.WebBeanFilter.class),
        @Filter(type = FilterType.CUSTOM, value = Config.ConfigBeanFilter.class),
        @Filter(type = FilterType.ASSIGNABLE_TYPE, value = Application.class)
})
public class Config {

    static class WebBeanFilter extends RegexPatternTypeFilter {
        public WebBeanFilter() {
            super(Pattern.compile("net\\.coding\\.ide\\.web\\..*"));
        }
    }

    static class ConfigBeanFilter extends RegexPatternTypeFilter {
        public ConfigBeanFilter() {
            super(Pattern.compile("net\\.coding\\.ide\\.config\\..*"));
        }
    }

    static {
        SshSessionFactory.setInstance(new MultiUserSshSessionFactory());
    }
}


