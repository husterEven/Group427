package com.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forum.common.SecurityUtil;
import com.forum.dto.*;
import com.forum.entity.*;
import com.forum.mapper.*;
import com.forum.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final PrivacySettingMapper privacySettingMapper;
    private final UserAchievementMapper userAchievementMapper;
    private final RiskAssessmentAnswerMapper riskAssessmentAnswerMapper;
    private final UserVerificationMapper userVerificationMapper;
    private final FollowMapper followMapper;
    private final SecurityUtil securityUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User getMe() {
        return securityUtil.getCurrentUser();
    }

    @Override
    public User updateMe(UserUpdateRequest req) {
        User user = securityUtil.getCurrentUser();
        if (req.getNickname() != null) {
            user.setNickname(req.getNickname());
        }
        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(req.getAvatarUrl());
        }
        if (req.getBio() != null) {
            user.setBio(req.getBio());
        }
        if (req.getGender() != null) {
            user.setGender(req.getGender());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return user;
    }

    @Override
    public void changePassword(ChangePasswordRequest req) {
        User user = securityUtil.getCurrentUser();
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())) {
            throw new RuntimeException("原密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public UserPreference getPreference() {
        Long userId = securityUtil.getCurrentUserId();
        return userPreferenceMapper.selectOne(
                new QueryWrapper<UserPreference>().eq("user_id", userId));
    }

    @Override
    @Transactional
    public UserPreference updatePreference(UserPreferenceDTO dto) {
        Long userId = securityUtil.getCurrentUserId();
        UserPreference existing = userPreferenceMapper.selectOne(
                new QueryWrapper<UserPreference>().eq("user_id", userId));
        if (existing != null) {
            existing.setFocusMarkets(dto.getFocusMarkets());
            existing.setRiskType(dto.getRiskType());
            existing.setUpdatedAt(LocalDateTime.now());
            userPreferenceMapper.updateById(existing);
            return existing;
        } else {
            UserPreference preference = new UserPreference();
            preference.setUserId(userId);
            preference.setFocusMarkets(dto.getFocusMarkets());
            preference.setRiskType(dto.getRiskType());
            preference.setCreatedAt(LocalDateTime.now());
            preference.setUpdatedAt(LocalDateTime.now());
            userPreferenceMapper.insert(preference);
            return preference;
        }
    }

    @Override
    public PrivacySetting getPrivacy() {
        Long userId = securityUtil.getCurrentUserId();
        return privacySettingMapper.selectOne(
                new QueryWrapper<PrivacySetting>().eq("user_id", userId));
    }

    @Override
    @Transactional
    public PrivacySetting updatePrivacy(PrivacySettingDTO dto) {
        Long userId = securityUtil.getCurrentUserId();
        PrivacySetting existing = privacySettingMapper.selectOne(
                new QueryWrapper<PrivacySetting>().eq("user_id", userId));
        if (existing != null) {
            existing.setProfileVisibility(dto.getProfileVisibility());
            existing.setUpdatedAt(LocalDateTime.now());
            privacySettingMapper.updateById(existing);
            return existing;
        } else {
            PrivacySetting privacy = new PrivacySetting();
            privacy.setUserId(userId);
            privacy.setProfileVisibility(dto.getProfileVisibility());
            privacy.setUpdatedAt(LocalDateTime.now());
            privacySettingMapper.insert(privacy);
            return privacy;
        }
    }

    @Override
    public UserAchievement getAchievement() {
        Long userId = securityUtil.getCurrentUserId();
        return userAchievementMapper.selectOne(
                new QueryWrapper<UserAchievement>().eq("user_id", userId));
    }

    @Override
    public RiskAssessmentAnswer getRiskAssessment() {
        Long userId = securityUtil.getCurrentUserId();
        return riskAssessmentAnswerMapper.selectOne(
                new QueryWrapper<RiskAssessmentAnswer>().eq("user_id", userId));
    }

    @Override
    @Transactional
    public RiskAssessmentAnswer submitRiskAssessment(RiskAssessmentDTO dto) {
        Long userId = securityUtil.getCurrentUserId();
        RiskAssessmentAnswer existing = riskAssessmentAnswerMapper.selectOne(
                new QueryWrapper<RiskAssessmentAnswer>().eq("user_id", userId));
        if (existing != null) {
            existing.setResultLevel(dto.getResultLevel());
            existing.setCompleteTime(LocalDateTime.now());
            riskAssessmentAnswerMapper.updateById(existing);
            return existing;
        } else {
            RiskAssessmentAnswer answer = new RiskAssessmentAnswer();
            answer.setUserId(userId);
            answer.setResultLevel(dto.getResultLevel());
            answer.setCompleteTime(LocalDateTime.now());
            riskAssessmentAnswerMapper.insert(answer);
            return answer;
        }
    }

    @Override
    public List<UserVerification> getVerifications() {
        Long userId = securityUtil.getCurrentUserId();
        return userVerificationMapper.selectList(
                new QueryWrapper<UserVerification>()
                        .eq("user_id", userId)
                        .orderByDesc("created_at"));
    }

    @Override
    @Transactional
    public UserVerification submitVerification(VerificationRequest req) {
        Long userId = securityUtil.getCurrentUserId();
        UserVerification verification = new UserVerification();
        verification.setUserId(userId);
        verification.setVerificationType(req.getVerificationType());
        verification.setAuditStatus(0);
        verification.setCreatedAt(LocalDateTime.now());
        userVerificationMapper.insert(verification);
        return verification;
    }

    @Override
    public User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("资源不存在");
        }
        if (user.getIsDeleted() != null && user.getIsDeleted() == 1) {
            throw new RuntimeException("资源不存在");
        }
        user.setMobile(null);
        user.setEmail(null);
        Long currentUserId = securityUtil.getCurrentUserIdOrNull();
        if (currentUserId != null) {
            Follow follow = followMapper.selectByBoth(currentUserId, userId);
            user.setIsFollowed(follow != null);
        }
        user.setFollowerCount(followMapper.selectByFolloweeId(userId).size());
        user.setFollowingCount(followMapper.selectByFollowerId(userId).size());
        return user;
    }

    @Override
    public List<User> searchUsers(String keyword, int page, int pageSize) {
        Page<User> userPage = new Page<>(page, pageSize);
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.like("nickname", keyword);
        wrapper.eq("is_deleted", 0);
        userMapper.selectPage(userPage, wrapper);
        return userPage.getRecords();
    }
}
