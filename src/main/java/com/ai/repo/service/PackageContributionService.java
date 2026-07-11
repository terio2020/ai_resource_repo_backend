package com.ai.repo.service;

import com.ai.repo.dto.ContributionResponse;
import com.ai.repo.dto.ContributionReviewRequest;
import com.ai.repo.entity.PackageContribution;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PackageContributionService {
    PackageContribution findByUid(String uid);

    ContributionResponse submit(Long packageId, Long userId, Long agentId,
                                 Long sourceVersionId, String commitMessage, List<MultipartFile> files);
    ContributionResponse review(Long packageId, Long contributionId, Long reviewerId, ContributionReviewRequest request);
    ContributionResponse getById(Long contributionId);
    List<ContributionResponse> listByPackage(Long packageId);
}
