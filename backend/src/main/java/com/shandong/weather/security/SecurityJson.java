package com.shandong.weather.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shandong.weather.common.ApiResponse;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

final class SecurityJson {
    private SecurityJson() { }
    static void write(ObjectMapper json, HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getWriter(), body);
    }
    static void error(ObjectMapper json, HttpServletResponse response, int status) throws IOException {
        String message = status == 401 ? "invalid credentials or unauthenticated" :
                status == 403 ? "forbidden" : status == 400 ? "invalid request body" : "internal server error";
        write(json, response, status, ApiResponse.error(status, message));
    }
}
