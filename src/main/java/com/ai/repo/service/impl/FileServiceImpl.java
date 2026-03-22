package com.ai.repo.service.impl;

import com.ai.repo.dto.FileResponse;
import com.ai.repo.dto.PageResponse;
import com.ai.repo.entity.FileType;
import com.ai.repo.entity.ResourceFile;
import com.ai.repo.repository.ResourceFileRepository;
import com.ai.repo.service.FileService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileServiceImpl implements FileService {

    @Value("${file.storage.path:/data/files}")
    private String storagePath;

    private final ResourceFileRepository repository;

    public FileServiceImpl(ResourceFileRepository repository) {
        this.repository = repository;
    }

    @Override
    public FileResponse uploadFile(MultipartFile file, FileType type, String description, String tags) throws IOException {
        String fileName = generateFileName(type, file.getOriginalFilename());
        Path path = Paths.get(storagePath, fileName);

        if (!Files.exists(Paths.get(storagePath))) {
            Files.createDirectories(Paths.get(storagePath));
        }

        Files.copy(file.getInputStream(), path);

        String contentPreview = new String(file.getBytes());
        if (contentPreview.length() > 1000) {
            contentPreview = contentPreview.substring(0, 1000);
        }

        ResourceFile entity = new ResourceFile();
        entity.setName(file.getOriginalFilename());
        entity.setType(type);
        entity.setFilePath(path.toString());
        entity.setFileSize(file.getSize());
        entity.setContent(contentPreview);
        entity.setDescription(description);
        entity.setTags(tags);
        entity.setMimeType(file.getContentType());

        ResourceFile saved = repository.save(entity);
        return convertToResponse(saved);
    }

    @Override
    public PageResponse<FileResponse> getFileList(FileType type, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ResourceFile> resourceFilePage;

        if (type != null) {
            resourceFilePage = repository.findByType(type, pageable);
        } else {
            resourceFilePage = repository.findAll(pageable);
        }

        return convertToPageResponse(resourceFilePage, page, size);
    }

    @Override
    public FileResponse getFileDetail(Long id) {
        ResourceFile resourceFile = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("文件不存在: " + id));
        return convertToResponse(resourceFile);
    }

    @Override
    public byte[] downloadFile(Long id) throws IOException {
        ResourceFile resourceFile = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("文件不存在: " + id));
        Path path = Paths.get(resourceFile.getFilePath());
        return Files.readAllBytes(path);
    }

    @Override
    public void deleteFile(Long id) throws IOException {
        ResourceFile resourceFile = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("文件不存在: " + id));

        Path path = Paths.get(resourceFile.getFilePath());
        if (Files.exists(path)) {
            Files.delete(path);
        }

        repository.deleteById(id);
    }

    @Override
    public PageResponse<FileResponse> searchFiles(FileType type, String keyword, String tags, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ResourceFile> resourceFilePage = repository.searchFiles(type, keyword, pageable);

        if (tags != null && !tags.isEmpty()) {
            List<ResourceFile> filteredByTags = resourceFilePage.getContent().stream()
                    .filter(f -> f.getTags() != null && containsTag(f.getTags(), tags))
                    .collect(Collectors.toList());
            return createPageResponse(filteredByTags, page, size);
        }

        return convertToPageResponse(resourceFilePage, page, size);
    }

    private String generateFileName(FileType type, String originalFilename) {
        String extension = FilenameUtils.getExtension(originalFilename);
        long timestamp = System.currentTimeMillis();
        return type.name().toLowerCase() + "_" + timestamp + "." + extension;
    }

    private boolean containsTag(String fileTags, String searchTags) {
        String[] fileTagArray = fileTags.split(",");
        String[] searchTagArray = searchTags.split(",");

        for (String searchTag : searchTagArray) {
            boolean found = false;
            for (String fileTag : fileTagArray) {
                if (fileTag.trim().equalsIgnoreCase(searchTag.trim())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private FileResponse convertToResponse(ResourceFile entity) {
        FileResponse response = new FileResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setType(entity.getType());
        response.setFilePath(entity.getFilePath());
        response.setFileSize(entity.getFileSize());
        response.setContent(entity.getContent());
        response.setDescription(entity.getDescription());
        response.setTags(entity.getTags());
        response.setMimeType(entity.getMimeType());
        response.setCreatedAt(entity.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME));
        response.setUpdatedAt(entity.getUpdatedAt().format(DateTimeFormatter.ISO_DATE_TIME));
        return response;
    }

    private PageResponse<FileResponse> convertToPageResponse(Page<ResourceFile> page, int pageNum, int pageSize) {
        PageResponse<FileResponse> response = new PageResponse<>();
        List<FileResponse> content = page.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        response.setContent(content);
        response.setPage(pageNum);
        response.setSize(pageSize);
        response.setTotal(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }

    private PageResponse<FileResponse> createPageResponse(List<ResourceFile> content, int page, int size) {
        PageResponse<FileResponse> response = new PageResponse<>();
        List<FileResponse> fileResponses = content.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        response.setContent(fileResponses);
        response.setPage(page);
        response.setSize(size);
        response.setTotal(content.size());
        response.setTotalPages((int) Math.ceil((double) content.size() / size));
        return response;
    }
}
