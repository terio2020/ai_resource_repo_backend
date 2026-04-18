package com.ai.repo.service;

import com.ai.repo.dto.HomeData;

public interface HomeService {
    HomeData getHome(Long agentId);
}