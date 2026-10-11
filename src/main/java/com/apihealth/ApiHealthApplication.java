package com.apihealth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApiHealthApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiHealthApplication.class, args);
    }
}
