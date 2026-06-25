package com.forum.service.impl;

import com.forum.common.SecurityUtil;
import com.forum.config.JwtUtil;
import com.forum.dto.*;
import com.forum.entity.*;
import com.forum.mapper.*;
import com.forum.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final PrivacySettingMapper privacySettingMapper;
    private final UserAchievementMapper userAchievementMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest req) {
        User existing = userMapper.selectByAccount(req.getAccount());
        if (existing != null) {
            throw new RuntimeException("账号已存在");
        }

        User user = new User();
        user.setNickname(req.getNickname());
        if (req.getAccount().contains("@")) {
            user.setEmail(req.getAccount());
        } else {
            user.setMobile(req.getAccount());
        }
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setLevel(1);
        user.setPoints(0);
        user.setIsBanned(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        UserPreference preference = new UserPreference();
        preference.setUserId(user.getUserId());
        preference.setFocusMarkets("");
        preference.setRiskType("");
        preference.setCreatedAt(LocalDateTime.now());
        preference.setUpdatedAt(LocalDateTime.now());
        userPreferenceMapper.insert(preference);

        PrivacySetting privacy = new PrivacySetting();
        privacy.setUserId(user.getUserId());
        privacy.setProfileVisibility(0);
        privacy.setUpdatedAt(LocalDateTime.now());
        privacySettingMapper.insert(privacy);

        UserAchievement achievement = new UserAchievement();
        achievement.setUserId(user.getUserId());
        achievement.setTotalPostCount(0);
        achievement.setEssencePostCount(0);
        achievement.setUpdatedAt(LocalDateTime.now());
        userAchievementMapper.insert(achievement);

        TokenResponse resp = new TokenResponse();
        resp.setAccessToken(jwtUtil.generateAccessToken(user.getUserId()));
        resp.setRefreshToken(jwtUtil.generateRefreshToken(user.getUserId()));
        return resp;
    }

    @Override
    public TokenResponse login(LoginRequest req) {
        User user = userMapper.selectByAccount(req.getAccount());
        if (user == null) {
            throw new RuntimeException("账号或密码错误");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("账号或密码错误");
        }
        if (user.getIsBanned() != null && user.getIsBanned() == 1) {
            throw new RuntimeException("账号已被封禁");
        }
        if (user.getIsDeleted() != null && user.getIsDeleted() == 1) {
            throw new RuntimeException("账号不存在");
        }

        TokenResponse resp = new TokenResponse();
        resp.setAccessToken(jwtUtil.generateAccessToken(user.getUserId()));
        resp.setRefreshToken(jwtUtil.generateRefreshToken(user.getUserId()));
        return resp;
    }

    @Override
    public TokenResponse refresh(RefreshTokenRequest req) {
        if (!jwtUtil.validateToken(req.getRefreshToken())) {
            throw new RuntimeException("refreshToken无效或已过期");
        }
        Long userId = jwtUtil.getUserIdFromToken(req.getRefreshToken());

        TokenResponse resp = new TokenResponse();
        resp.setAccessToken(jwtUtil.generateAccessToken(userId));
        resp.setRefreshToken(jwtUtil.generateRefreshToken(userId));
        return resp;
    }

    @Override
    public void logout() {
        // stateless JWT — client handles token removal
    }
}
