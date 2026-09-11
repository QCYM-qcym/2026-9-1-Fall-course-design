package com.shandong.weather;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.assertj.core.api.Assertions.assertThat;

class AuthSeedTest {
    @Test void publicDemoSeedAuthenticatesOnlyItsDocumentedPasswords() throws Exception {
        Path file = Path.of("../database/auth-data.sql");
        assertThat(file).exists();
        String sql = Files.readString(file);
        var rows = Pattern.compile("\\('(?<user>demo_user|demo_admin)',\\s*'(?<hash>\\$2[aby]\\$[^']+)',\\s*'(?<role>USER|ADMIN)',\\s*1\\)").matcher(sql);
        var encoder = new BCryptPasswordEncoder();
        int count = 0;
        while (rows.find()) {
            boolean user = rows.group("user").equals("demo_user");
            assertThat(rows.group("role")).isEqualTo(user ? "USER" : "ADMIN");
            assertThat(encoder.matches(user ? "DemoUser@2026" : "DemoAdmin@2026", rows.group("hash"))).isTrue();
            assertThat(encoder.matches("wrong", rows.group("hash"))).isFalse();
            count++;
        }
        assertThat(count).isEqualTo(2);
    }
}
