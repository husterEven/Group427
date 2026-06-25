package com.forum.service;

import com.forum.dto.*;

public interface AuthService {

    TokenResponse register(RegisterRequest req);

    TokenResponse login(LoginRequest req);

    TokenResponse refresh(RefreshTokenRequest req);

    void logout();
}
