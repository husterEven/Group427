package com.forum.controller;

import com.forum.common.GlobalExceptionHandler;
import com.forum.entity.Attachment;
import com.forum.service.AttachmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttachmentController 附件接口 单元测试")
class AttachmentControllerTest {

    @Mock private AttachmentService attachmentService;
    @InjectMocks private AttachmentController attachmentController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(attachmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/attachments/upload — 上传附件")
    void upload() throws Exception {
        Attachment attachment = new Attachment();
        attachment.setAttachmentId(1L);
        attachment.setFileName("test.jpg");
        attachment.setFileUrl("/uploads/test.jpg");
        attachment.setFileSize(1024L);

        when(attachmentService.upload(any())).thenReturn(attachment);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test image content".getBytes());

        mockMvc.perform(multipart("/api/v1/attachments/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("上传成功"))
                .andExpect(jsonPath("$.data.fileName").value("test.jpg"))
                .andExpect(jsonPath("$.data.fileSize").value(1024));
    }

    @Test
    @DisplayName("上传失败应返回错误")
    void uploadError() throws Exception {
        when(attachmentService.upload(any()))
                .thenThrow(new RuntimeException("文件大小超过限制"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", "content".getBytes());

        mockMvc.perform(multipart("/api/v1/attachments/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("文件大小超过限制"));
    }
}
