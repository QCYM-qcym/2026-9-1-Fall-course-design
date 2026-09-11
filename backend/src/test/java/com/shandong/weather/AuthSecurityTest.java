package com.shandong.weather;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@org.springframework.context.annotation.Import(com.shandong.weather.security.SecurityConfig.class)
class AuthSecurityTest {
    @MockBean com.shandong.weather.mapper.SysUserMapper users;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    static final String USER_PASSWORD = "DemoUser@2026";
    static final String ADMIN_PASSWORD = "DemoAdmin@2026";
    final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

    @org.junit.jupiter.api.BeforeEach void accounts() {
        for (String role : new String[]{"USER", "ADMIN"}) {
            var u = new com.shandong.weather.entity.SysUser();
            u.setId(role.equals("USER") ? 1L : 2L);
            u.setUsername("demo_" + role.toLowerCase(java.util.Locale.ROOT));
            u.setRole(role); u.setEnabled(true);
            u.setPasswordHash(encoder.encode(role.equals("USER") ? USER_PASSWORD : ADMIN_PASSWORD));
            org.mockito.Mockito.when(users.findByUsername(u.getUsername())).thenReturn(u);
        }
        var disabled = new com.shandong.weather.entity.SysUser();
        disabled.setId(3L); disabled.setUsername("disabled"); disabled.setRole("USER");
        disabled.setEnabled(false); disabled.setPasswordHash(encoder.encode(USER_PASSWORD));
        org.mockito.Mockito.when(users.findByUsername("disabled")).thenReturn(disabled);
    }

    org.springframework.test.web.servlet.ResultActions login(String name, String password, String type,
            org.springframework.mock.web.MockHttpSession session) throws Exception {
        return mvc.perform(post("/api/auth/login").session(session)
                .with(BusinessSecurityTest.CsrfDefaults.csrfCookie())
                .contentType("application/json").content(json.writeValueAsString(
                        java.util.Map.of("username", name, "password", password, "loginType", type, "role", "ADMIN"))));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "demo_user,DemoUser@2026,USER,1", "demo_admin,DemoAdmin@2026,ADMIN,2"
    })
    void databaseRoleAndSafePrincipalSurviveSessionFixation(String name, String password, String role, long id) throws Exception {
        var session = new org.springframework.mock.web.MockHttpSession();
        String oldId = session.getId();
        login(name, password, role, session).andExpect(status().isOk())
                .andExpect(content().json("{\"code\":200,\"message\":\"success\",\"data\":{\"id\":" + id +
                        ",\"username\":\"" + name + "\",\"role\":\"" + role + "\"}}", true));
        org.assertj.core.api.Assertions.assertThat(session.getId()).isNotEqualTo(oldId);
        var context = (org.springframework.security.core.context.SecurityContext)
                session.getAttribute("SPRING_SECURITY_CONTEXT");
        org.assertj.core.api.Assertions.assertThat(context).isNotNull();
        org.assertj.core.api.Assertions.assertThat(context.getAuthentication().getCredentials()).isNull();
        String principal = json.writeValueAsString(context.getAuthentication().getPrincipal());
        org.assertj.core.api.Assertions.assertThat(principal).doesNotContain("password", "Password", "$2");
        var bytes = new java.io.ByteArrayOutputStream();
        try (var stream = new java.io.ObjectOutputStream(bytes)) { stream.writeObject(context); }
        org.assertj.core.api.Assertions.assertThat(bytes.toString(java.nio.charset.StandardCharsets.ISO_8859_1))
                .doesNotContain(password, "$2a$", "$2b$");
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id)).andExpect(jsonPath("$.data.role").value(role))
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.aMapWithSize(3)));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "demo_user,wrong,USER,401", "missing,DemoUser@2026,USER,401",
        "disabled,DemoUser@2026,USER,401", "demo_user,DemoUser@2026,ADMIN,403",
        "demo_admin,DemoAdmin@2026,USER,403", "demo_user,wrong,ADMIN,401"
    })
    void unsuccessfulLoginNeverAuthenticates(String name, String password, String role, int expected) throws Exception {
        var session = new org.springframework.mock.web.MockHttpSession();
        login(name, password, role, session).andExpect(status().is(expected)).andExpect(jsonPath("$.code").value(expected));
        org.assertj.core.api.Assertions.assertThat(session.getAttribute("SPRING_SECURITY_CONTEXT")).isNull();
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test void realCookieHeaderCsrfAndLogoutInvalidation() throws Exception {
        var tokenResponse = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn().getResponse();
        var data = json.readTree(tokenResponse.getContentAsString()).get("data");
        var cookie = tokenResponse.getCookie("XSRF-TOKEN");
        org.assertj.core.api.Assertions.assertThat(cookie.getValue()).isEqualTo(data.get("token").asText());
        var session = new org.springframework.mock.web.MockHttpSession();
        mvc.perform(post("/api/auth/login").session(session).cookie(cookie)
                .header("X-XSRF-TOKEN", cookie.getValue()).contentType("application/json")
                .content("{\"username\":\"demo_user\",\"password\":\"DemoUser@2026\",\"loginType\":\"USER\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").session(session)
                .with(BusinessSecurityTest.CsrfDefaults.csrfCookie()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        org.assertj.core.api.Assertions.assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/cities", "/api/forecast-models", "/api/weather-elements"})
    void userReadsDictionaryButCannotWrite(String path) throws Exception {
        mvc.perform(get(path).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("u").roles("USER")))
                .andExpect(status().isOk());
        for (String method : new String[]{"POST", "PUT", "DELETE"}) {
            mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), path + (method.equals("POST") ? "" : "/1"))
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("u").roles("USER"))
                    .with(BusinessSecurityTest.CsrfDefaults.csrfCookie())
                    .contentType("application/json").content("{}")).andExpect(status().isForbidden());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/weather/workbench", "/api/weather/trend", "/api/weather/comparison"})
    void userCanReachActualQueryController(String path) throws Exception {
        mvc.perform(get(path).param("cityId", "1").param("modelId", "1").param("elementId", "1")
                .param("startTime", "2026-09-01 00:00:00").param("endTime", "2026-09-02 00:00:00")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("u").roles("USER")))
                .andExpect(status().isOk());
    }

    @Test void recordsAndUnknownMethodsAreDenied() throws Exception {
        for (String role : new String[]{"USER", "ADMIN"}) {
            var identity = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("u").roles(role);
            mvc.perform(get("/api/forecast-records").with(identity)).andExpect(status().is(role.equals("ADMIN") ? 200 : 403));
            for (String path : new String[]{"/api/not-found", "/api/cities/1/extra", "/api/auth/login"}) {
                mvc.perform(get(path).with(identity)).andExpect(status().isForbidden());
            }
            mvc.perform(patch("/api/cities/1").with(identity)
                    .with(BusinessSecurityTest.CsrfDefaults.csrfCookie()))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/cities")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        for (String method : new String[]{"POST", "PUT", "DELETE"}) {
            mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method),
                    "/api/forecast-records" + (method.equals("POST") ? "" : "/1"))
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("u").roles("USER"))
                    .with(BusinessSecurityTest.CsrfDefaults.csrfCookie()))
                    .andExpect(status().isForbidden());
        }
    }
    @Autowired MockMvc mvc;
    @MockBean com.shandong.weather.service.CityService cities;
    @MockBean com.shandong.weather.service.ForecastModelService models;
    @MockBean com.shandong.weather.service.WeatherElementService elements;
    @MockBean com.shandong.weather.service.ForecastRecordService records;
    @MockBean com.shandong.weather.service.WeatherQueryService weather;

    @ParameterizedTest
    @ValueSource(strings = {"/api/cities", "/api/forecast-models", "/api/weather-elements", "/api/forecast-records", "/api/auth/me", "/api/not-found"})
    void anonymousCannotReadProtectedResources(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
    }

    @Test void csrfHasCookieAndContract() throws Exception {
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.data.parameterName").value("_csrf"));
    }

    @Test void loginRequiresCsrf() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"demo_user\",\"password\":\"DemoUser@2026\",\"loginType\":\"USER\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
    }

    @Test void loginPageForwardsToSpa() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }

    @Test void mismatchedCsrfHeaderCannotLogin() throws Exception {
        mvc.perform(post("/api/auth/login").cookie(new javax.servlet.http.Cookie("XSRF-TOKEN", "expected"))
                .header("X-XSRF-TOKEN", "wrong").contentType("application/json")
                .content("{\"username\":\"demo_user\",\"password\":\"DemoUser@2026\",\"loginType\":\"USER\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "null", "[]", "{}", "{\"username\":5}", "{} {}"})
    void malformedLoginUsesSafeJson400(String body) throws Exception {
        mvc.perform(post("/api/auth/login").with(BusinessSecurityTest.CsrfDefaults.csrfCookie())
                .contentType("application/json").content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
    }

    @Test void failedReloginClearsPreviousAuthentication() throws Exception {
        var session = new org.springframework.mock.web.MockHttpSession();
        login("demo_admin", ADMIN_PASSWORD, "ADMIN", session).andExpect(status().isOk());
        login("demo_admin", ADMIN_PASSWORD, "USER", session).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        org.assertj.core.api.Assertions.assertThat(session.getAttribute("SPRING_SECURITY_CONTEXT")).isNull();
    }

    @Test void databaseFailuresNeverExposeInternalDetails() throws Exception {
        org.mockito.Mockito.when(users.findByUsername("demo_user"))
                .thenThrow(new IllegalStateException("private database details"));
        login("demo_user", USER_PASSWORD, "USER", new org.springframework.mock.web.MockHttpSession())
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"code\":500,\"message\":\"internal server error\",\"data\":null}", true));
    }
}
