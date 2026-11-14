package com.crego.backend;

import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Keeps emails in memory instead of sending them, so tests can read them. */
public class RecordingMailSender extends JavaMailSenderImpl {

    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    public record SentEmail(List<String> to, String subject, String text, String html) {
    }

    private final List<SentEmail> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(MimeMessage... messages) {
        for (MimeMessage message : messages) {
            try {
                message.saveChanges(); // sets the content-type headers, as a real send would
                List<String> to = Arrays.stream(message.getRecipients(Message.RecipientType.TO))
                        .map(address -> ((InternetAddress) address).getAddress())
                        .toList();
                sent.add(new SentEmail(to, message.getSubject(),
                        findPart(message, "text/plain"), findPart(message, "text/html")));
            } catch (Exception e) {
                throw new IllegalStateException("Could not read test email", e);
            }
        }
    }

    public List<SentEmail> sentTo(String email) {
        return sent.stream().filter(m -> m.to().contains(email)).toList();
    }

    public Optional<SentEmail> lastSentTo(String email) {
        List<SentEmail> messages = sentTo(email);
        return messages.isEmpty() ? Optional.empty() : Optional.of(messages.getLast());
    }

    /** The token from the verification link in the latest email to this address. */
    public String verificationToken(String email) {
        String text = lastSentTo(email).map(SentEmail::text).orElseThrow();
        Matcher matcher = TOKEN.matcher(text);
        if (!matcher.find()) {
            throw new AssertionError("No verification link in email to " + email);
        }
        return matcher.group(1);
    }

    // Walks the multipart structure (mixed > related > alternative) to find the text or HTML part
    private static String findPart(Part part, String mimeType) throws Exception {
        if (part.isMimeType(mimeType)) {
            return (String) part.getContent();
        }
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart child = multipart.getBodyPart(i);
                String found = findPart(child, mimeType);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
