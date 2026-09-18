package com.ai.repo.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillRepositoryContentValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void validate_shouldAcceptCanonicalSkillRepository() throws Exception {
        try (CommittedRepository committed = commit(Map.of(
                "SKILL.md", "---\nname: test-skill\ndescription: \"Use for tests\"\n---\n\n# Test\n",
                "README.md", "# Test Skill\n",
                "references/usage.md", "# Usage\n"))) {
            assertDoesNotThrow(() -> SkillRepositoryContentValidator.validate(
                    committed.repository(), committed.commitId(), "test-skill"));
        }
    }

    @Test
    void validate_shouldRejectMissingRootSkillFile() throws Exception {
        try (CommittedRepository committed = commit(Map.of("README.md", "# Missing entrypoint\n"))) {
            assertThrows(IllegalArgumentException.class, () ->
                    SkillRepositoryContentValidator.validate(
                            committed.repository(), committed.commitId(), "test-skill"));
        }
    }

    @Test
    void validate_shouldRejectMismatchedFrontmatterName() throws Exception {
        try (CommittedRepository committed = commit(Map.of(
                "SKILL.md", "---\nname: another-skill\ndescription: Test\n---\n"))) {
            assertThrows(IllegalArgumentException.class, () ->
                    SkillRepositoryContentValidator.validate(
                            committed.repository(), committed.commitId(), "test-skill"));
        }
    }

    @Test
    void validate_shouldRejectPrivateConfigurationPaths() throws Exception {
        try (CommittedRepository committed = commit(Map.of(
                "SKILL.md", "---\nname: test-skill\ndescription: Test\n---\n",
                ".env", "TOKEN=secret\n"))) {
            assertThrows(IllegalArgumentException.class, () ->
                    SkillRepositoryContentValidator.validate(
                            committed.repository(), committed.commitId(), "test-skill"));
        }
    }

    private CommittedRepository commit(Map<String, String> files) throws Exception {
        Path workTree = Files.createTempDirectory(tempDir, "skill-");
        Repository repository = new FileRepositoryBuilder()
                .setGitDir(workTree.resolve(".git").toFile())
                .setWorkTree(workTree.toFile())
                .build();
        repository.create();
        Git git = new Git(repository);
        for (Map.Entry<String, String> entry : files.entrySet()) {
            Path file = workTree.resolve(entry.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.getValue());
        }
        git.add().addFilepattern(".").call();
        ObjectId commitId = git.commit()
                .setAuthor("test", "test@example.com")
                .setCommitter("test", "test@example.com")
                .setMessage("test")
                .call()
                .getId();
        return new CommittedRepository(git, git.getRepository(), commitId);
    }

    private record CommittedRepository(Git git, Repository repository, ObjectId commitId)
            implements AutoCloseable {
        @Override
        public void close() {
            git.close();
        }
    }
}
