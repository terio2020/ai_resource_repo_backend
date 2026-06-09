package com.ai.repo.service.impl;

import com.ai.repo.entity.SkillRepository;
import com.ai.repo.exception.BusinessException;
import com.ai.repo.exception.FileNotAllowedException;
import com.ai.repo.exception.RepositoryNotFoundException;
import com.ai.repo.mapper.SkillRepositoryMapper;
import com.ai.repo.service.SkillRepositoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
public class SkillRepositoryServiceImpl implements SkillRepositoryService {

    private static final long MAX_FILE_SIZE_BYTES = 1_048_576L; // 1 MB

    @Value("${app.git.root-path:/data/git_repos/}")
    private String gitRootPath;

    @Resource
    private SkillRepositoryMapper skillRepositoryMapper;

    @Override
    public SkillRepository findById(Long id) {
        SkillRepository repo = skillRepositoryMapper.selectById(id);
        if (repo == null) {
            throw new RepositoryNotFoundException(id);
        }
        return repo;
    }

    @Override
    public List<SkillRepository> findByAgentId(Long agentId) {
        return skillRepositoryMapper.selectByAgentId(agentId);
    }

    @Override
    public SkillRepository create(SkillRepository skillRepository) {
        if (skillRepository.getIsPublic() == null) {
            skillRepository.setIsPublic(false);
        }
        if (skillRepository.getEnabled() == null) {
            skillRepository.setEnabled(true);
        }
        if (skillRepository.getDownloadCount() == null) {
            skillRepository.setDownloadCount(0);
        }
        if (skillRepository.getLikeCount() == null) {
            skillRepository.setLikeCount(0);
        }
        skillRepository.setCreatedAt(LocalDateTime.now());
        skillRepositoryMapper.insert(skillRepository);
        return skillRepository;
    }

    @Override
    public SkillRepository updateMetadata(SkillRepository repo) {
        SkillRepository existing = findById(repo.getId());
        if (!existing.getAgentId().equals(repo.getAgentId())) {
            throw new BusinessException(403, "Only the owning agent can update metadata");
        }
        skillRepositoryMapper.updateMetadata(repo);
        return skillRepositoryMapper.selectById(repo.getId());
    }

    @Override
    public void incrementDownloadCount(Long repoId) {
        findById(repoId);
        skillRepositoryMapper.incrementDownloadCount(repoId);
    }

    @Override
    public void incrementLikeCount(Long repoId) {
        findById(repoId);
        skillRepositoryMapper.incrementLikeCount(repoId);
    }

    @Override
    public List<SkillRepository> findByCategory(String category) {
        return skillRepositoryMapper.selectByCategory(category);
    }

    @Override
    public List<SkillRepository> findByType(String type) {
        return skillRepositoryMapper.selectByType(type);
    }

    @Override
    public List<SkillRepository> searchByKeyword(String keyword) {
        return skillRepositoryMapper.searchByKeyword(keyword);
    }

    @Override
    public List<SkillRepository> findForksByParentId(Long parentId) {
        return skillRepositoryMapper.selectByParentId(parentId);
    }

    @Override
    @Transactional
    public void setVisibility(Long repoId, Long requestAgentId, boolean isPublic) {
        SkillRepository repo = findById(repoId);
        if (!repo.getAgentId().equals(requestAgentId)) {
            throw new BusinessException(403, "Only the owning agent can change visibility");
        }
        skillRepositoryMapper.updateVisibility(repoId, isPublic);
        log.info("Repository {} visibility set to {} by agent {}", repoId, isPublic, requestAgentId);
    }

    @Override
    public List<SkillRepository> findPublicRepos() {
        return skillRepositoryMapper.selectPublic();
    }

    @Override
    public List<SkillRepository> findPublicReposByAgentId(Long agentId) {
        return skillRepositoryMapper.selectPublicByAgentId(agentId);
    }

    @Override
    public SkillRepository findByRepoPath(String repoPath) {
        return skillRepositoryMapper.selectByRepoPath(repoPath);
    }

    @Override
    public void delete(Long id) {
        SkillRepository repo = findById(id);
        Path repoDir = Paths.get(repo.getRepoPath());
        try {
            if (Files.exists(repoDir)) {
                deleteDirectory(repoDir);
            }
        } catch (IOException e) {
            log.warn("Failed to delete repository directory {}: {}", repoDir, e.getMessage());
        }
        skillRepositoryMapper.deleteById(id);
    }

    @Override
    @Transactional
    public SkillRepository forkRepository(Long currentAgentId, Long currentUserId, Long sourceRepoId) {
        SkillRepository source = skillRepositoryMapper.selectById(sourceRepoId);
        if (source == null) {
            throw new RepositoryNotFoundException(sourceRepoId);
        }

        String forkedSkillName = source.getSkillName() + "_fork";

        SkillRepository existing = skillRepositoryMapper.selectByAgentIdAndSkillName(currentAgentId, forkedSkillName);
        if (existing != null) {
            throw new BusinessException(409, "You already have a fork named '" + forkedSkillName + "'");
        }

        Path sourceDir = Paths.get(source.getRepoPath());
        Path targetDir = Paths.get(gitRootPath, "agent_" + currentAgentId, forkedSkillName + ".git");

        try {
            Files.createDirectories(targetDir.getParent());
            copyDirectory(sourceDir, targetDir);
        } catch (IOException e) {
            try {
                deleteDirectory(targetDir);
            } catch (IOException cleanupEx) {
                log.warn("Failed to clean up partial fork at {}: {}", targetDir, cleanupEx.getMessage());
            }
            throw new BusinessException(500, "Failed to fork repository: " + e.getMessage());
        }

        SkillRepository forked = new SkillRepository();
        forked.setAgentId(currentAgentId);
        forked.setUserId(currentUserId);
        forked.setSkillName(forkedSkillName);
        forked.setRepoPath(targetDir.toAbsolutePath().toString());
        forked.setParentId(sourceRepoId);
        skillRepositoryMapper.insert(forked);

        log.info("Repository forked: source={} target={} agent={}", sourceRepoId, forked.getId(), currentAgentId);
        return forked;
    }

    @Override
    public List<String> getFileTree(Long repoId) {
        SkillRepository repo = findById(repoId);
        Path repoPath = Paths.get(repo.getRepoPath());

        try (Repository repository = openRepository(repoPath)) {
            ObjectId headId = repository.resolve("HEAD");
            if (headId == null) {
                return Collections.emptyList();
            }

            try (RevWalk revWalk = new RevWalk(repository);
                 TreeWalk treeWalk = new TreeWalk(repository)) {

                RevCommit headCommit = revWalk.parseCommit(headId);
                treeWalk.addTree(headCommit.getTree());
                treeWalk.setRecursive(true);

                List<String> files = new ArrayList<>();
                while (treeWalk.next()) {
                    files.add(treeWalk.getPathString());
                }
                return files;
            }
        } catch (IOException e) {
            log.error("Failed to read file tree for repository {}: {}", repoId, e.getMessage());
            throw new BusinessException(500, "Failed to read repository file tree: " + e.getMessage());
        }
    }

    @Override
    public String getFileContent(Long repoId, String filePath) {
        String safePath = sanitizePath(filePath);

        SkillRepository repo = findById(repoId);
        Path repoDir = Paths.get(repo.getRepoPath());

        try (Repository repository = openRepository(repoDir)) {
            ObjectId headId = repository.resolve("HEAD");
            if (headId == null) {
                throw new RepositoryNotFoundException("No commits in repository " + repoId);
            }

            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit headCommit = revWalk.parseCommit(headId);

                try (TreeWalk treeWalk = TreeWalk.forPath(repository, safePath, headCommit.getTree())) {
                    if (treeWalk == null) {
                        throw new FileNotAllowedException("File not found: " + safePath);
                    }

                    ObjectId blobId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(blobId);

                    long size = loader.getSize();
                    if (size > MAX_FILE_SIZE_BYTES) {
                        return "FILE_TOO_LARGE_FOR_PREVIEW";
                    }

                    byte[] bytes = loader.getBytes();
                    return new String(bytes, StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            log.error("Failed to read file '{}' from repository {}: {}", safePath, repoId, e.getMessage());
            throw new BusinessException(500, "Failed to read file: " + e.getMessage());
        }
    }

    private Repository openRepository(Path repoPath) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder.setGitDir(repoPath.toFile())
                .setMustExist(true)
                .build();
    }

    static String sanitizePath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new BusinessException(400, "File path must not be empty");
        }

        String normalized = filePath.replace('\\', '/').trim();

        if (normalized.startsWith("/")) {
            throw new BusinessException(400, "File path must be relative");
        }

        if (normalized.contains("..")) {
            throw new BusinessException(400, "File path must not contain '..'");
        }

        if (normalized.contains("\0")) {
            throw new BusinessException(400, "File path contains invalid characters");
        }

        return normalized;
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            List<Path> sources = stream.toList();
            for (Path src : sources) {
                Path dest = target.resolve(source.relativize(src));
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (Stream<Path> walk = Files.walk(dir)) {
                List<Path> paths = walk.sorted((a, b) -> b.compareTo(a)).toList();
                for (Path path : paths) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
