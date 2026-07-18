package com.greengrid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * GreenGrid - a Git-based developer learning platform.
 * Every commit produced by this application represents genuine work
 * completed by the authenticated user in their own GitHub repository.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class GreenGridApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreenGridApplication.class, args);
    }
}
