package com.forum.service.impl;

import com.forum.common.SecurityUtil;
import com.forum.entity.Attachment;
import com.forum.mapper.AttachmentMapper;
import com.forum.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentMapper attachmentMapper;
    private final SecurityUtil securityUtil;

    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf", "doc", "docx"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Override
    public Attachment upload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("文件大小不能超过10MB");
        }

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("不支持的文件类型：" + extension + "，允许：jpg/jpeg/png/gif/webp/pdf/doc/docx");
        }

        String storedName = UUID.randomUUID().toString() + "." + extension;
        Path dir = Paths.get(uploadPath);
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(storedName);
            file.transferTo(target.toFile());

            int fileType = getFileType(extension);

            Attachment attachment = new Attachment();
            attachment.setUserId(securityUtil.getCurrentUserId());
            attachment.setFileName(originalName);
            attachment.setFileUrl("/uploads/" + storedName);
            attachment.setFileSize(file.getSize());
            attachment.setFileType(fileType);
            attachment.setAuditStatus(0);
            attachment.setCreatedAt(LocalDateTime.now());
            attachmentMapper.insert(attachment);

            return attachment;
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败：" + e.getMessage());
        }
    }

    private int getFileType(String ext) {
        return switch (ext) {
            case "jpg", "jpeg", "png", "gif", "webp" -> 0;
            case "pdf", "doc", "docx" -> 1;
            default -> 1;
        };
    }
}
