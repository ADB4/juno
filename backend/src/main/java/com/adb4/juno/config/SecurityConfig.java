package com.adb4.juno.config;

import com.adb4.juno.auth.EmailCodeAuthenticationToken;
import com.adb4.juno.auth.EmailCodeSender;
import com.adb4.juno.auth.EmailCodeService;
import com.adb4.juno.auth.Emails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, EmailCodeService emailCodeService,
                                            EmailCodeSender emailCodeSender) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/hello").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/code").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf.spa())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .oneTimeTokenLogin(ott -> ott
                .tokenGeneratingUrl("/api/auth/code")
                .loginProcessingUrl("/api/auth/code/verify")
                .showDefaultSubmitPage(false)
                .tokenService(emailCodeService)
                .tokenGenerationSuccessHandler(emailCodeSender)
                .generateRequestResolver(SecurityConfig::codeRequest)
                .authenticationConverter(SecurityConfig::codeSubmission)
                .successHandler((request, response, authentication) ->
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT))
                .failureHandler((request, response, exception) ->
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED))
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
            );
        return http.build();
    }

    /** POST /api/auth/code with form field {@code username}: returning null skips code generation. */
    private static GenerateOneTimeTokenRequest codeRequest(HttpServletRequest request) {
        String email = request.getParameter("username");
        if (!Emails.isValid(email)) {
            return null;
        }
        return new GenerateOneTimeTokenRequest(Emails.normalize(email), EmailCodeService.CODE_TTL);
    }

    /** POST /api/auth/code/verify with form fields {@code username} and {@code token}: binds the code to the email. */
    private static Authentication codeSubmission(HttpServletRequest request) {
        String email = request.getParameter("username");
        String code = request.getParameter("token");
        if (email == null || code == null) {
            return null;
        }
        return new EmailCodeAuthenticationToken(Emails.normalize(email), code);
    }
}
