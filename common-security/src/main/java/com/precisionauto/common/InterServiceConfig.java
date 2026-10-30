package com.precisionauto.common;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Load-balanced RestClient (resolves "http://record-service" through Eureka + Spring Cloud LoadBalancer)
 * that forwards the caller's JWT so downstream services authenticate the same user.
 */
@AutoConfiguration(before = RestClientAutoConfiguration.class)
public class InterServiceConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder().requestInterceptor((request, body, execution) -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                String auth = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (auth != null) request.getHeaders().set(HttpHeaders.AUTHORIZATION, auth);
            }
            return execution.execute(request, body);
        });
    }
}
