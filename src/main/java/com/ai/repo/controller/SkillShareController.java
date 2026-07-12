package com.ai.repo.controller;

import com.ai.repo.common.Result;
import com.ai.repo.entity.SkillRepository;
import com.ai.repo.service.SkillRepositoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repo")
@Tag(name = "Repo Lookup API", description = "Resolve skill repositories by UID")
public class SkillShareController {

    @Resource
    private SkillRepositoryService skillRepositoryService;

    @GetMapping("/{uid}")
    @Operation(summary = "Resolve a share link to a skill by UID",
            description = "No auth required. Only returns public skills.")
    public ResponseEntity<Result<SkillRepository>> getByUid(
            @PathVariable String uid) {
        SkillRepository repo = skillRepositoryService.findByUid(uid);
        if (repo == null || !Boolean.TRUE.equals(repo.getIsPublic())) {
            return Result.fail("Skill not found");
        }
        return Result.ok(repo);
    }
}
