package com.adb4.juno.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class EmailCodeSender implements OneTimeTokenGenerationSuccessHandler {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailCodeSender(JavaMailSender mailSender, @Value("${juno.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, OneTimeToken token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(token.getUsername());
        message.setSubject("Your Juno sign-in code");
        message.setText("""
                Your Juno sign-in code is %s

                It expires in %d minutes. If you didn't try to sign in, you can ignore this email.
                """.formatted(token.getTokenValue(), EmailCodeService.CODE_TTL.toMinutes()));
        mailSender.send(message);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
