package com.snowresorts.user.infrastructure.web;

import com.snowresorts.user.application.InternalApiProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class InternalApiFilterConfig {

    @Bean
    FilterRegistrationBean<InternalApiSecretFilter> internalApiSecretFilter(InternalApiProperties properties) {
        FilterRegistrationBean<InternalApiSecretFilter> registration =
                new FilterRegistrationBean<>(new InternalApiSecretFilter(properties));
        registration.addUrlPatterns("/snow-resort-service/v1/users/internal/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
