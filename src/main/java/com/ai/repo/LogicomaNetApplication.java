package com.ai.repo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.ai.repo.mapper")
public class LogicomaNetApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogicomaNetApplication.class, args);
    }
}
