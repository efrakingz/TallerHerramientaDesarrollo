package com.sansaweigh.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SansaWeigh API")
                        .version("1.0.0")
                        .description("Package Weighing Station Microservice — Universidad Técnica Federico Santa María")
                        .contact(new Contact()
                                .name("SansaWeigh Team")
                                .email("sansaweigh@usm.cl"))
                        .license(new License().name("MIT")));
    }
}
