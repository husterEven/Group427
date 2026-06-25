package com.forum.service;

import com.forum.dto.*;
import com.forum.entity.*;

import java.util.List;

public interface UserService {

    User getMe();

    User updateMe(UserUpdateRequest req);

    void changePassword(ChangePasswordRequest req);

    UserPreference getPreference();

    UserPreference updatePreference(UserPreferenceDTO dto);

    PrivacySetting getPrivacy();

    PrivacySetting updatePrivacy(PrivacySettingDTO dto);

    UserAchievement getAchievement();

    RiskAssessmentAnswer getRiskAssessment();

    RiskAssessmentAnswer submitRiskAssessment(RiskAssessmentDTO dto);

    List<UserVerification> getVerifications();

    UserVerification submitVerification(VerificationRequest req);

    User getUserById(Long userId);

    List<User> searchUsers(String keyword, int page, int pageSize);
}
