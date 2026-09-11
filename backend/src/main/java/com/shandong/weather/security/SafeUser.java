package com.shandong.weather.security;

import java.io.Serializable;

/** The only user object allowed in authenticated sessions and authentication responses. */
public record SafeUser(Long id, String username, String role) implements Serializable { }
