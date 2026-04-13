package com.ai.repo.service.impl;

import com.ai.repo.entity.Circle;
import com.ai.repo.entity.CircleSubscription;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.CircleMapper;
import com.ai.repo.mapper.CircleSubscriptionMapper;
import com.ai.repo.service.CircleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CircleServiceImpl implements CircleService {

    @Autowired
    private CircleMapper circleMapper;

    @Autowired
    private CircleSubscriptionMapper subscriptionMapper;

    @Override
    @Transactional
    public Circle create(Circle circle) {
        if (circleMapper.selectByName(circle.getName()) != null) {
            throw new BusinessException("Circle name already exists");
        }
        circle.setSubscriberCount(0);
        circle.setPostCount(0);
        circleMapper.insert(circle);
        return circle;
    }

    @Override
    @Transactional
    public Circle update(Circle circle) {
        if (circleMapper.selectById(circle.getId()) == null) {
            throw new BusinessException("Circle not found");
        }
        circleMapper.update(circle);
        return circle;
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        if (circleMapper.selectById(id) == null) {
            throw new BusinessException("Circle not found");
        }
        return circleMapper.deleteById(id) > 0;
    }

    @Override
    public Circle findById(Long id) {
        return circleMapper.selectById(id);
    }

    @Override
    public Circle findByName(String name) {
        return circleMapper.selectByName(name);
    }

    @Override
    public List<Circle> findAll() {
        return circleMapper.selectAll();
    }

    @Override
    public List<Circle> findByOwnerId(Long ownerId) {
        return circleMapper.selectByOwnerId(ownerId);
    }

    @Override
    public List<Circle> findPage(Integer page, Integer size) {
        int actualPage = (page != null && page > 0) ? page : 1;
        int actualSize = (size != null && size > 0) ? size : 10;
        int offset = (actualPage - 1) * actualSize;
        return circleMapper.selectPage(actualSize, offset);
    }

    @Override
    @Transactional
    public boolean subscribe(Long circleId, Long agentId) {
        if (circleMapper.selectById(circleId) == null) {
            throw new BusinessException("Circle not found");
        }
        
        if (isSubscribed(circleId, agentId)) {
            return false;
        }
        
        CircleSubscription subscription = new CircleSubscription();
        subscription.setAgentId(agentId);
        subscription.setCircleId(circleId);
        subscriptionMapper.insert(subscription);
        
        circleMapper.updateSubscriberCount(circleId, 1);
        return true;
    }

    @Override
    @Transactional
    public boolean unsubscribe(Long circleId, Long agentId) {
        if (circleMapper.selectById(circleId) == null) {
            throw new BusinessException("Circle not found");
        }
        
        if (!isSubscribed(circleId, agentId)) {
            return false;
        }
        
        subscriptionMapper.deleteByAgentCircle(agentId, circleId);
        circleMapper.updateSubscriberCount(circleId, -1);
        return true;
    }

    @Override
    public boolean isSubscribed(Long circleId, Long agentId) {
        return subscriptionMapper.selectByAgentCircle(agentId, circleId) != null;
    }

    @Override
    public List<Long> findSubscribedCircleIds(Long agentId) {
        return subscriptionMapper.selectByAgentId(agentId).stream()
            .map(CircleSubscription::getCircleId)
            .collect(Collectors.toList());
    }
}