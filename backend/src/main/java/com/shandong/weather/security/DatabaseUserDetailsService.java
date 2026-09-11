package com.shandong.weather.security;

import com.shandong.weather.mapper.SysUserMapper;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.AuthorityUtils;

public class DatabaseUserDetailsService implements UserDetailsService {
    private final SysUserMapper users;
    public DatabaseUserDetailsService(SysUserMapper users) { this.users = users; }

    @Override public UserDetails loadUserByUsername(String username) {
        var row = users.findByUsername(username);
        if (row == null || !("USER".equals(row.getRole()) || "ADMIN".equals(row.getRole()))) {
            throw new UsernameNotFoundException("invalid credentials");
        }
        return new LoginUser(new SafeUser(row.getId(), row.getUsername(), row.getRole()),
                row.getPasswordHash(), Boolean.TRUE.equals(row.getEnabled()));
    }

    /** Temporary provider input; replaced with SafeUser before authentication succeeds. */
    static final class LoginUser extends User {
        final SafeUser safe;
        LoginUser(SafeUser safe, String hash, boolean enabled) {
            super(safe.username(), hash, enabled, true, true, true,
                    AuthorityUtils.createAuthorityList("ROLE_" + safe.role()));
            this.safe = safe;
        }
    }
}
