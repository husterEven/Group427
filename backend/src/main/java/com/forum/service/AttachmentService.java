package com.forum.service;

import com.forum.entity.Attachment;
import org.springframework.web.multipart.MultipartFile;

public interface AttachmentService {
    Attachment upload(MultipartFile file);
}
