package com.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forum.common.SecurityUtil;
import com.forum.dto.*;
import com.forum.entity.*;
import com.forum.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl 用户服务 单元测试")
class UserServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private UserPreferenceMapper userPreferenceMapper;
    @Mock private PrivacySettingMapper privacySettingMapper;
    @Mock private UserAchievementMapper userAchievementMapper;
    @Mock private RiskAssessmentAnswerMapper riskAssessmentAnswerMapper;
    @Mock private UserVerificationMapper userVerificationMapper;
    @Mock private SecurityUtil securityUtil;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User createUser(Long id, String nickname) {
        User user = new User();
        user.setUserId(id);
        user.setNickname(nickname);
        user.setPasswordHash("hashed_xxx");
        user.setEmail("test@test.com");
        user.setMobile("13800138000");
        user.setIsDeleted(0);
        return user;
    }

    @Nested
    @DisplayName("getMe() 获取当前用户")
    class GetMe {

        @Test
        @DisplayName("有认证用户时返回 User")
        void shouldReturnCurrentUser() {
            User user = createUser(1L, "test");
            when(securityUtil.getCurrentUser()).thenReturn(user);

            assertEquals(user, userService.getMe());
        }
    }

    @Nested
    @DisplayName("updateMe() 更新个人信息")
    class UpdateMe {

        @Test
        @DisplayName("更新昵称和头像")
        void updateNicknameAndAvatar() {
            User user = createUser(1L, "old");
            when(securityUtil.getCurrentUser()).thenReturn(user);
            when(userMapper.updateById(any(User.class))).thenReturn(1);

            UserUpdateRequest req = new UserUpdateRequest();
            req.setNickname("newName");
            req.setAvatarUrl("/avatar.png");
            req.setBio("bio text");
            req.setGender(1);

            User result = userService.updateMe(req);

            assertEquals("newName", result.getNickname());
            assertEquals("/avatar.png", result.getAvatarUrl());
            assertEquals("bio text", result.getBio());
            assertEquals(1, result.getGender());
        }

        @Test
        @DisplayName("部分更新 - 只改昵称")
        void partialUpdate_nicknameOnly() {
            User user = createUser(1L, "old");
            when(securityUtil.getCurrentUser()).thenReturn(user);
            when(userMapper.updateById(any(User.class))).thenReturn(1);

            UserUpdateRequest req = new UserUpdateRequest();
            req.setNickname("new");

            User result = userService.updateMe(req);
            assertEquals("new", result.getNickname());
        }
    }

    @Nested
    @DisplayName("changePassword() 修改密码")
    class ChangePassword {

        @Test
        @DisplayName("原密码正确时应成功修改")
        void correctOldPassword_shouldSucceed() {
            User user = createUser(1L, "test");
            when(securityUtil.getCurrentUser()).thenReturn(user);
            when(passwordEncoder.matches("oldPwd", "hashed_xxx")).thenReturn(true);
            when(passwordEncoder.encode("newPwd")).thenReturn("new_hashed");
            when(userMapper.updateById(any(User.class))).thenReturn(1);

            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setOldPassword("oldPwd");
            req.setNewPassword("newPwd");

            assertDoesNotThrow(() -> userService.changePassword(req));
        }

        @Test
        @DisplayName("原密码错误应抛出 RuntimeException")
        void wrongOldPassword_shouldThrow() {
            User user = createUser(1L, "test");
            when(securityUtil.getCurrentUser()).thenReturn(user);
            when(passwordEncoder.matches("wrongPwd", "hashed_xxx")).thenReturn(false);

            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setOldPassword("wrongPwd");
            req.setNewPassword("newPwd");

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.changePassword(req));
            assertEquals("原密码错误", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("getPreference() / updatePreference() 投资偏好")
    class Preference {

        @Test
        @DisplayName("获取已存在的偏好")
        void getExistingPreference() {
            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            UserPreference pref = new UserPreference();
            pref.setFocusMarkets("A股,港股");
            when(userPreferenceMapper.selectOne(any(QueryWrapper.class))).thenReturn(pref);

            assertEquals("A股,港股", userService.getPreference().getFocusMarkets());
        }

        @Test
        @DisplayName("更新已有偏好")
        void updateExistingPreference() {
            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            UserPreference existing = new UserPreference();
            existing.setFocusMarkets("A股");
            when(userPreferenceMapper.selectOne(any(QueryWrapper.class))).thenReturn(existing);
            when(userPreferenceMapper.updateById(any(UserPreference.class))).thenReturn(1);

            UserPreferenceDTO dto = new UserPreferenceDTO();
            dto.setFocusMarkets("港股,美股");
            dto.setRiskType("high");

            UserPreference result = userService.updatePreference(dto);
            assertEquals("港股,美股", result.getFocusMarkets());
            assertEquals("high", result.getRiskType());
        }

        @Test
        @DisplayName("创建新的偏好")
        void createNewPreference() {
            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            when(userPreferenceMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
            when(userPreferenceMapper.insert(any(UserPreference.class))).thenReturn(1);

            UserPreferenceDTO dto = new UserPreferenceDTO();
            dto.setFocusMarkets("基金");
            dto.setRiskType("low");

            UserPreference result = userService.updatePreference(dto);
            assertEquals("基金", result.getFocusMarkets());
        }
    }

    @Nested
    @DisplayName("getPrivacy() / updatePrivacy() 隐私设置")
    class Privacy {

        @Test
        @DisplayName("获取隐私设置")
        void getPrivacy() {
            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            PrivacySetting setting = new PrivacySetting();
            setting.setProfileVisibility(2);
            when(privacySettingMapper.selectOne(any(QueryWrapper.class))).thenReturn(setting);

            assertEquals(2, userService.getPrivacy().getProfileVisibility());
        }

        @Test
        @DisplayName("更新隐私设置")
        void updatePrivacy() {
            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            PrivacySetting existing = new PrivacySetting();
            existing.setProfileVisibility(0);
            when(privacySettingMapper.selectOne(any(QueryWrapper.class))).thenReturn(existing);
            when(privacySettingMapper.updateById(any(PrivacySetting.class))).thenReturn(1);

            PrivacySettingDTO dto = new PrivacySettingDTO();
            dto.setProfileVisibility(1);

            PrivacySetting result = userService.updatePrivacy(dto);
            assertEquals(1, result.getProfileVisibility());
        }
    }

    @Nested
    @DisplayName("getAchievement() 成就")
    class Achievement {

        @Test
        @DisplayName("获取成就统计")
        void getAchievement() {
            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            UserAchievement achievement = new UserAchievement();
            achievement.setTotalPostCount(50);
            when(userAchievementMapper.selectOne(any(QueryWrapper.class))).thenReturn(achievement);

            assertEquals(50, userService.getAchievement().getTotalPostCount());
        }
    }

    @Nested
    @DisplayName("submitVerification() 认证申请")
    class Verification {

        @Test
        @DisplayName("提交认证申请应成功")
        void submitVerification() {
            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            when(userVerificationMapper.insert(any(UserVerification.class))).thenReturn(1);

            VerificationRequest req = new VerificationRequest();
            req.setVerificationType(0);

            UserVerification result = userService.submitVerification(req);
            assertEquals(0, result.getVerificationType());
            assertEquals(0, result.getAuditStatus());
        }
    }

    @Nested
    @DisplayName("getUserById() 查看用户主页")
    class GetUserById {

        @Test
        @DisplayName("存在的用户应返回信息（手机邮箱已脱敏）")
        void existingUser_shouldReturnMaskedInfo() {
            User user = createUser(1L, "test");
            when(userMapper.selectById(1L)).thenReturn(user);

            User result = userService.getUserById(1L);

            assertNotNull(result);
            assertEquals("test", result.getNickname());
            assertNull(result.getMobile()); // 隐私脱敏
            assertNull(result.getEmail());   // 隐私脱敏
        }

        @Test
        @DisplayName("不存在的用户应抛出 RuntimeException")
        void nonExistentUser_shouldThrow() {
            when(userMapper.selectById(999L)).thenReturn(null);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.getUserById(999L));
            assertEquals("资源不存在", ex.getMessage());
        }

        @Test
        @DisplayName("已删除用户应抛出 RuntimeException")
        void deletedUser_shouldThrow() {
            User user = createUser(1L, "deleted");
            user.setIsDeleted(1);
            when(userMapper.selectById(1L)).thenReturn(user);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.getUserById(1L));
            assertEquals("资源不存在", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("searchUsers() 搜索用户")
    class SearchUsers {

        @Test
        @DisplayName("按昵称搜索应返回匹配用户")
        void searchByNickname() {
            when(userMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(new Page<>(1, 10));

            List<User> result = userService.searchUsers("test", 1, 10);
            assertNotNull(result);
        }
    }
}
