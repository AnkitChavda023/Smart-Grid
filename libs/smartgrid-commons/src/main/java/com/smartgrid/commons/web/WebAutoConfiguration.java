package com.smartgrid.commons.web;

import com.smartgrid.commons.tracing.HttpServerTracingFilter;
import com.smartgrid.commons.tracing.TracingAutoConfiguration;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@AutoConfiguration(before = WebMvcAutoConfiguration.class, after = TracingAutoConfiguration.class)
public class WebAutoConfiguration {

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>(new CorrelationIdFilter());
        registration.setOrder(Integer.MIN_VALUE);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    @ConditionalOnBean(OpenTelemetry.class)
    public FilterRegistrationBean<HttpServerTracingFilter> httpServerTracingFilter(OpenTelemetry openTelemetry) {
        FilterRegistrationBean<HttpServerTracingFilter> registration =
                new FilterRegistrationBean<>(new HttpServerTracingFilter(openTelemetry));
        registration.setOrder(Integer.MIN_VALUE + 1);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnClass(ObjectOptimisticLockingFailureException.class)
    public OptimisticLockExceptionHandler optimisticLockExceptionHandler() {
        return new OptimisticLockExceptionHandler();
    }
}
