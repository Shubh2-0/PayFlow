package com.payflow.wallet.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI walletServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PayFlow Wallet Service")
                        .description("Digital wallet and payment processing microservice")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PayFlow Team")));
    }
}
