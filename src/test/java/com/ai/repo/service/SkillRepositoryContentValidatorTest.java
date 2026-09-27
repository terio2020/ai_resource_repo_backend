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

    @Test
    void validate_shouldRejectDeletedPrivateFileInHistory() throws Exception {
        try (CommittedRepository committed = commit(Map.of(
                "SKILL.md", "---\nname: test-skill\ndescription: Test\n---\n",
                ".env", "SYNTHETIC_TEST_ONLY=not-a-secret\n"))) {
            committed.git().rm().addFilepattern(".env").call();
            ObjectId cleanHead = committed.git().commit().setAuthor("test", "test@example.com")
                    .setCommitter("test", "test@example.com").setMessage("remove synthetic config").call().getId();
            assertThrows(IllegalArgumentException.class, () ->
                    SkillRepositoryContentValidator.validate(committed.repository(), cleanHead, "test-skill"));
        }
    }

    @Test
    void validate_shouldAllowSafeHistoryBeforeEntrypointWasAdded() throws Exception {
        try (CommittedRepository committed = commit(Map.of("README.md", "Initial draft\n"))) {
            Files.writeString(committed.repository().getWorkTree().toPath().resolve("SKILL.md"),
                    "---\nname: test-skill\ndescription: Test\n---\n");
            committed.git().add().addFilepattern("SKILL.md").call();
            ObjectId head = committed.git().commit().setAuthor("test", "test@example.com")
                    .setCommitter("test", "test@example.com").setMessage("add entrypoint").call().getId();
            assertDoesNotThrow(() -> SkillRepositoryContentValidator.validate(
                    committed.repository(), head, "test-skill"));
        }
    }

    @Test
    void validate_shouldInspectNonFirstMergeParentHistory() throws Exception {
        try (CommittedRepository committed = commit(Map.of(
                "SKILL.md", "---\nname: test-skill\ndescription: Test\n---\n"))) {
            Git git = committed.git();
            String original = committed.repository().getBranch();
            git.checkout().setCreateBranch(true).setName("side-history").call();
            Files.writeString(committed.repository().getWorkTree().toPath().resolve(".env"), "SYNTHETIC=not-a-secret\n");
            git.add().addFilepattern(".env").call();
            git.commit().setAuthor("test", "test@example.com").setCommitter("test", "test@example.com")
                    .setMessage("synthetic private file").call();
            git.rm().addFilepattern(".env").call();
            ObjectId side = git.commit().setAuthor("test", "test@example.com").setCommitter("test", "test@example.com")
                    .setMessage("clean side tip").call().getId();
            git.checkout().setName(original).call();
            var config = committed.repository().getConfig();
            config.setString("user", null, "name", "test");
            config.setString("user", null, "email", "test@example.com");
            config.save();
            ObjectId merged = git.merge().include(side)
                    .setFastForward(org.eclipse.jgit.api.MergeCommand.FastForwardMode.NO_FF)
                    .setStrategy(org.eclipse.jgit.merge.MergeStrategy.OURS).call().getNewHead();
            assertThrows(IllegalArgumentException.class, () -> SkillRepositoryContentValidator.validate(
                    committed.repository(), merged, "test-skill"));
        }
    }

    @Test
    void validate_shouldRejectExcessiveHistoryEvenWithIdenticalTrees() throws Exception {
        try (CommittedRepository committed = commit(Map.of(
                "SKILL.md", "---\nname: test-skill\ndescription: Test\n---\n"))) {
            ObjectId head = committed.commitId();
            for (int i = 0; i < SkillRepositoryContentValidator.MAX_HISTORY_COMMITS; i++) {
                head = committed.git().commit().setAllowEmpty(true).setAuthor("test", "test@example.com")
                        .setCommitter("test", "test@example.com").setMessage("version " + i).call().getId();
            }
            ObjectId finalHead = head;
            assertThrows(IllegalArgumentException.class, () -> SkillRepositoryContentValidator.validate(
                    committed.repository(), finalHead, "test-skill"));
        }
    }

    private record CommittedRepository(Git git, Repository repository, ObjectId commitId)
            implements AutoCloseable {
        @Override
        public void close() {
            git.close();
        }
    }
}
