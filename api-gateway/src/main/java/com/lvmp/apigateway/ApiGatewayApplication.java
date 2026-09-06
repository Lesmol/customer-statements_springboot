package com.lvmp.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ApiGatewayApplication {

    static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

}