package com.ai.repo.config;

import com.ai.repo.entity.Agent;
import com.ai.repo.entity.SkillRepository;
import com.ai.repo.mapper.SkillRepositoryMapper;
import com.ai.repo.service.AgentService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.UploadPack;
import org.eclipse.jgit.transport.resolver.ReceivePackFactory;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.eclipse.jgit.transport.resolver.UploadPackFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Registers JGit's GitServlet to serve smart-HTTP git operations
 * (clone, fetch, push) at the /api/git/* path.
 *
 * Access control (clone/fetch):
 * - Public repositories: any authenticated agent can clone
 * - Private repositories: only the owning agent can clone
 *
 * Access control (push):
 * - Only the owning agent can push
 */
@Slf4j
@Configuration
public class GitServletConfig {

    @Value("${app.git.root-path:/data/git_repos/}")
    private String gitRootPath;

    @Resource
    private SkillRepositoryMapper skillRepositoryMapper;

    @Resource
    private AgentService agentService;

    @Bean
    public ServletRegistrationBean<GitServlet> gitServlet() {
        GitServlet gitServlet = new GitServlet();

        gitServlet.setRepositoryResolver(new SkillRepositoryResolver(gitRootPath, skillRepositoryMapper));

        gitServlet.setUploadPackFactory(new UploadPackFactory<HttpServletRequest>() {
            @Override
            public UploadPack create(HttpServletRequest req, Repository repo)
                    throws ServiceNotEnabledException, ServiceNotAuthorizedException {
                String repoDirectory = repo.getDirectory().getAbsolutePath();
                SkillRepository skillRepo = skillRepositoryMapper.selectByRepoPath(repoDirectory);

                if (skillRepo == null) {
                    throw new ServiceNotEnabledException("Repository not registered");
                }

                if (Boolean.TRUE.equals(skillRepo.getIsPublic())) {
                    return new UploadPack(repo);
                }

                Long agentId = authenticateAgent(req);
                if (agentId == null || !agentId.equals(skillRepo.getAgentId())) {
                    throw new ServiceNotAuthorizedException();
                }

                return new UploadPack(repo);
            }
        });

        gitServlet.setReceivePackFactory(new ReceivePackFactory<HttpServletRequest>() {
            @Override
            public ReceivePack create(HttpServletRequest req, Repository repo)
                    throws ServiceNotEnabledException, ServiceNotAuthorizedException {
                String repoDirectory = repo.getDirectory().getAbsolutePath();
                SkillRepository skillRepo = skillRepositoryMapper.selectByRepoPath(repoDirectory);

                if (skillRepo == null) {
                    throw new ServiceNotEnabledException("Repository not registered");
                }

                Long agentId = authenticateAgent(req);
                if (agentId == null || !agentId.equals(skillRepo.getAgentId())) {
                    throw new ServiceNotAuthorizedException();
                }

                ReceivePack receivePack = new ReceivePack(repo);
                receivePack.setAllowNonFastForwards(false);
                return receivePack;
            }
        });

        ServletRegistrationBean<GitServlet> registration = new ServletRegistrationBean<>(gitServlet, "/api/git/*");
        registration.setName("gitServlet");
        registration.setLoadOnStartup(1);
        return registration;
    }

    private Long authenticateAgent(HttpServletRequest req) {
        String apiKey = extractApiKey(req);
        if (apiKey == null) {
            return null;
        }

        Agent agent = agentService.findByApiKey(apiKey);
        return agent != null ? agent.getId() : null;
    }

    private String extractApiKey(HttpServletRequest req) {
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return req.getHeader("agent-auth-api-key");
    }

    /**
     * Resolves a repository path from the HTTP request URL to a bare JGit
     * Repository on the local filesystem.
     *
     * URL structure: /api/git/{relative-repo-path}
     * Example: /api/git/agent_101/weather_skill.git resolves to
     * <git-root-path>/agent_101/weather_skill.git
     *
     * If the repository directory does not exist on disk but the database
     * record exists, it auto-initializes a bare Git repository (lazy-init).
     */
    private class SkillRepositoryResolver implements RepositoryResolver<HttpServletRequest> {

        private final String gitRootPath;
        private final SkillRepositoryMapper mapper;

        SkillRepositoryResolver(String gitRootPath, SkillRepositoryMapper mapper) {
            this.gitRootPath = gitRootPath;
            this.mapper = mapper;
        }

        @Override
        public Repository open(HttpServletRequest request, String name)
                throws ServiceNotEnabledException, ServiceNotAuthorizedException {

            Path repoPath = Paths.get(gitRootPath, name).normalize();

            try {
                String canonicalRoot = new File(gitRootPath).getCanonicalPath();
                String canonicalRepo = repoPath.toFile().getCanonicalPath();
                if (!canonicalRepo.startsWith(canonicalRoot + File.separator)
                        && !canonicalRepo.equals(canonicalRoot)) {
                    log.warn("Path traversal attempt detected: {}", name);
                    throw new ServiceNotEnabledException("Invalid repository path");
                }
            } catch (IOException e) {
                log.error("Failed to resolve repository path: {}", e.getMessage());
                throw new ServiceNotEnabledException("Repository not accessible");
            }

            File gitDir = repoPath.toFile();
            if (!gitDir.exists()) {
                String repoPathStr = repoPath.toAbsolutePath().toString();
                SkillRepository dbRepo = mapper.selectByRepoPath(repoPathStr);
                if (dbRepo != null) {
                    try {
                        Files.createDirectories(repoPath.getParent());
                        try (Repository repo = Git.init().setBare(true).setDirectory(gitDir).call().getRepository()) {
                            log.info("Lazy-initialized bare Git repository at {}", repoPath);
                        }
                    } catch (Exception e) {
                        log.error("Failed to lazy-init Git repository at {}: {}", repoPath, e.getMessage());
                        throw new ServiceNotEnabledException("Repository not found");
                    }
                } else {
                    log.warn("Repository not found: {}", repoPath);
                    throw new ServiceNotEnabledException("Repository not found");
                }
            }

            try {
                FileRepositoryBuilder builder = new FileRepositoryBuilder();
                return builder.setGitDir(gitDir)
                        .setMustExist(true)
                        .build();
            } catch (IOException e) {
                log.error("Failed to open repository at {}: {}", repoPath, e.getMessage());
                throw new ServiceNotEnabledException("Cannot open repository");
            }
        }
    }
}