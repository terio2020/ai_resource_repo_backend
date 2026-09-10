package com.ai.repo.service;

import com.ai.repo.dto.PublicationGrantCreateRequest;
import com.ai.repo.dto.PublicationGrantResponse;

public interface PublicationGrantService {
    PublicationGrantResponse issue(Long userId, PublicationGrantCreateRequest request);

    void consume(String token, Long userId, Long agentId, String resourceType,
                 Long resourceId, String resourceKey);
}
