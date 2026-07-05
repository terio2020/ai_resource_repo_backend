package com.ai.repo.util;

import com.ai.repo.exception.BusinessException;

/**
 * Sanitizes path components used to build filesystem paths under a known base directory.
 *
 * <p>Two helpers are provided:
 * <ul>
 *   <li>{@link #safeSegment(String, String)} — for single path segments (e.g. package name, version
 *       tag, package type). Rejects null/blank, absolute paths, {@code ..} traversal, embedded
 *       path separators and null bytes.</li>
 *   <li>{@link #safeRelativePath(String, String)} — for multi-segment relative file paths that may
 *       legitimately contain internal slashes (e.g. {@code sub/dir/file.md}). Rejects null/blank,
 *       absolute paths, {@code ..} traversal, null bytes. Collapses consecutive separators.</li>
 * </ul>
 *
 * <p>The motivation: {@code Path.of(base, "/etc/passwd")} resolves to {@code /etc/passwd} because
 * an absolute second argument replaces the first. Likewise {@code ../} segments escape the base.
 * Both cases had to be made impossible before concatenating user input into filesystem paths.
 */
public final class StoragePathResolver {

    private StoragePathResolver() {
    }

    /**
     * Validate a single path segment (no internal slashes allowed).
     *
     * @param segment the user-controlled segment (package name, version tag, package type, …)
     * @param label   a short label used in error messages (e.g. "packageName")
     * @return the sanitized segment
     * @throws BusinessException on any invalid input
     */
    public static String safeSegment(String segment, String label) {
        if (segment == null || segment.isBlank()) {
            throw new BusinessException(400, label + " must not be empty");
        }
        String normalized = segment.replace('\\', '/').trim();
        if (normalized.startsWith("/")) {
            throw new BusinessException(400, label + " must be relative");
        }
        if (normalized.contains("..")) {
            throw new BusinessException(400, label + " must not contain '..'");
        }
        if (normalized.contains("/")) {
            throw new BusinessException(400, label + " must not contain path separators");
        }
        if (normalized.contains("\0")) {
            throw new BusinessException(400, label + " contains invalid characters");
        }
        return normalized;
    }

    /**
     * Validate a relative file path that may contain internal slashes but must not escape the
     * base directory.
     *
     * @param filePath the user-controlled relative path (e.g. {@code sub/dir/file.md})
     * @param label    a short label used in error messages (e.g. "filePath")
     * @return the sanitized relative path with collapsed separators
     * @throws BusinessException on any invalid input
     */
    public static String safeRelativePath(String filePath, String label) {
        if (filePath == null || filePath.isBlank()) {
            throw new BusinessException(400, label + " must not be empty");
        }
        String normalized = filePath.replace('\\', '/').trim();
        if (normalized.startsWith("/")) {
            throw new BusinessException(400, label + " must be relative");
        }
        if (normalized.contains("..")) {
            throw new BusinessException(400, label + " must not contain '..'");
        }
        if (normalized.contains("\0")) {
            throw new BusinessException(400, label + " contains invalid characters");
        }
        return normalized.replaceAll("[/]+", "/");
    }
}