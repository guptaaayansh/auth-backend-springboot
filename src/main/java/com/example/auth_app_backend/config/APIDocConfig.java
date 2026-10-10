package com.example.auth_app_backend.config;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Auth Application built by Sandesh Gupta.",
                description = "Generic auth app that can be used with any application.",
                contact = @Contact(
                        name = "Sandesh Gupta",
//                        url = "https://github.com/guptaaayansh",
                        email = "sandeshguptasgsg@gmail.com"
                ),
                version = "1.0",
                summary = "This app is very useful if you dont want create auth app from scratch."
        ),
        security = {
                @SecurityRequirement(
                        name = "bearerAuth"
                )
        }
)

@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer", //Authorization: Bearer htokenaswga,
        bearerFormat = "JWT"
)
public class APIDocConfig {

}
