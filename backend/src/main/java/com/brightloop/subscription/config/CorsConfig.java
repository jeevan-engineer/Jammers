package com.brightloop.subscription.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for the REST API.
 *
 * CorsConfigurationSource bean — Spring Security's CORS support reads
 * this bean (configured via SecurityConfig's .cors()) to handle pre-flight
 * OPTIONS requests and apply CORS headers to responses.
 *
 * app.cors.allowed-origins is a comma-separated list in application.properties.
 * @Value with split() converts it to a List for the CORS config.
 *
 * Common error: not exposing the Authorization header prevents the browser
 * from reading it in XHR/fetch responses (relevant for login).
 * Not allowing OPTIONS causes pre-flight requests to be rejected → 403.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Split comma-separated origins from properties.
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));

        // All HTTP methods the frontend will use.
        config.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));

        // Headers the frontend can send.
        config.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE
        ));

        // Allow cookies/credentials (needed if frontend uses Authorization header).
        config.setAllowCredentials(true);

        // Expose Authorization header to the browser (for login response reading).
        config.setExposedHeaders(List.of(HttpHeaders.AUTHORIZATION));

        // Cache pre-flight response for 1 hour to reduce OPTIONS requests.
        config.setMaxAge(3600L);

        // Apply this configuration to all URL patterns.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
