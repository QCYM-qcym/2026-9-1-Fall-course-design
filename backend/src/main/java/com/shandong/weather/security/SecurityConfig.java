package com.shandong.weather.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shandong.weather.common.ApiResponse;
import com.shandong.weather.mapper.SysUserMapper;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean DatabaseUserDetailsService databaseUserDetailsService(SysUserMapper users) {
        return new DatabaseUserDetailsService(users);
    }
    @Bean WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> sessionCookieHttpOnly() {
        return factory -> factory.addInitializers(context -> context.getSessionCookieConfig().setHttpOnly(true));
    }
    @Bean CookieSameSiteSupplier sessionSameSite() {
        return CookieSameSiteSupplier.ofLax().whenHasName("JSESSIONID");
    }

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, DatabaseUserDetailsService users,
            PasswordEncoder encoder, ObjectMapper json) throws Exception {
        var csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookiePath("/");
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(users);
        provider.setPasswordEncoder(encoder);
        var manager = new ProviderManager(provider);
        var login = new JsonLoginFilter(manager, json);
        login.setSessionAuthenticationStrategy(new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrf))));
        http.csrf().csrfTokenRepository(csrf).and()
                .requestCache().disable()
                .formLogin().disable().httpBasic().disable()
                .exceptionHandling()
                .authenticationEntryPoint((request, response, exception) -> SecurityJson.error(json, response, 401))
                .accessDeniedHandler((request, response, exception) -> SecurityJson.error(json, response, 403))
                .and().authorizeRequests()
                .antMatchers(HttpMethod.GET, "/", "/index.html", "/login", "/weather", "/analysis",
                        "/comparison", "/management", "/assets/**", "/favicon.ico", "/api/health", "/api/auth/csrf").permitAll()
                .antMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout").permitAll()
                .antMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                .antMatchers(HttpMethod.GET, "/api/cities", "/api/forecast-models", "/api/weather-elements",
                        "/api/weather/workbench", "/api/weather/trend", "/api/weather/comparison").hasAnyRole("USER", "ADMIN")
                .antMatchers(HttpMethod.GET, "/api/forecast-records").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST, "/api/cities", "/api/forecast-models", "/api/weather-elements",
                        "/api/forecast-records").hasRole("ADMIN")
                .antMatchers(HttpMethod.PUT, "/api/cities/*", "/api/forecast-models/*", "/api/weather-elements/*",
                        "/api/forecast-records/*").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/cities/*", "/api/forecast-models/*", "/api/weather-elements/*",
                        "/api/forecast-records/*").hasRole("ADMIN")
                .anyRequest().denyAll()
                .and().sessionManagement().sessionFixation().changeSessionId()
                .and().logout().logoutUrl("/api/auth/logout").invalidateHttpSession(true)
                .clearAuthentication(true).deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) ->
                        SecurityJson.write(json, response, 200, ApiResponse.success(null)));
        http.addFilterAt(login, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
