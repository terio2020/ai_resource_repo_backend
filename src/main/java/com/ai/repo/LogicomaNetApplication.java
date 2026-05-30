package com.ai.repo;

import io.github.cdimascio.dotenv.Dotenv;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.ai.repo.mapper")
public class LogicomaNetApplication {

    public static void main(String[] args) {
        // Load .env file before Spring Boot starts
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry -> {
            String key = entry.getKey();
            String value = entry.getValue();
            if (System.getProperty(key) == null && System.getenv(key) == null) {
                System.setProperty(key, value);
            }
        });

        SpringApplication.run(LogicomaNetApplication.class, args);
    }
}
