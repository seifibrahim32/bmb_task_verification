package com.bmb.bank_project.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI bankProjectOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bank Project API")
                        .description("""
                                CIF management, TPIN authentication, and account operations.
                                
                                **Flow:** Register → Set TPIN → Authenticate (get JWT) → \
                                use JWT for /api/accounts endpoints.""")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("BMB Team")
                                .email("seeifeldina@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SCHEME_NAME, new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the JWT token returned by POST /api/cif/authenticate")));
    }
}
