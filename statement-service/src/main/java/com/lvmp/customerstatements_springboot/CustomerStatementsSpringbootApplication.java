package com.lvmp.customerstatements_springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CustomerStatementsSpringbootApplication {

    static void main(String[] args) {
        SpringApplication.run(CustomerStatementsSpringbootApplication.class, args);
    }

}
