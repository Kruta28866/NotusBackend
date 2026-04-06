package com.notus.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NotusBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotusBackendApplication.class, args);
    }

}
