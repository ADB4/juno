package com.adb4.juno.auth;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
class AppUserDetailsService implements UserDetailsService {

    private final JdbcClient jdbc;

    AppUserDetailsService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return jdbc.sql("select id, email from app_user where email = ?")
            .param(Emails.normalize(email))
            .query(AppUserPrincipal.class)
            .optional()
            .orElseThrow(() -> new UsernameNotFoundException("No user with email " + email));
    }
}
