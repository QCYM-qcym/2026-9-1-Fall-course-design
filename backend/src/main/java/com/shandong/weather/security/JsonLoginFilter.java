package com.shandong.weather.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shandong.weather.common.ApiResponse;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

final class JsonLoginFilter extends AbstractAuthenticationProcessingFilter {
    private final ObjectMapper json;

    JsonLoginFilter(AuthenticationManager manager, ObjectMapper json) {
        super(new AntPathRequestMatcher("/api/auth/login", "POST"), manager);
        this.json = json;
        setAuthenticationSuccessHandler((request, response, authentication) ->
                SecurityJson.write(json, response, 200, ApiResponse.success(authentication.getPrincipal())));
        setAuthenticationFailureHandler((request, response, exception) -> {
            var session = request.getSession(false);
            if (session != null) session.removeAttribute("SPRING_SECURITY_CONTEXT");
            int status = exception instanceof LoginTypeMismatch ? 403 :
                    exception instanceof InvalidLoginBody ? 400 :
                    exception instanceof InternalAuthenticationServiceException ? 500 : 401;
            SecurityJson.error(json, response, status);
        });
    }

    @Override public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException {
        JsonNode body;
        try {
            if (request.getContentType() == null ||
                    !MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(request.getContentType()))) {
                throw new InvalidLoginBody();
            }
            byte[] bytes = request.getInputStream().readNBytes(4097);
            if (bytes.length > 4096) throw new InvalidLoginBody();
            body = json.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readTree(bytes);
            if (body == null || !body.isObject()) throw new InvalidLoginBody();
        } catch (IOException | IllegalArgumentException exception) {
            throw new InvalidLoginBody();
        }
        String username = text(body, "username");
        String password = text(body, "password");
        String loginType = text(body, "loginType");
        if (username.isBlank() || username.length() > 64 || password.isEmpty() ||
                password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72 ||
                !("USER".equals(loginType) || "ADMIN".equals(loginType))) throw new InvalidLoginBody();
        var input = new UsernamePasswordAuthenticationToken(username, password);
        Authentication verified;
        try {
            verified = getAuthenticationManager().authenticate(input);
        } finally {
            input.eraseCredentials();
        }
        var user = (DatabaseUserDetailsService.LoginUser) verified.getPrincipal();
        // Check the entry only AFTER BCrypt and enabled checks, BEFORE any session strategy/success.
        if (!user.safe.role().equals(loginType)) throw new LoginTypeMismatch();
        return new UsernamePasswordAuthenticationToken(user.safe, null, verified.getAuthorities());
    }

    private String text(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || !value.isTextual()) throw new InvalidLoginBody();
        return value.textValue();
    }

    private static final class InvalidLoginBody extends AuthenticationException {
        InvalidLoginBody() { super("invalid request body"); }
    }
    private static final class LoginTypeMismatch extends AuthenticationException {
        LoginTypeMismatch() { super("forbidden"); }
    }
}
