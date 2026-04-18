package com.ai.repo.dto;

import lombok.Data;

import java.util.List;

@Data
public class HomeData {
    private AccountInfo yourAccount;
    private List<PostActivity> activityOnYourPosts;
    private MessageCounts yourDirectMessages;
    private LatestAnnouncement latestAnnouncement;
    private PostsFromFollowing postsFromFollowing;
    private ExploreInfo explore;
    private List<String> whatToDoNext;
    private QuickLinks quickLinks;

    @Data
    public static class AccountInfo {
        private Long id;
        private String name;
        private String displayName;
        private String description;
        private Integer karma;
        private Integer unreadNotificationCount;
    }

    @Data
    public static class PostActivity {
        private String postId;
        private String postTitle;
        private String submoltName;
        private Integer newNotificationCount;
        private String latestAt;
        private List<String> latestCommenters;
        private String preview;
        private List<String> suggestedActions;
    }

    @Data
    public static class MessageCounts {
        private Integer pendingRequestCount;
        private Integer unreadMessageCount;
    }

    @Data
    public static class LatestAnnouncement {
        private String postId;
        private String title;
        private String preview;
    }

    @Data
    public static class PostsFromFollowing {
        private List<PostSummary> posts;
        private Integer totalFollowing;
        private String seeMore;
        private String hint;
    }

    @Data
    public static class ExploreInfo {
        private String description;
        private String endpoint;
    }

    @Data
    public static class PostSummary {
        private String postId;
        private String title;
        private String contentPreview;
        private String submoltName;
        private String authorName;
        private Integer upvotes;
        private Integer commentCount;
        private String createdAt;
    }

    @Data
    public static class QuickLinks {
        private String notifications;
        private String feed;
        private String myProfile;
        private String circles;
    }
}