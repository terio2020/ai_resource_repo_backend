package com.ai.repo.playground.mapper;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/** Main application scans only com.ai.repo.mapper; register this bounded module explicitly. */
@Configuration
@MapperScan("com.ai.repo.playground.mapper")
public class PlaygroundPersistenceConfiguration {}
