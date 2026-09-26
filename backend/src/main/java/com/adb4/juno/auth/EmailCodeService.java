package com.adb4.juno.auth;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.ott.DefaultOneTimeToken;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and checks 6-digit email sign-in codes. Codes are bound to the email they were sent to,
 * stored only as a hash, expire after {@link #CODE_TTL}, and are burned after {@link #MAX_ATTEMPTS} wrong guesses.
 */
@Service
public class EmailCodeService implements OneTimeTokenService {

    public static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 5;

    private final SecureRandom random = new SecureRandom();
    private final JdbcClient jdbc;

    public EmailCodeService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public OneTimeToken generate(GenerateOneTimeTokenRequest request) {
        String email = Emails.normalize(request.getUsername());
        String code = "%06d".formatted(random.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plus(request.getExpiresIn());
        jdbc.sql("""
                insert into login_code (email, code_hash, expires_at, attempts)
                values (:email, :hash, :expiresAt, 0)
                on conflict (email) do update
                    set code_hash = excluded.code_hash, expires_at = excluded.expires_at, attempts = 0
                """)
            .param("email", email)
            .param("hash", hash(email, code))
            .param("expiresAt", expiresAt.atOffset(ZoneOffset.UTC))
            .update();
        return new DefaultOneTimeToken(code, email, expiresAt);
    }

    @Override
    @Transactional
    public OneTimeToken consume(OneTimeTokenAuthenticationToken authentication) {
        if (!(authentication instanceof EmailCodeAuthenticationToken submitted) || submitted.getTokenValue() == null) {
            return null;
        }
        String email = Emails.normalize(submitted.getEmail());
        String code = submitted.getTokenValue().strip();

        StoredCode stored = jdbc.sql("select code_hash, expires_at, attempts from login_code where email = ? for update")
            .param(email)
            .query((rs, row) -> new StoredCode(
                rs.getString("code_hash"),
                rs.getObject("expires_at", OffsetDateTime.class).toInstant(),
                rs.getInt("attempts")))
            .optional()
            .orElse(null);
        if (stored == null) {
            return null;
        }
        if (stored.expiresAt().isBefore(Instant.now()) || stored.attempts() >= MAX_ATTEMPTS) {
            deleteCode(email);
            return null;
        }
        if (!MessageDigest.isEqual(hash(email, code).getBytes(UTF_8), stored.hash().getBytes(UTF_8))) {
            jdbc.sql("update login_code set attempts = attempts + 1 where email = ?").param(email).update();
            return null;
        }

        deleteCode(email);
        // Open sign-up: the account is created the first time someone proves they own the email.
        jdbc.sql("insert into app_user (email) values (?) on conflict (email) do nothing").param(email).update();
        return new DefaultOneTimeToken(code, email, stored.expiresAt());
    }

    private void deleteCode(String email) {
        jdbc.sql("delete from login_code where email = ?").param(email).update();
    }

    private static String hash(String email, String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest((email + ":" + code).getBytes(UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private record StoredCode(String hash, Instant expiresAt, int attempts) {
    }
}
