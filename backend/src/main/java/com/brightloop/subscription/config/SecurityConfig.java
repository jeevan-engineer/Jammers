package com.brightloop.subscription.config;

import com.brightloop.subscription.security.JwtAuthFilter;
import com.brightloop.subscription.security.RestAccessDeniedHandler;
import com.brightloop.subscription.security.RestAuthenticationEntryPoint;
import com.brightloop.subscription.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration — Spring Boot 3.x / Spring Security 6.x style.
 *
 * Key design decisions:
 *
 * 1. @EnableMethodSecurity — activates @PreAuthorize on service/controller methods
 *    (replaces the deprecated @EnableGlobalMethodSecurity).
 *
 * 2. SecurityFilterChain bean — the modern way to configure security;
 *    WebSecurityConfigurerAdapter was removed in Spring Security 6.
 *
 * 3. SessionCreationPolicy.STATELESS — no HttpSession; every request must
 *    carry a valid JWT. The server never stores session state.
 *
 * 4. CSRF disabled — stateless REST APIs don't need CSRF protection
 *    because they don't use cookies for authentication.
 *
 * 5. DaoAuthenticationProvider — wires our CustomUserDetailsService and
 *    BCryptPasswordEncoder into the standard authentication flow used by
 *    AuthenticationManager.authenticate() in AuthServiceImpl.
 *
 * Common error: if JwtAuthFilter is not added BEFORE
 * UsernamePasswordAuthenticationFilter, the filter won't run and all
 * protected requests will be rejected as unauthenticated.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint authEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    /** Public endpoints — no JWT required. */
    private static final String[] PUBLIC_PATHS = {
            "/api/auth/signup",
            "/api/auth/login",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/v3/api-docs"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — not needed for stateless JWT REST API.
            .csrf(AbstractHttpConfigurer::disable)

            // Allow all CORS pre-flight OPTIONS requests (actual CORS config is in CorsConfig).
            .cors(cors -> {})

            // Stateless session — no server-side session storage.
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Custom 401/403 handlers returning ApiError JSON.
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(authEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))

            // Authorization rules.
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PUBLIC_PATHS).permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .anyRequest().authenticated()
            )

            // Wire our DaoAuthenticationProvider.
            .authenticationProvider(authenticationProvider())

            // JWT filter runs before Spring's username/password filter.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * DaoAuthenticationProvider links our UserDetailsService and PasswordEncoder.
     * Spring Security calls this during AuthenticationManager.authenticate().
     * Spring Security 7: the UserDetailsService is passed to the constructor.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes the AuthenticationManager bean so AuthServiceImpl can inject it
     * directly and call .authenticate() on login.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCryptPasswordEncoder — strength 10 (default).
     * Single bean shared across the entire application.
     * Never store plain-text passwords; always encode before persisting.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
