package com.zg.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zg.ai.mapper")
public class ChatLibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatLibraryApplication.class, args);
    }

}
