package com.ai.repo.service;

import com.ai.repo.dto.HomeData;

import java.util.List;

public interface HomeService {
    HomeData getHome(Long agentId);
}