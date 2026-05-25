package com.ai.repo.service.impl;

import com.ai.repo.entity.Post;
import com.ai.repo.entity.Vote;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.PostMapper;
import com.ai.repo.mapper.VoteMapper;
import com.ai.repo.service.AgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private PostMapper postMapper;

    @Mock
    private VoteMapper voteMapper;

    @Mock
    private AgentService agentService;

    private PostServiceImpl postService;

    @BeforeEach
    void setUp() {
        postService = new PostServiceImpl();
        // Use reflection to inject mocks
        try {
            java.lang.reflect.Field postMapperField = PostServiceImpl.class.getDeclaredField("postMapper");
            postMapperField.setAccessible(true);
            postMapperField.set(postService, postMapper);

            java.lang.reflect.Field voteMapperField = PostServiceImpl.class.getDeclaredField("voteMapper");
            voteMapperField.setAccessible(true);
            voteMapperField.set(postService, voteMapper);

            java.lang.reflect.Field agentServiceField = PostServiceImpl.class.getDeclaredField("agentService");
            agentServiceField.setAccessible(true);
            agentServiceField.set(postService, agentService);
        } catch (Exception e) {
            fail("Failed to inject mocks: " + e.getMessage());
        }
    }

    // ========== create() Tests ==========

    @Test
    void create_shouldInitializePostWithCorrectDefaults() {
        // Given
        Post post = new Post();
        post.setAgentId(1L);
        post.setTitle("Test Post");
        post.setContent("Test Content");

        when(postMapper.insert(any(Post.class))).thenReturn(1);

        // When
        Post result = postService.create(post);

        // Then
        assertNotNull(result, "Created post should not be null");
        assertEquals(0, result.getUpvotes(), "Upvotes should be initialized to 0");
        assertEquals(0, result.getDownvotes(), "Downvotes should be initialized to 0");
        assertEquals(0, result.getCommentCount(), "CommentCount should be initialized to 0");
        assertEquals(0, result.getViewCount(), "ViewCount should be initialized to 0");
        assertEquals("verified", result.getVerificationStatus(), "VerificationStatus should be 'verified'");
        assertFalse(result.getIsPinned(), "IsPinned should be false");
        assertFalse(result.getIsLocked(), "IsLocked should be false");
        verify(postMapper).insert(any(Post.class));
        verify(agentService).incrementPostsCount(1L, 1);
    }

    @Test
    void create_shouldCallIncrementPostsCount() {
        // Given
        Post post = new Post();
        post.setAgentId(1L);
        post.setTitle("Test Post");

        when(postMapper.insert(any(Post.class))).thenReturn(1);

        // When
        postService.create(post);

        // Then
        verify(agentService).incrementPostsCount(1L, 1);
    }

    // ========== update() Tests ==========

    @Test
    void update_shouldSucceed_whenPostExists() {
        // Given
        Post existingPost = new Post();
        existingPost.setId(1L);
        existingPost.setTitle("Original Title");

        Post updatedPost = new Post();
        updatedPost.setId(1L);
        updatedPost.setTitle("Updated Title");

        when(postMapper.selectById(1L)).thenReturn(existingPost);
        when(postMapper.update(any(Post.class))).thenReturn(1);

        // When
        Post result = postService.update(updatedPost);

        // Then
        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        verify(postMapper).update(any(Post.class));
    }

    @Test
    void update_shouldThrowException_whenPostNotFound() {
        // Given
        Post post = new Post();
        post.setId(999L);

        when(postMapper.selectById(999L)).thenReturn(null);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            postService.update(post);
        });
        assertTrue(exception.getMessage().contains("Post not found"), "Should throw Post not found exception");
        verify(postMapper, never()).update(any(Post.class));
    }

    // ========== delete() Tests ==========

    @Test
    void delete_shouldSucceed_whenPostExists() {
        // Given
        Long postId = 1L;
        Post existingPost = new Post();
        existingPost.setId(postId);

        when(postMapper.selectById(postId)).thenReturn(existingPost);
        when(postMapper.deleteById(postId)).thenReturn(1);

        // When
        boolean result = postService.delete(postId);

        // Then
        assertTrue(result, "Delete should return true");
        verify(postMapper).deleteById(postId);
    }

    @Test
    void delete_shouldThrowException_whenPostNotFound() {
        // Given
        Long postId = 999L;

        when(postMapper.selectById(postId)).thenReturn(null);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            postService.delete(postId);
        });
        assertTrue(exception.getMessage().contains("Post not found"), "Should throw Post not found exception");
        verify(postMapper, never()).deleteById(anyLong());
    }

    // ========== findById() Tests ==========

    @Test
    void findById_shouldReturnPost_whenExists() {
        // Given
        Long postId = 1L;
        Post expectedPost = new Post();
        expectedPost.setId(postId);
        expectedPost.setTitle("Test Post");

        when(postMapper.selectById(postId)).thenReturn(expectedPost);

        // When
        Post result = postService.findById(postId);

        // Then
        assertNotNull(result);
        assertEquals(postId, result.getId());
        assertEquals("Test Post", result.getTitle());
        verify(postMapper).selectById(postId);
    }

    @Test
    void findById_shouldReturnNull_whenNotExists() {
        // Given
        Long postId = 999L;
        when(postMapper.selectById(postId)).thenReturn(null);

        // When
        Post result = postService.findById(postId);

        // Then
        assertNull(result);
        verify(postMapper).selectById(postId);
    }

    // ========== findAll() Tests ==========

    @Test
    void findAll_shouldReturnAllPosts() {
        // Given
        Post post1 = new Post();
        post1.setId(1L);
        Post post2 = new Post();
        post2.setId(2L);
        List<Post> expectedPosts = Arrays.asList(post1, post2);

        when(postMapper.selectAll()).thenReturn(expectedPosts);

        // When
        List<Post> result = postService.findAll();

        // Then
        assertEquals(2, result.size());
        verify(postMapper).selectAll();
    }

    @Test
    void findAll_shouldReturnEmptyList_whenNoPosts() {
        // Given
        when(postMapper.selectAll()).thenReturn(Collections.emptyList());

        // When
        List<Post> result = postService.findAll();

        // Then
        assertTrue(result.isEmpty());
        verify(postMapper).selectAll();
    }

    // ========== findByAgentId() Tests ==========

    @Test
    void findByAgentId_shouldReturnPostsForAgent() {
        // Given
        Long agentId = 1L;
        Post post = new Post();
        post.setId(1L);
        post.setAgentId(agentId);
        List<Post> expectedPosts = Collections.singletonList(post);

        when(postMapper.selectByAgentId(agentId)).thenReturn(expectedPosts);

        // When
        List<Post> result = postService.findByAgentId(agentId);

        // Then
        assertEquals(1, result.size());
        assertEquals(agentId, result.get(0).getAgentId());
        verify(postMapper).selectByAgentId(agentId);
    }

    // ========== findByCircleId() Tests ==========

    @Test
    void findByCircleId_shouldReturnPostsForCircle() {
        // Given
        Long circleId = 1L;
        Post post = new Post();
        post.setId(1L);
        post.setCircleId(circleId);
        List<Post> expectedPosts = Collections.singletonList(post);

        when(postMapper.selectByCircleId(circleId)).thenReturn(expectedPosts);

        // When
        List<Post> result = postService.findByCircleId(circleId);

        // Then
        assertEquals(1, result.size());
        assertEquals(circleId, result.get(0).getCircleId());
        verify(postMapper).selectByCircleId(circleId);
    }

    // ========== findFeed() Tests ==========

    @Test
    void findFeed_shouldUseDefaultLimit_whenLimitIsNull() {
        // Given
        when(postMapper.selectPage(isNull(), any(), eq("hot"), eq(25), eq(0)))
            .thenReturn(Collections.emptyList());

        // When
        List<Post> result = postService.findFeed(null, null, null, null);

        // Then
        assertNotNull(result);
        verify(postMapper).selectPage(isNull(), any(), eq("hot"), eq(25), eq(0));
    }

    @Test
    void findFeed_shouldEnforceMaxLimitOf100() {
        // Given
        when(postMapper.selectPage(isNull(), any(), eq("hot"), eq(100), eq(0)))
            .thenReturn(Collections.emptyList());

        // When
        List<Post> result = postService.findFeed(null, "hot", 200, null);

        // Then
        assertNotNull(result);
        verify(postMapper).selectPage(isNull(), any(), eq("hot"), eq(100), eq(0));
    }

    @Test
    void findFeed_shouldUseProvidedLimit_whenBelowMax() {
        // Given
        when(postMapper.selectPage(isNull(), any(), eq("new"), eq(10), eq(0)))
            .thenReturn(Collections.emptyList());

        // When
        List<Post> result = postService.findFeed(null, "new", 10, null);

        // Then
        assertNotNull(result);
        verify(postMapper).selectPage(isNull(), any(), eq("new"), eq(10), eq(0));
    }

    @Test
    void findFeed_shouldUseDefaultSort_whenSortIsNull() {
        // Given
        when(postMapper.selectPage(isNull(), any(), eq("hot"), anyInt(), eq(0)))
            .thenReturn(Collections.emptyList());

        // When
        List<Post> result = postService.findFeed(null, null, 10, null);

        // Then
        assertNotNull(result);
        verify(postMapper).selectPage(isNull(), any(), eq("hot"), eq(10), eq(0));
    }

    @Test
    void findFeed_shouldUseProvidedSort() {
        // Given
        when(postMapper.selectPage(isNull(), any(), eq("top"), anyInt(), eq(0)))
            .thenReturn(Collections.emptyList());

        // When
        List<Post> result = postService.findFeed(null, "top", 10, null);

        // Then
        assertNotNull(result);
        verify(postMapper).selectPage(isNull(), any(), eq("top"), eq(10), eq(0));
    }

    // ========== upvote() Tests ==========

    @Test
    void upvote_shouldCreateNewUpvote_whenNoExistingVote() {
        // Given
        Long postId = 1L;
        Long agentId = 1L;
        Post post = new Post();
        post.setId(postId);
        post.setAgentId(2L);
        post.setUpvotes(0);

        when(postMapper.selectById(postId)).thenReturn(post);
        when(voteMapper.selectByTarget(agentId, postId, "post")).thenReturn(null);
        when(voteMapper.insert(any(Vote.class))).thenReturn(1);
        when(postMapper.updateUpvotes(postId, 1)).thenReturn(1);

        // When
        boolean result = postService.upvote(postId, agentId);

        // Then
        assertTrue(result, "Upvote should return true");
        verify(voteMapper).insert(any(Vote.class));
        verify(postMapper).updateUpvotes(postId, 1);
        verify(agentService).updateKarma(2L, 1);
    }

    @Test
    void upvote_shouldReturnFalse_whenAlreadyUpvoted() {
        // Given
        Long postId = 1L;
        Long agentId = 1L;
        Post post = new Post();
        post.setId(postId);

        Vote existingUpvote = new Vote();
        existingUpvote.setId(1L);
        existingUpvote.setVoteType("up");

        when(postMapper.selectById(postId)).thenReturn(post);
        when(voteMapper.selectByTarget(agentId, postId, "post")).thenReturn(existingUpvote);

        // When
        boolean result = postService.upvote(postId, agentId);

        // Then
        assertFalse(result, "Should return false when already upvoted");
        verify(voteMapper, never()).insert(any(Vote.class));
        verify(postMapper, never()).updateUpvotes(anyLong(), anyInt());
        verify(agentService, never()).updateKarma(anyLong(), anyInt());
    }

    @Test
    void upvote_shouldRemoveDownvote_thenAddUpvote() {
        // Given
        Long postId = 1L;
        Long agentId = 1L;
        Post post = new Post();
        post.setId(postId);
        post.setAgentId(2L);

        Vote existingDownvote = new Vote();
        existingDownvote.setId(1L);
        existingDownvote.setVoteType("down");

        when(postMapper.selectById(postId)).thenReturn(post);
        when(voteMapper.selectByTarget(agentId, postId, "post")).thenReturn(existingDownvote);
        when(voteMapper.deleteById(1L)).thenReturn(1);
        when(postMapper.updateDownvotes(postId, -1)).thenReturn(1);
        when(voteMapper.insert(any(Vote.class))).thenReturn(1);
        when(postMapper.updateUpvotes(postId, 1)).thenReturn(1);

        // When
        boolean result = postService.upvote(postId, agentId);

        // Then
        assertTrue(result, "Should return true when switching from downvote to upvote");
        verify(voteMapper).deleteById(1L);
        verify(postMapper).updateDownvotes(postId, -1);
        verify(postMapper).updateUpvotes(postId, 1);
    }

    @Test
    void upvote_shouldThrowException_whenPostNotFound() {
        // Given
        Long postId = 999L;
        Long agentId = 1L;

        when(postMapper.selectById(postId)).thenReturn(null);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            postService.upvote(postId, agentId);
        });
        assertTrue(exception.getMessage().contains("Post not found"));
        verify(voteMapper, never()).selectByTarget(anyLong(), anyLong(), anyString());
    }

    // ========== downvote() Tests ==========

    @Test
    void downvote_shouldCreateNewDownvote_whenNoExistingVote() {
        // Given
        Long postId = 1L;
        Long agentId = 1L;
        Post post = new Post();
        post.setId(postId);

        when(postMapper.selectById(postId)).thenReturn(post);
        when(voteMapper.selectByTarget(agentId, postId, "post")).thenReturn(null);
        when(voteMapper.insert(any(Vote.class))).thenReturn(1);
        when(postMapper.updateDownvotes(postId, 1)).thenReturn(1);

        // When
        boolean result = postService.downvote(postId, agentId);

        // Then
        assertTrue(result, "Downvote should return true");
        verify(voteMapper).insert(any(Vote.class));
        verify(postMapper).updateDownvotes(postId, 1);
    }

    @Test
    void downvote_shouldReturnFalse_whenAlreadyDownvoted() {
        // Given
        Long postId = 1L;
        Long agentId = 1L;
        Post post = new Post();
        post.setId(postId);

        Vote existingDownvote = new Vote();
        existingDownvote.setId(1L);
        existingDownvote.setVoteType("down");

        when(postMapper.selectById(postId)).thenReturn(post);
        when(voteMapper.selectByTarget(agentId, postId, "post")).thenReturn(existingDownvote);

        // When
        boolean result = postService.downvote(postId, agentId);

        // Then
        assertFalse(result, "Should return false when already downvoted");
        verify(voteMapper, never()).insert(any(Vote.class));
        verify(postMapper, never()).updateDownvotes(anyLong(), anyInt());
    }

    @Test
    void downvote_shouldRemoveUpvote_thenAddDownvote() {
        // Given
        Long postId = 1L;
        Long agentId = 1L;
        Post post = new Post();
        post.setId(postId);
        post.setUpvotes(1);

        Vote existingUpvote = new Vote();
        existingUpvote.setId(1L);
        existingUpvote.setVoteType("up");

        when(postMapper.selectById(postId)).thenReturn(post);
        when(voteMapper.selectByTarget(agentId, postId, "post")).thenReturn(existingUpvote);
        when(voteMapper.deleteById(1L)).thenReturn(1);
        when(postMapper.updateUpvotes(postId, -1)).thenReturn(1);
        when(voteMapper.insert(any(Vote.class))).thenReturn(1);
        when(postMapper.updateDownvotes(postId, 1)).thenReturn(1);

        // When
        boolean result = postService.downvote(postId, agentId);

        // Then
        assertTrue(result, "Should return true when switching from upvote to downvote");
        verify(voteMapper).deleteById(1L);
        verify(postMapper).updateUpvotes(postId, -1);
        verify(postMapper).updateDownvotes(postId, 1);
    }

    @Test
    void downvote_shouldThrowException_whenPostNotFound() {
        // Given
        Long postId = 999L;
        Long agentId = 1L;

        when(postMapper.selectById(postId)).thenReturn(null);

        // When/Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            postService.downvote(postId, agentId);
        });
        assertTrue(exception.getMessage().contains("Post not found"));
        verify(voteMapper, never()).selectByTarget(anyLong(), anyLong(), anyString());
    }

    // ========== incrementViewCount() Tests ==========

    @Test
    void incrementViewCount_shouldReturnTrue_whenSuccessful() {
        // Given
        Long postId = 1L;
        when(postMapper.updateViewCount(postId, 1)).thenReturn(1);

        // When
        boolean result = postService.incrementViewCount(postId);

        // Then
        assertTrue(result, "Should return true when view count updated");
        verify(postMapper).updateViewCount(postId, 1);
    }

    @Test
    void incrementViewCount_shouldReturnFalse_whenPostNotExists() {
        // Given
        Long postId = 999L;
        when(postMapper.updateViewCount(postId, 1)).thenReturn(0);

        // When
        boolean result = postService.incrementViewCount(postId);

        // Then
        assertFalse(result, "Should return false when post not found");
        verify(postMapper).updateViewCount(postId, 1);
    }
}