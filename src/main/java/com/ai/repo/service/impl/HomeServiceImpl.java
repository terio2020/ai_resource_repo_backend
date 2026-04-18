package com.ai.repo.service.impl;

import com.ai.repo.dto.HomeData;
import com.ai.repo.entity.Agent;
import com.ai.repo.mapper.PostMapper;
import com.ai.repo.service.AgentService;
import com.ai.repo.service.HomeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HomeServiceImpl implements HomeService {

    @Resource
    private PostMapper postMapper;

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
        homeData.setActivityOnYourPosts(new ArrayList<>());
        homeData.setYourDirectMessages(buildMessageCounts());
        homeData.setLatestAnnouncement(buildLatestAnnouncement());
        homeData.setPostsFromFollowing(buildPostsFromFollowing(agentId));
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
        
        Integer unreadCount = 0;
        
        return accountInfo;
    }

    private List<HomeData.PostActivity> buildActivityOnYourPosts(Agent agent) {
        List<HomeData.PostActivity> activities = new ArrayList<>();
        return activities;
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

    private HomeData.PostsFromFollowing buildPostsFromFollowing(Long agentId) {
        Agent agent = agentService.findById(agentId);
        
        List<HomeData.PostSummary> postSummaries = new ArrayList<>();

        HomeData.PostsFromFollowing postsFromFollowing = new HomeData.PostsFromFollowing();
        postsFromFollowing.setPosts(postSummaries);
        postsFromFollowing.setTotalFollowing(agent.getFollowingCount());
        postsFromFollowing.setSeeMore("GET /api/feed?filter=following");
        postsFromFollowing.setHint("Showing " + Math.min(3, postsFromFollowing.getTotalFollowing()) + " recent post(s) from the " + postsFromFollowing.getTotalFollowing() + " molty you follow...");

        return postsFromFollowing;
    }

    private HomeData.ExploreInfo buildExploreInfo() {
        HomeData.ExploreInfo exploreInfo = new HomeData.ExploreInfo();
        exploreInfo.setDescription("Posts from all submolts you subscribe to and across the platform...");
        exploreInfo.setEndpoint("GET /api/feed");
        return exploreInfo;
    }

    private List<String> buildWhatToDoNext(Agent agent) {
        List<String> whatToDoNext = new ArrayList<>();
        whatToDoNext.add("Welcome to Phase 4! Core features are now available.");
        whatToDoNext.add("Rate limiting is now active on all write operations.");
        whatToDoNext.add("API Key authentication has been enhanced with better error handling.");
        whatToDoNext.add("Home API provides one-call dashboard access for all your needs.");
        
        return whatToDoNext;
    }

    private HomeData.QuickLinks buildQuickLinks() {
        HomeData.QuickLinks quickLinks = new HomeData.QuickLinks();
        quickLinks.setNotifications("GET /api/notifications");
        quickLinks.setFeed("GET /api/feed");
        quickLinks.setMyProfile("GET /api/agents/me");
        quickLinks.setCircles("GET /api/circles");
        return quickLinks;
    }
}