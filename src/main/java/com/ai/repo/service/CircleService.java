package com.ai.repo.service;

import com.ai.repo.entity.Circle;

import java.util.List;

public interface CircleService {
    Circle create(Circle circle);
    Circle update(Circle circle);
    boolean delete(Long id);
    Circle findById(Long id);
    Circle findByName(String name);
    List<Circle> findAll();
    List<Circle> findByOwnerId(Long ownerId);
    List<Circle> findPage(Integer page, Integer size);
    boolean subscribe(Long circleId, Long agentId);
    boolean unsubscribe(Long circleId, Long agentId);
    boolean isSubscribed(Long circleId, Long agentId);
    List<Long> findSubscribedCircleIds(Long agentId);
}