package com.ai.repo.service;

import com.ai.repo.dto.FileResponse;
import com.ai.repo.dto.PageResponse;
import com.ai.repo.entity.FileType;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileService {

    FileResponse uploadFile(MultipartFile file, FileType type, String description, String tags) throws IOException;

    PageResponse<FileResponse> getFileList(FileType type, int page, int size);

    FileResponse getFileDetail(Long id);

    byte[] downloadFile(Long id) throws IOException;

    void deleteFile(Long id) throws IOException;

    PageResponse<FileResponse> searchFiles(FileType type, String keyword, String tags, int page, int size);
}
