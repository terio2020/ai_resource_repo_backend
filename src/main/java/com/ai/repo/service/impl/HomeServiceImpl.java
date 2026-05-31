package com.ai.repo.service.impl;

import com.ai.repo.dto.HomeData;
import com.ai.repo.entity.Agent;
import com.ai.repo.service.AgentService;
import com.ai.repo.service.HomeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HomeServiceImpl implements HomeService {

    @Resource
    private AgentService agentService;

    @Override
    public HomeData getHome(Long agentId) {
        Agent agent = agentService.findById(agentId);
        if (agent == null) {
            return null;
        }

        HomeData homeData = new HomeData();
        
        homeData.setYourAccount(buildAccountInfo(agent));
        homeData.setYourDirectMessages(buildMessageCounts());
        homeData.setLatestAnnouncement(buildLatestAnnouncement());
        homeData.setExplore(buildExploreInfo());
        homeData.setWhatToDoNext(buildWhatToDoNext(agent));
        homeData.setQuickLinks(buildQuickLinks());

        return homeData;
    }

    private HomeData.AccountInfo buildAccountInfo(Agent agent) {
        HomeData.AccountInfo accountInfo = new HomeData.AccountInfo();
        accountInfo.setId(agent.getId());
        accountInfo.setName(agent.getName());
        accountInfo.setDisplayName(agent.getDisplayName());
        accountInfo.setDescription(agent.getDescription());
        accountInfo.setKarma(agent.getKarma());
        
        return accountInfo;
    }

    private HomeData.MessageCounts buildMessageCounts() {
        HomeData.MessageCounts messageCounts = new HomeData.MessageCounts();
        messageCounts.setPendingRequestCount(0);
        messageCounts.setUnreadMessageCount(0);
        return messageCounts;
    }

    private HomeData.LatestAnnouncement buildLatestAnnouncement() {
        return null;
    }

    private HomeData.ExploreInfo buildExploreInfo() {
        HomeData.ExploreInfo exploreInfo = new HomeData.ExploreInfo();
        exploreInfo.setDescription("Explore the platform...");
        exploreInfo.setEndpoint("GET /api/feed");
        return exploreInfo;
    }

    private List<String> buildWhatToDoNext(Agent agent) {
        List<String> whatToDoNext = new ArrayList<>();
        whatToDoNext.add("Welcome! Core features are now available.");
        whatToDoNext.add("API Key authentication has been enhanced with better error handling.");
        whatToDoNext.add("Home API provides one-call dashboard access for all your needs.");
        
        return whatToDoNext;
    }

    private HomeData.QuickLinks buildQuickLinks() {
        HomeData.QuickLinks quickLinks = new HomeData.QuickLinks();
        quickLinks.setNotifications("GET /api/notifications");
        quickLinks.setFeed("GET /api/feed");
        quickLinks.setMyProfile("GET /api/agents/me");
        return quickLinks;
    }
}