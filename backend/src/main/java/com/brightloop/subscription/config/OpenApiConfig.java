package com.brightloop.subscription.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc / OpenAPI 3 configuration.
 *
 * @OpenAPIDefinition — sets global API metadata visible at /v3/api-docs.
 * @SecurityScheme — registers "bearerAuth" as a JWT bearer scheme.
 *   - type=HTTP, scheme="bearer", bearerFormat="JWT" tells Swagger UI
 *     to show the "Authorize 🔒" button and prefix tokens with "Bearer ".
 *
 * @SecurityRequirement(name = "bearerAuth") on this class sets the default
 * security requirement for all operations; individual public endpoints
 * can override with @SecurityRequirement(name = "").
 *
 * To use in Swagger UI:
 *   1. POST /api/auth/login → copy the "token" from the response.
 *   2. Click "Authorize" → paste the token → click "Authorize".
 *   3. All subsequent requests will include the Authorization: Bearer <token> header.
 *
 * Common error: if PUBLIC_PATHS in SecurityConfig don't include /v3/api-docs/**
 * and /swagger-ui/**, Swagger UI itself returns 401.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Brightloop Subscription Management API",
                version = "1.0.0",
                description = "REST API for the Brightloop Subscription Management System. " +
                              "Authenticate via POST /api/auth/login, then click Authorize and paste your JWT.",
                contact = @Contact(
                        name = "Team Jammers",
                        email = "admin@brightloop.com"
                )
        ),
        servers = @Server(url = "http://localhost:8080", description = "Local Development"),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "Paste your JWT token (without 'Bearer ' prefix) into the field below."
)
public class OpenApiConfig {
    // No beans needed — SpringDoc reads the annotations directly.
}
