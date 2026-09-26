package com.adb4.juno.auth;

import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;

/** A submitted sign-in code together with the email it was sent to, so a code only works for that email. */
public class EmailCodeAuthenticationToken extends OneTimeTokenAuthenticationToken {

    private final String email;

    public EmailCodeAuthenticationToken(String email, String code) {
        super(code);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
