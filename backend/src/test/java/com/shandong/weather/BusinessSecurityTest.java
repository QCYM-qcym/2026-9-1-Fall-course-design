package com.shandong.weather;

import java.lang.annotation.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Existing business assertions run behind the production filter chain. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({com.shandong.weather.security.SecurityConfig.class, BusinessSecurityTest.CsrfDefaults.class})
@MockBean(com.shandong.weather.mapper.SysUserMapper.class)
public @interface BusinessSecurityTest {
    @TestConfiguration
    class CsrfDefaults {
        static RequestPostProcessor csrfCookie() {
            return request -> {
                String token = java.util.UUID.randomUUID().toString();
                request.setCookies(new javax.servlet.http.Cookie("XSRF-TOKEN", token));
                request.addHeader("X-XSRF-TOKEN", token);
                return request;
            };
        }
        @Bean MockMvcBuilderCustomizer businessCsrf() {
            return builder -> builder.defaultRequest(get("/").with(csrfCookie()));
        }
    }
}
