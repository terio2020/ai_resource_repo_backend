package com.ai.repo.service.impl;

import com.ai.repo.entity.ShareLink;
import com.ai.repo.entity.Skill;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.mapper.ShareLinkMapper;
import com.ai.repo.mapper.SkillMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShareServiceImplTest {

    @Mock
    private ShareLinkMapper shareLinkMapper;

    @Mock
    private SkillMapper skillMapper;

    private ShareServiceImpl shareService;

    @BeforeEach
    void setUp() {
        shareService = new ShareServiceImpl();
        try {
            java.lang.reflect.Field shareLinkField = ShareServiceImpl.class.getDeclaredField("shareLinkMapper");
            shareLinkField.setAccessible(true);
            shareLinkField.set(shareService, shareLinkMapper);

            java.lang.reflect.Field skillField = ShareServiceImpl.class.getDeclaredField("skillMapper");
            skillField.setAccessible(true);
            skillField.set(shareService, skillMapper);
        } catch (Exception e) {
            fail("Failed to inject mock mappers: " + e.getMessage());
        }
    }

    // ========== createShareLink() Tests ==========

    @Test
    void createShareLink_shouldSucceed_whenSkillIsPublic() {
        // Given
        Long skillId = 1L;
        Long userId = 10L;

        Skill skill = new Skill();
        skill.setId(skillId);
        skill.setIsPublic(true);
        skill.setName("Test Skill");

        when(skillMapper.selectById(skillId)).thenReturn(skill);
        when(shareLinkMapper.insert(any(ShareLink.class))).thenReturn(1);

        // When
        String token = shareService.createShareLink(skillId, userId);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        verify(skillMapper).selectById(skillId);
        verify(shareLinkMapper).insert(argThat(link ->
                link.getSkillId().equals(skillId) &&
                link.getCreatedBy().equals(userId) &&
                link.getViewCount() == 0 &&
                link.getShareToken() != null &&
                !link.getShareToken().isEmpty()
        ));
    }

    @Test
    void createShareLink_shouldThrow_whenSkillNotFound() {
        // Given
        Long skillId = 999L;
        when(skillMapper.selectById(skillId)).thenReturn(null);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> shareService.createShareLink(skillId, 10L));
        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("not found"));
        verify(shareLinkMapper, never()).insert(any());
    }

    @Test
    void createShareLink_shouldThrow_whenSkillIsNotPublic() {
        // Given
        Long skillId = 1L;

        Skill skill = new Skill();
        skill.setId(skillId);
        skill.setIsPublic(false);

        when(skillMapper.selectById(skillId)).thenReturn(skill);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> shareService.createShareLink(skillId, 10L));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("public"));
        verify(shareLinkMapper, never()).insert(any());
    }

    // ========== getSharedSkill() Tests ==========

    @Test
    void getSharedSkill_shouldReturnSkill_whenTokenIsValid() {
        // Given
        String token = "test-uuid-token";
        Long skillId = 1L;
        Long shareLinkId = 100L;

        ShareLink shareLink = new ShareLink();
        shareLink.setId(shareLinkId);
        shareLink.setSkillId(skillId);
        shareLink.setShareToken(token);

        Skill skill = new Skill();
        skill.setId(skillId);
        skill.setName("Test Skill");
        skill.setIsPublic(true);

        when(shareLinkMapper.findByToken(token)).thenReturn(shareLink);
        when(skillMapper.selectById(skillId)).thenReturn(skill);

        // When
        Skill result = shareService.getSharedSkill(token);

        // Then
        assertNotNull(result);
        assertEquals(skillId, result.getId());
        verify(shareLinkMapper).findByToken(token);
        verify(shareLinkMapper).incrementViewCount(shareLinkId);
        verify(skillMapper).selectById(skillId);
    }

    @Test
    void getSharedSkill_shouldThrow_whenTokenNotFound() {
        // Given
        String token = "invalid-token";
        when(shareLinkMapper.findByToken(token)).thenReturn(null);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> shareService.getSharedSkill(token));
        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("not found") || ex.getMessage().contains("expired"));
        verify(shareLinkMapper, never()).incrementViewCount(any());
        verify(skillMapper, never()).selectById(any());
    }

    @Test
    void getSharedSkill_shouldThrow_whenSkillNoLongerAvailable() {
        // Given
        String token = "test-uuid-token";
        Long skillId = 1L;
        Long shareLinkId = 100L;

        ShareLink shareLink = new ShareLink();
        shareLink.setId(shareLinkId);
        shareLink.setSkillId(skillId);
        shareLink.setShareToken(token);

        when(shareLinkMapper.findByToken(token)).thenReturn(shareLink);
        when(skillMapper.selectById(skillId)).thenReturn(null);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> shareService.getSharedSkill(token));
        assertTrue(ex.getMessage().contains("no longer available"));
        verify(shareLinkMapper).findByToken(token);
        verify(shareLinkMapper).incrementViewCount(shareLinkId);
        verify(skillMapper).selectById(skillId);
    }
}
