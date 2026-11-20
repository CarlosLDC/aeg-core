package com.aeg.core.security;

import com.aeg.core.mail.AuthEmailTemplateService;
import com.aeg.core.mail.EmailDeliveryException;
import com.aeg.core.mail.EmailMessage;
import com.aeg.core.mail.EmailSendResult;
import com.aeg.core.mail.EmailSender;
import com.aeg.core.mail.MailProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@Slf4j
public class UserAccountLifecycleService {

    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final String INVALID_INVITE_MESSAGE =
            "Este enlace de invitación es inválido, ya fue utilizado o ha expirado. Solicita al administrador que reenvíe la invitación.";
    private static final String INVALID_RESET_MESSAGE =
            "Este enlace de recuperación es inválido, ya fue utilizado o ha expirado. Solicita uno nuevo.";

    private final UserRepository userRepository;
    private final UserActionTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final AuthEmailTemplateService emailTemplateService;
    private final MailProperties mailProperties;
    private final AuthRateLimiter rateLimiter;
    private final TransactionTemplate writeTx;
    private final TransactionTemplate readOnlyTx;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.invite-expiration-hours:72}")
    private long inviteExpirationHours;

    @Value("${app.auth.password-reset-expiration-minutes:60}")
    private long passwordResetExpirationMinutes;

    public UserAccountLifecycleService(
            UserRepository userRepository,
            UserActionTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EmailSender emailSender,
            AuthEmailTemplateService emailTemplateService,
            MailProperties mailProperties,
            AuthRateLimiter rateLimiter,
            PlatformTransactionManager transactionManager
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.emailTemplateService = emailTemplateService;
        this.mailProperties = mailProperties;
        this.rateLimiter = rateLimiter;
        this.writeTx = new TransactionTemplate(transactionManager);
        this.readOnlyTx = new TransactionTemplate(transactionManager);
        this.readOnlyTx.setReadOnly(true);
        this.readOnlyTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    public String generateUnusableEncodedPassword() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String randomSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return passwordEncoder.encode(randomSecret);
    }

    public IssuedUserToken createInvitedUserWithToken(User user) {
        return writeTx.execute(status -> {
            OffsetDateTime now = OffsetDateTime.now();
            user.setPassword(generateUnusableEncodedPassword());
            user.setEnabled(false);
            user.setInvitationPending(true);
            user.setInvitedAt(now);
            user.setActivatedAt(null);
            User savedUser = userRepository.save(user);

            String rawToken = createToken(savedUser, UserActionTokenPurpose.INVITATION, now,
                    now.plusHours(effectiveInviteExpirationHours()));
            return new IssuedUserToken(savedUser, rawToken);
        });
    }

    public IssuedUserToken reissueInvitationToken(User user) {
        return writeTx.execute(status -> {
            OffsetDateTime now = OffsetDateTime.now();
            tokenRepository.invalidateActiveTokens(user.getId(), UserActionTokenPurpose.INVITATION, now);
            user.setInvitedAt(now);
            User savedUser = userRepository.save(user);

            String rawToken = createToken(savedUser, UserActionTokenPurpose.INVITATION, now,
                    now.plusHours(effectiveInviteExpirationHours()));
            return new IssuedUserToken(savedUser, rawToken);
        });
    }

    public String issuePasswordResetToken(User user) {
        return writeTx.execute(status -> {
            OffsetDateTime now = OffsetDateTime.now();
            tokenRepository.invalidateActiveTokens(user.getId(), UserActionTokenPurpose.PASSWORD_RESET, now);
            return createToken(user, UserActionTokenPurpose.PASSWORD_RESET, now,
                    now.plusMinutes(effectivePasswordResetExpirationMinutes()));
        });
    }

    public void invalidatePendingInvitations(Long userId) {
        if (userId == null) {
            return;
        }
        writeTx.executeWithoutResult(status ->
                tokenRepository.invalidateActiveTokens(userId, UserActionTokenPurpose.INVITATION, OffsetDateTime.now())
        );
    }

    public String buildInvitationUrl(String rawToken) {
        return emailTemplateService.buildInvitationUrl(rawToken);
    }

    public String buildPasswordResetUrl(String rawToken) {
        return emailTemplateService.buildPasswordResetUrl(rawToken);
    }

    public EmailSendResult sendInvitationEmail(User user, String rawToken) {
        EmailMessage message = emailTemplateService.buildInvitationEmail(user, rawToken, effectiveInviteExpirationHours());
        return emailSender.send(message);
    }

    public EmailSendResult sendPasswordResetEmail(User user, String rawToken) {
        EmailMessage message = emailTemplateService.buildPasswordResetEmail(
                user, rawToken, effectivePasswordResetExpirationMinutes());
        return emailSender.send(message);
    }

    public InvitationDetails validateInvitation(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidActionTokenException(INVALID_INVITE_MESSAGE);
        }
        String tokenHash = hashToken(rawToken.trim());
        return readOnlyTx.execute(status -> {
            UserActionToken actionToken = tokenRepository
                    .findByTokenHashAndPurposeWithUser(tokenHash, UserActionTokenPurpose.INVITATION)
                    .orElseThrow(() -> new InvalidActionTokenException(INVALID_INVITE_MESSAGE));

            OffsetDateTime now = OffsetDateTime.now();
            User user = actionToken.getUser();
            if (actionToken.getUsedAt() != null
                    || !actionToken.getExpiresAt().isAfter(now)
                    || !user.isInvitationPending()) {
                throw new InvalidActionTokenException(INVALID_INVITE_MESSAGE);
            }

            return new InvitationDetails(
                    user.getName(),
                    user.getUsername(),
                    user.getRole(),
                    actionToken.getExpiresAt()
            );
        });
    }

    public User acceptInvitation(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidActionTokenException(INVALID_INVITE_MESSAGE);
        }
        validateNewPassword(newPassword);
        String tokenHash = hashToken(rawToken.trim());

        return writeTx.execute(status -> {
            UserActionToken actionToken = tokenRepository
                    .findByTokenHashAndPurposeWithUser(tokenHash, UserActionTokenPurpose.INVITATION)
                    .orElseThrow(() -> new InvalidActionTokenException(INVALID_INVITE_MESSAGE));

            OffsetDateTime now = OffsetDateTime.now();
            User user = actionToken.getUser();
            if (actionToken.getUsedAt() != null
                    || !actionToken.getExpiresAt().isAfter(now)
                    || !user.isInvitationPending()) {
                throw new InvalidActionTokenException(INVALID_INVITE_MESSAGE);
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            user.setEnabled(true);
            user.setInvitationPending(false);
            user.setActivatedAt(now);
            userRepository.save(user);

            actionToken.setUsedAt(now);
            tokenRepository.save(actionToken);
            tokenRepository.invalidateActiveTokens(user.getId(), UserActionTokenPurpose.INVITATION, now);

            return userRepository.findByUsernameWithRelations(user.getUsername()).orElse(user);
        });
    }

    public void requestPasswordReset(String rawEmail, String clientIp) {
        rateLimiter.checkForgotPasswordIp(clientIp);

        String email = normalizeEmail(rawEmail);
        if (email == null) {
            return;
        }

        PasswordResetDispatch dispatch = writeTx.execute(status -> {
            var maybeUser = userRepository.findByUsername(email);
            if (maybeUser.isEmpty()) {
                return null;
            }
            User user = maybeUser.get();
            if (!user.isEnabled() || user.isInvitationPending()) {
                return null;
            }
            if (!rateLimiter.tryConsumeForgotPasswordEmail(email)) {
                return null;
            }
            OffsetDateTime now = OffsetDateTime.now();
            tokenRepository.invalidateActiveTokens(user.getId(), UserActionTokenPurpose.PASSWORD_RESET, now);
            String rawToken = createToken(user, UserActionTokenPurpose.PASSWORD_RESET, now,
                    now.plusMinutes(effectivePasswordResetExpirationMinutes()));
            return new PasswordResetDispatch(user, rawToken);
        });

        if (dispatch == null) {
            return;
        }

        if (mailProperties.isAsyncPasswordReset()) {
            Thread.ofVirtual().start(() -> {
                try {
                    sendPasswordResetEmail(dispatch.user(), dispatch.rawToken());
                } catch (Exception e) {
                    log.error("Error enviando correo de restablecimiento a {}", dispatch.user().getUsername(), e);
                }
            });
        } else {
            try {
                sendPasswordResetEmail(dispatch.user(), dispatch.rawToken());
            } catch (EmailDeliveryException e) {
                log.error("Error enviando correo de restablecimiento a {}", dispatch.user().getUsername(), e);
            }
        }
    }

    public PasswordResetDetails validatePasswordReset(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidActionTokenException(INVALID_RESET_MESSAGE);
        }
        String tokenHash = hashToken(rawToken.trim());
        return readOnlyTx.execute(status -> {
            UserActionToken actionToken = tokenRepository
                    .findByTokenHashAndPurposeWithUser(tokenHash, UserActionTokenPurpose.PASSWORD_RESET)
                    .orElseThrow(() -> new InvalidActionTokenException(INVALID_RESET_MESSAGE));

            OffsetDateTime now = OffsetDateTime.now();
            User user = actionToken.getUser();
            if (actionToken.getUsedAt() != null
                    || !actionToken.getExpiresAt().isAfter(now)
                    || !user.isEnabled()
                    || user.isInvitationPending()) {
                throw new InvalidActionTokenException(INVALID_RESET_MESSAGE);
            }

            return new PasswordResetDetails(user.getUsername(), actionToken.getExpiresAt());
        });
    }

    public void resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidActionTokenException(INVALID_RESET_MESSAGE);
        }
        validateNewPassword(newPassword);
        String tokenHash = hashToken(rawToken.trim());

        writeTx.executeWithoutResult(status -> {
            UserActionToken actionToken = tokenRepository
                    .findByTokenHashAndPurposeWithUser(tokenHash, UserActionTokenPurpose.PASSWORD_RESET)
                    .orElseThrow(() -> new InvalidActionTokenException(INVALID_RESET_MESSAGE));

            OffsetDateTime now = OffsetDateTime.now();
            User user = actionToken.getUser();
            if (actionToken.getUsedAt() != null
                    || !actionToken.getExpiresAt().isAfter(now)
                    || !user.isEnabled()
                    || user.isInvitationPending()) {
                throw new InvalidActionTokenException(INVALID_RESET_MESSAGE);
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            actionToken.setUsedAt(now);
            tokenRepository.save(actionToken);
            tokenRepository.invalidateActiveTokens(user.getId(), UserActionTokenPurpose.PASSWORD_RESET, now);
        });
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpiredTokens() {
        writeTx.executeWithoutResult(status -> {
            int deleted = tokenRepository.deleteExpiredBefore(OffsetDateTime.now().minusDays(7));
            if (deleted > 0) {
                log.info("Purgados {} tokens expirados de user_action_tokens.", deleted);
            }
        });
    }

    private String createToken(
            User user,
            UserActionTokenPurpose purpose,
            OffsetDateTime createdAt,
            OffsetDateTime expiresAt
    ) {
        byte[] rawBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(rawBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes);
        String tokenHash = hashToken(rawToken);

        UserActionToken entity = UserActionToken.builder()
                .user(userRepository.getReferenceById(user.getId()))
                .tokenHash(tokenHash)
                .purpose(purpose)
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .build();
        tokenRepository.save(entity);
        return rawToken;
    }

    private long effectiveInviteExpirationHours() {
        return inviteExpirationHours > 0 ? inviteExpirationHours : 72L;
    }

    private long effectivePasswordResetExpirationMinutes() {
        return passwordResetExpirationMinutes > 0 ? passwordResetExpirationMinutes : 60L;
    }

    private static void validateNewPassword(String newPassword) {
        if (newPassword == null
                || newPassword.length() < MIN_PASSWORD_LENGTH
                || newPassword.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener entre " + MIN_PASSWORD_LENGTH + " y " + MAX_PASSWORD_LENGTH + " caracteres."
            );
        }
    }

    private static String normalizeEmail(String rawEmail) {
        if (rawEmail == null) {
            return null;
        }
        String normalized = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || !normalized.contains("@")) {
            return null;
        }
        return normalized;
    }

    static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public record IssuedUserToken(User user, String rawToken) {
    }

    public record InvitationDetails(String name, String email, Role role, OffsetDateTime expiresAt) {
    }

    public record PasswordResetDetails(String email, OffsetDateTime expiresAt) {
    }

    private record PasswordResetDispatch(User user, String rawToken) {
    }
}
