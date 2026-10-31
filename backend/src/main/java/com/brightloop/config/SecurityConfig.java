package com.brightloop.config;

/**
 * This class intentionally left without @Configuration.
 *
 * The authoritative Spring Security configuration is in:
 *   com.brightloop.subscription.config.SecurityConfig
 *
 * That class handles JWT authentication, role-based access control,
 * CORS, and all security filter chain setup.
 *
 * Having two @Configuration @EnableWebSecurity classes causes duplicate
 * SecurityFilterChain bean conflicts and the permissive "allow all" rule
 * here would override JWT protection. Disabled by removing @Configuration.
 */
public class SecurityConfig {
    // Disabled — see com.brightloop.subscription.config.SecurityConfig
}
