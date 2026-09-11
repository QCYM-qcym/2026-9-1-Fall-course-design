package com.shandong.weather.controller;

import com.shandong.weather.common.ApiResponse;
import com.shandong.weather.security.SafeUser;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    @GetMapping("/api/auth/me")
    public ApiResponse<SafeUser> me(@AuthenticationPrincipal SafeUser user) {
        return ApiResponse.success(user);
    }
    @GetMapping("/api/auth/csrf")
    public ApiResponse<Map<String, String>> csrf(CsrfToken token) {
        return ApiResponse.success(Map.of("token", token.getToken(), "headerName", token.getHeaderName(),
                "parameterName", token.getParameterName()));
    }
}
