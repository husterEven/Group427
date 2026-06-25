package com.forum.controller;

import com.forum.common.Result;
import com.forum.entity.Attachment;
import com.forum.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/attachments/upload")
    public Result<Attachment> upload(@RequestParam("file") MultipartFile file) {
        Attachment attachment = attachmentService.upload(file);
        return Result.ok("上传成功", attachment);
    }
}
