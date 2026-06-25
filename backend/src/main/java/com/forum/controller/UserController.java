package com.forum.controller;

import com.forum.common.Result;
import com.forum.dto.ChangePasswordRequest;
import com.forum.dto.PrivacySettingDTO;
import com.forum.dto.RiskAssessmentDTO;
import com.forum.dto.UserPreferenceDTO;
import com.forum.dto.UserUpdateRequest;
import com.forum.dto.VerificationRequest;
import com.forum.service.DynamicService;
import com.forum.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final DynamicService dynamicService;

    @GetMapping("/me")
    public Result<?> getMe() {
        return Result.ok(userService.getMe());
    }

    @PutMapping("/me")
    public Result<?> updateMe(@Valid @RequestBody UserUpdateRequest request) {
        return Result.ok("更新成功", userService.updateMe(request));
    }

    @PutMapping("/me/password")
    public Result<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return Result.ok("密码修改成功", null);
    }

    @GetMapping("/me/preference")
    public Result<?> getPreference() {
        return Result.ok(userService.getPreference());
    }

    @PutMapping("/me/preference")
    public Result<?> updatePreference(@Valid @RequestBody UserPreferenceDTO request) {
        return Result.ok("更新成功", userService.updatePreference(request));
    }

    @GetMapping("/me/privacy")
    public Result<?> getPrivacy() {
        return Result.ok(userService.getPrivacy());
    }

    @PutMapping("/me/privacy")
    public Result<?> updatePrivacy(@Valid @RequestBody PrivacySettingDTO request) {
        return Result.ok("更新成功", userService.updatePrivacy(request));
    }

    @GetMapping("/me/achievement")
    public Result<?> getAchievement() {
        return Result.ok(userService.getAchievement());
    }

    @GetMapping("/me/risk-assessment")
    public Result<?> getRiskAssessment() {
        return Result.ok(userService.getRiskAssessment());
    }

    @PostMapping("/me/risk-assessment")
    public Result<?> submitRiskAssessment(@Valid @RequestBody RiskAssessmentDTO request) {
        return Result.ok("提交成功", userService.submitRiskAssessment(request));
    }

    @GetMapping("/me/verification")
    public Result<?> getVerifications() {
        return Result.ok(userService.getVerifications());
    }

    @PostMapping("/me/verification")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<?> submitVerification(@Valid @RequestBody VerificationRequest request) {
        return Result.ok("认证申请已提交", userService.submitVerification(request));
    }

    @GetMapping("/search")
    public Result<?> search(@RequestParam String keyword,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(userService.searchUsers(keyword, page, pageSize));
    }

    @GetMapping("/{userId}")
    public Result<?> getUserById(@PathVariable Long userId) {
        return Result.ok(userService.getUserById(userId));
    }

    @GetMapping("/{userId}/dynamics")
    public Result<?> getUserDynamics(@PathVariable Long userId) {
        return Result.ok(dynamicService.getByUser(userId));
    }
}
