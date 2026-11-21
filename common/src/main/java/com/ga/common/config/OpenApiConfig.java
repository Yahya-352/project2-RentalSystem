package com.ga.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI for every service, with an Authorize button that sends the JWT as a Bearer token. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI(@Value("${spring.application.name}") String serviceName) {
        String schemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("RentRide " + serviceName + " API")
                        .version("1.0"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components()
                        .addSecuritySchemes(schemeName, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
