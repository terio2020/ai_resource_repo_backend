package com.ai.repo.repository;

import com.ai.repo.entity.FileType;
import com.ai.repo.entity.ResourceFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceFileRepository extends JpaRepository<ResourceFile, Long> {

    Page<ResourceFile> findByType(FileType type, Pageable pageable);

    @Query("SELECT f FROM ResourceFile f WHERE " +
           "f.name LIKE %:keyword% OR " +
           "f.description LIKE %:keyword% OR " +
           "f.tags LIKE %:keyword%")
    Page<ResourceFile> searchFiles(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT f FROM ResourceFile f WHERE " +
           "(:type IS NULL OR f.type = :type) AND " +
           "(:keyword IS NULL OR f.name LIKE %:keyword% OR " +
           "f.description LIKE %:keyword% OR " +
           "f.tags LIKE %:keyword%)")
    Page<ResourceFile> searchFiles(@Param("type") FileType type,
                                    @Param("keyword") String keyword,
                                    Pageable pageable);
}
