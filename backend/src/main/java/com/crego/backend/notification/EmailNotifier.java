package com.crego.backend.notification;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends emails only after the surrounding transaction has committed, so a mail server problem
 * can never undo a registration or a decision. Sending happens on a background thread, so the
 * HTTP response never waits for the mail server. Failures are logged; the client can request a new link.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotifier {

    private static final String SENDER_NAME = "Payment Methods";

    private final JavaMailSender mailSender;
    private final AppMailProperties props;

    @Async
    @TransactionalEventListener
    public void onVerificationRequested(VerificationEmailRequested event) {
        String link = props.clientAppUrl() + "/verify?token=" + event.token();
        send(event.email(), EmailTemplates.verification(event.fullName(), link, props.verificationTtl().toHours()));
    }

    @Async
    @TransactionalEventListener
    public void onChangeRequestDecided(ChangeRequestDecided event) {
        send(event.email(), EmailTemplates.decision(event.fullName(), event.approved(), event.comment(),
                props.clientAppUrl()));
    }

    // HTML for email clients that show it, plain text for the rest
    private void send(String to, EmailTemplates.Email email) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(props.from(), SENDER_NAME);
            helper.setTo(to);
            helper.setSubject(email.subject());
            helper.setText(email.text(), email.html());
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Could not send email '{}': {}", email.subject(), e.getMessage());
        }
    }
}
