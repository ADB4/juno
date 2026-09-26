package com.adb4.juno.auth;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    record Me(UUID id, String email) {
    }

    @GetMapping("/me")
    Me me(@AuthenticationPrincipal AppUserPrincipal user) {
        return new Me(user.id(), user.email());
    }

    @GetMapping("/csrf")
    ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken(); // reading the token makes Spring Security write the XSRF-TOKEN cookie
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/code")
    ResponseEntity<Void> invalidEmail() {
        // Valid requests are handled by Spring Security's code filter and never get here;
        // it passes the request on only when the email is missing or malformed.
        return ResponseEntity.badRequest().build();
    }
}
