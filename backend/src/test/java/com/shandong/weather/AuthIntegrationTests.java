package com.shandong.weather;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shandong.weather.entity.SysUser;
import com.shandong.weather.mapper.SysUserMapper;
import com.shandong.weather.security.DatabaseUserDetailsService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real MySQL + real authentication chain. Main provides fresh isolated DB credentials.
 * Temporary users are inserted inside rollback transactions; seed users are never updated.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTests {
    @Autowired SysUserMapper users;
    @Autowired DatabaseUserDetailsService details;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test void twoDemoRowsHaveRealMatchingBcryptHashesAndDatabaseRoles() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class)).isEqualTo(2);
        for (String role : new String[]{"USER", "ADMIN"}) {
            String name = "demo_" + role.toLowerCase(java.util.Locale.ROOT);
            SysUser row = users.findByUsername(name);
            assertThat(row).isNotNull();
            assertThat(row.getId()).isPositive();
            assertThat(row.getRole()).isEqualTo(role);
            assertThat(row.getEnabled()).isTrue();
            assertThat(row.getPasswordHash()).startsWith("$2");
            assertThat(encoder.matches(role.equals("USER") ? "DemoUser@2026" : "DemoAdmin@2026",
                    row.getPasswordHash())).isTrue();
            assertThat(encoder.matches("wrong", row.getPasswordHash())).isFalse();
            var loaded = details.loadUserByUsername(name);
            assertThat(loaded.isEnabled()).isTrue();
            assertThat(loaded.getPassword()).isEqualTo(row.getPasswordHash());
            assertThat(loaded.getAuthorities()).extracting("authority").containsExactly("ROLE_" + role);
        }
        assertThat(users.findByUsername("nonexistent_auth_test")).isNull();
    }

    @ParameterizedTest
    @CsvSource({"USER,true,200", "ADMIN,true,200", "USER,false,401", "ADMIN,false,401"})
    void databaseRoleAndEnabledControlRealLoginWithoutChangingSeeds(String role, boolean enabled, int status) throws Exception {
        String username = "auth_test_" + UUID.randomUUID().toString().replace("-", "");
        String password = "AuthTest@2026";
        // Explicit insert avoids any dependency on global MyBatis ID-generation settings.
        jdbc.update("INSERT INTO sys_user (username,password_hash,role,enabled) VALUES (?,?,?,?)",
                username, encoder.encode(password), role, enabled ? 1 : 0);
        SysUser row = users.findByUsername(username);
        assertThat(row.getEnabled()).isEqualTo(enabled);
        var loaded = details.loadUserByUsername(username);
        assertThat(loaded.isEnabled()).isEqualTo(enabled);
        assertThat(loaded.getAuthorities()).extracting("authority").containsExactly("ROLE_" + role);
        var session = new MockHttpSession();
        mvc.perform(post("/api/auth/login").session(session)
                .with(BusinessSecurityTest.CsrfDefaults.csrfCookie()).contentType("application/json")
                .content(json.writeValueAsString(java.util.Map.of("username", username, "password", password, "loginType", role))))
                .andExpect(status().is(status)).andExpect(jsonPath("$.code").value(status));
        if (enabled) {
            mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(row.getId()))
                    .andExpect(jsonPath("$.data.username").value(username))
                    .andExpect(jsonPath("$.data.role").value(role))
                    .andExpect(jsonPath("$.data", org.hamcrest.Matchers.aMapWithSize(3)));
        } else {
            assertThat(session.getAttribute("SPRING_SECURITY_CONTEXT")).isNull();
            mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        }
    }
}
