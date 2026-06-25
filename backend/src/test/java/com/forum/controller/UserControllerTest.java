package com.forum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.common.GlobalExceptionHandler;
import com.forum.dto.*;
import com.forum.entity.*;
import com.forum.service.DynamicService;
import com.forum.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController 用户接口 单元测试")
class UserControllerTest {

    @Mock private UserService userService;
    @Mock private DynamicService dynamicService;
    @InjectMocks private UserController userController;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private User createUser() {
        User user = new User();
        user.setUserId(1L);
        user.setNickname("testUser");
        user.setEmail("test@test.com");
        return user;
    }

    @Nested
    @DisplayName("GET /api/v1/users/me")
    class GetMe {
        @Test
        @DisplayName("应返回当前用户信息")
        void shouldReturnCurrentUser() throws Exception {
            when(userService.getMe()).thenReturn(createUser());
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nickname").value("testUser"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me")
    class UpdateMe {
        @Test
        @DisplayName("更新个人信息应返回成功")
        void shouldUpdateProfile() throws Exception {
            when(userService.updateMe(any(UserUpdateRequest.class))).thenReturn(createUser());
            mockMvc.perform(put("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"newName\",\"bio\":\"hello\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("更新成功"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me/password")
    class ChangePassword {
        @Test
        @DisplayName("修改密码应返回成功")
        void shouldChangePassword() throws Exception {
            doNothing().when(userService).changePassword(any(ChangePasswordRequest.class));
            mockMvc.perform(put("/api/v1/users/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"oldPassword\":\"old\",\"newPassword\":\"new123\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("密码修改成功"));
        }
    }

    @Nested
    @DisplayName("GET/PUT /api/v1/users/me/preference")
    class Preference {
        @Test
        @DisplayName("获取偏好")
        void getPreference() throws Exception {
            UserPreference pref = new UserPreference();
            pref.setFocusMarkets("A股");
            when(userService.getPreference()).thenReturn(pref);
            mockMvc.perform(get("/api/v1/users/me/preference"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.focusMarkets").value("A股"));
        }

        @Test
        @DisplayName("更新偏好")
        void updatePreference() throws Exception {
            UserPreference pref = new UserPreference();
            pref.setFocusMarkets("港股");
            when(userService.updatePreference(any(UserPreferenceDTO.class))).thenReturn(pref);
            mockMvc.perform(put("/api/v1/users/me/preference")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"focusMarkets\":\"港股\",\"riskType\":\"high\"}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET/PUT /api/v1/users/me/privacy")
    class Privacy {
        @Test
        @DisplayName("获取隐私设置")
        void getPrivacy() throws Exception {
            PrivacySetting s = new PrivacySetting();
            s.setProfileVisibility(1);
            when(userService.getPrivacy()).thenReturn(s);
            mockMvc.perform(get("/api/v1/users/me/privacy"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.profileVisibility").value(1));
        }

        @Test
        @DisplayName("更新隐私设置")
        void updatePrivacy() throws Exception {
            PrivacySetting s = new PrivacySetting();
            s.setProfileVisibility(2);
            when(userService.updatePrivacy(any(PrivacySettingDTO.class))).thenReturn(s);
            mockMvc.perform(put("/api/v1/users/me/privacy")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"profileVisibility\":2}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/me/achievement")
    class Achievement {
        @Test
        @DisplayName("获取成就统计")
        void getAchievement() throws Exception {
            UserAchievement a = new UserAchievement();
            a.setTotalPostCount(50);
            when(userService.getAchievement()).thenReturn(a);
            mockMvc.perform(get("/api/v1/users/me/achievement"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalPostCount").value(50));
        }
    }

    @Nested
    @DisplayName("GET/POST /api/v1/users/me/risk-assessment")
    class RiskAssessment {
        @Test
        @DisplayName("提交风险评估")
        void submitRiskAssessment() throws Exception {
            RiskAssessmentAnswer a = new RiskAssessmentAnswer();
            a.setResultLevel("R4");
            when(userService.submitRiskAssessment(any(RiskAssessmentDTO.class))).thenReturn(a);
            mockMvc.perform(post("/api/v1/users/me/risk-assessment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"resultLevel\":\"R4\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("提交成功"));
        }
    }

    @Nested
    @DisplayName("GET/POST /api/v1/users/me/verification")
    class Verification {
        @Test
        @DisplayName("提交认证申请")
        void submitVerification() throws Exception {
            UserVerification v = new UserVerification();
            v.setVerificationType(0);
            when(userService.submitVerification(any(VerificationRequest.class))).thenReturn(v);
            mockMvc.perform(post("/api/v1/users/me/verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"verificationType\":0}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("认证申请已提交"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/search")
    class Search {
        @Test
        @DisplayName("搜索用户")
        void searchUsers() throws Exception {
            when(userService.searchUsers("test", 1, 20)).thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/v1/users/search").param("keyword", "test"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{userId}")
    class GetUserById {
        @Test
        @DisplayName("查看用户主页")
        void getUserById() throws Exception {
            when(userService.getUserById(1L)).thenReturn(createUser());
            mockMvc.perform(get("/api/v1/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nickname").value("testUser"));
        }

        @Test
        @DisplayName("查看不存在用户应返回错误")
        void nonExistent_shouldReturnError() throws Exception {
            when(userService.getUserById(999L)).thenThrow(new RuntimeException("资源不存在"));
            mockMvc.perform(get("/api/v1/users/999"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("资源不存在"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{userId}/dynamics")
    class GetUserDynamics {
        @Test
        @DisplayName("查看用户动态")
        void getUserDynamics() throws Exception {
            when(dynamicService.getByUser(1L)).thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/v1/users/1/dynamics")).andExpect(status().isOk());
        }
    }
}
