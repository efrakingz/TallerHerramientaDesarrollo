package com.sansaweigh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class SansaWeighApplication {
    public static void main(String[] args) {
        SpringApplication.run(SansaWeighApplication.class, args);
    }
}
