package com.ai.repo.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

/** Validates the publishable tree of a Skill repository before Git accepts it. */
public final class SkillRepositoryContentValidator {

    public static final int MAX_FILES = 200;
    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    public static final long MAX_TREE_SIZE_BYTES = 20L * 1024 * 1024;
    public static final long MAX_SKILL_FILE_SIZE_BYTES = 1024L * 1024;

    private static final Set<String> FORBIDDEN_FILE_NAMES = Set.of(
            ".env", "credential", "credentials", "heartbeat-state.json", "agent-profile.md",
            "id_rsa", "id_ed25519", "known_hosts");
    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
            ".key", ".pem", ".p12", ".pfx", ".jks", ".keystore");
    private static final Pattern FRONTMATTER_FIELD = Pattern.compile(
            "(?m)^([a-zA-Z][a-zA-Z0-9_-]*):\\s*(?:\"([^\"]*)\"|'([^']*)'|([^#\\r\\n]*))\\s*$");

    private SkillRepositoryContentValidator() {
    }

    public static void validate(Repository repository, ObjectId commitId, String expectedSkillName)
            throws IOException {
        if (commitId == null || ObjectId.zeroId().equals(commitId)) {
            throw new IllegalArgumentException("branch deletion is not allowed");
        }

        byte[] skillContents = null;
        int fileCount = 0;
        long totalSize = 0;

        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(commitId);
            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(commit.getTree());
                treeWalk.setRecursive(true);
                while (treeWalk.next()) {
                    String path = treeWalk.getPathString();
                    FileMode mode = treeWalk.getFileMode(0);
                    if (FileMode.SYMLINK.equals(mode) || FileMode.GITLINK.equals(mode)) {
                        throw new IllegalArgumentException("symbolic links and Git submodules are not allowed: " + path);
                    }
                    if (mode.getObjectType() != Constants.OBJ_BLOB) {
                        continue;
                    }
                    rejectForbiddenPath(path);

                    ObjectLoader loader = repository.open(treeWalk.getObjectId(0), Constants.OBJ_BLOB);
                    long size = loader.getSize();
                    if (size > MAX_FILE_SIZE_BYTES) {
                        throw new IllegalArgumentException("file exceeds 5 MiB limit: " + path);
                    }
                    fileCount++;
                    totalSize += size;
                    if (fileCount > MAX_FILES) {
                        throw new IllegalArgumentException("repository exceeds 200-file limit");
                    }
                    if (totalSize > MAX_TREE_SIZE_BYTES) {
                        throw new IllegalArgumentException("repository tree exceeds 20 MiB limit");
                    }
                    if ("SKILL.md".equals(path)) {
                        if (size > MAX_SKILL_FILE_SIZE_BYTES) {
                            throw new IllegalArgumentException("SKILL.md exceeds 1 MiB limit");
                        }
                        skillContents = loader.getBytes((int) MAX_SKILL_FILE_SIZE_BYTES);
                    }
                }
            }
        }

        if (skillContents == null) {
            throw new IllegalArgumentException("root SKILL.md is required");
        }
        validateSkillFile(skillContents, expectedSkillName);
    }

    private static void rejectForbiddenPath(String path) {
        String normalized = path.replace('\\', '/').toLowerCase(Locale.ROOT);
        for (String segment : normalized.split("/")) {
            if (FORBIDDEN_FILE_NAMES.contains(segment)
                    || segment.startsWith(".env.")
                    || segment.startsWith("secrets.")) {
                throw new IllegalArgumentException("private configuration path is not allowed: " + path);
            }
            for (String extension : FORBIDDEN_EXTENSIONS) {
                if (segment.endsWith(extension)) {
                    throw new IllegalArgumentException("credential file type is not allowed: " + path);
                }
            }
        }
    }

    private static void validateSkillFile(byte[] bytes, String expectedSkillName) {
        String content;
        try {
            content = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("SKILL.md must be valid UTF-8");
        }
        String normalized = content.replace("\r\n", "\n");
        if (!normalized.startsWith("---\n")) {
            throw new IllegalArgumentException("SKILL.md must start with YAML frontmatter");
        }
        int end = normalized.indexOf("\n---\n", 4);
        if (end < 0) {
            throw new IllegalArgumentException("SKILL.md frontmatter is not closed");
        }
        String frontmatter = normalized.substring(4, end);
        String name = frontmatterValue(frontmatter, "name");
        String description = frontmatterValue(frontmatter, "description");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("SKILL.md frontmatter requires name");
        }
        if (!name.equals(expectedSkillName)) {
            throw new IllegalArgumentException("SKILL.md name must match repository skillName");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("SKILL.md frontmatter requires description");
        }
        if (description.length() > 1024) {
            throw new IllegalArgumentException("SKILL.md description exceeds 1024 characters");
        }
    }

    private static String frontmatterValue(String frontmatter, String field) {
        Matcher matcher = FRONTMATTER_FIELD.matcher(frontmatter);
        while (matcher.find()) {
            if (field.equals(matcher.group(1))) {
                for (int group = 2; group <= 4; group++) {
                    if (matcher.group(group) != null) {
                        return matcher.group(group).trim();
                    }
                }
            }
        }
        return null;
    }
}
