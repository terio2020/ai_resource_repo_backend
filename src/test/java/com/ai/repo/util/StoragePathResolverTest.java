package com.ai.repo.util;

import com.ai.repo.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StoragePathResolverTest {

    // ==================== safeSegment ====================

    @Test
    void safeSegment_shouldReturnTrimmed_forValidName() {
        assertEquals("my-skill", StoragePathResolver.safeSegment("  my-skill  ", "name"));
        assertEquals("skill-1_v2", StoragePathResolver.safeSegment("skill-1_v2", "name"));
    }

    @Test
    void safeSegment_shouldRejectNull() {
        assertThrows(BusinessException.class, () -> StoragePathResolver.safeSegment(null, "name"));
    }

    @Test
    void safeSegment_shouldRejectBlank() {
        assertThrows(BusinessException.class, () -> StoragePathResolver.safeSegment("   ", "name"));
        assertThrows(BusinessException.class, () -> StoragePathResolver.safeSegment("", "name"));
    }

    @Test
    void safeSegment_shouldRejectAbsolute() {
        assertThrows(BusinessException.class, () -> StoragePathResolver.safeSegment("/etc/passwd", "name"));
    }

    @Test
    void safeSegment_shouldRejectParentTraversal() {
        assertThrows(BusinessException.class, () -> StoragePathResolver.safeSegment("..", "name"));
        assertThrows(BusinessException.class, () -> StoragePathResolver.safeSegment("../bar", "name"));
        assertThrows(BusinessException.class, () -> StoragePathResolver.safeSegment("foo/../bar", "name"));
    }

    @Test
    void safeSegment_shouldRejectEmbeddedSeparator() {
        assertThrows(BusinessException.class, () -> StoragePathResolver.safeSegment("foo/bar", "name"));
        assertThrows(BusinessException.class, () -> StoragePathResolver.safeSegment("foo\\bar", "name"));
    }

    @Test
    void safeSegment_shouldRejectNullByte() {
        assertThrows(BusinessException.class, () -> StoragePathResolver.safeSegment("foo\0bar", "name"));
    }

    // ==================== safeRelativePath ====================

    @Test
    void safeRelativePath_shouldReturnPath_forValidRelative() {
        assertEquals("sub/dir/file.md", StoragePathResolver.safeRelativePath("sub/dir/file.md", "path"));
        assertEquals("file.md", StoragePathResolver.safeRelativePath("file.md", "path"));
    }

    @Test
    void safeRelativePath_shouldNormalizeBackslashesAndCollapseSlashes() {
        assertEquals("sub/dir/file.md", StoragePathResolver.safeRelativePath("sub\\dir//file.md", "path"));
    }

    @Test
    void safeRelativePath_shouldRejectNull() {
        assertThrows(BusinessException.class, () -> StoragePathResolver.safeRelativePath(null, "path"));
    }

    @Test
    void safeRelativePath_shouldRejectBlank() {
        assertThrows(BusinessException.class, () -> StoragePathResolver.safeRelativePath("", "path"));
    }

    @Test
    void safeRelativePath_shouldRejectAbsolute() {
        assertThrows(BusinessException.class, () -> StoragePathResolver.safeRelativePath("/etc/passwd.txt", "path"));
    }

    @Test
    void safeRelativePath_shouldRejectParentTraversal() {
        assertThrows(BusinessException.class, () -> StoragePathResolver.safeRelativePath("../etc/passwd", "path"));
        assertThrows(BusinessException.class, () -> StoragePathResolver.safeRelativePath("foo/../../etc/passwd", "path"));
        assertThrows(BusinessException.class, () -> StoragePathResolver.safeRelativePath("foo/../bar", "path"));
    }

    @Test
    void safeRelativePath_shouldRejectNullByte() {
        assertThrows(BusinessException.class, () -> StoragePathResolver.safeRelativePath("foo\0bar", "path"));
    }
}