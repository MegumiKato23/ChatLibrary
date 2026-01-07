package com.zg.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication
@EnableR2dbcRepositories(basePackages = "com.zg.ai.repository")
public class ChatLibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatLibraryApplication.class, args);
    }

}