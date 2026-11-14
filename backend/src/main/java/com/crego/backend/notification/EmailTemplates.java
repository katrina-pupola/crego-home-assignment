package com.crego.backend.notification;

import org.springframework.web.util.HtmlUtils;

/**
 * Builds the HTML and plain-text versions of each email. Email clients ignore most modern CSS,
 * so the HTML uses tables and inline styles; user-provided values are always HTML-escaped.
 */
final class EmailTemplates {

    private static final String BRAND = "#1d4ed8";
    private static final String TEXT = "#1f2937";
    private static final String MUTED = "#6b7280";
    private static final String FONT = "Arial, Helvetica, sans-serif";

    record Email(String subject, String text, String html) {
    }

    private EmailTemplates() {
    }

    static Email verification(String fullName, String link, long validHours) {
        String subject = "Confirm your email address";
        String text = """
                Hello %s,

                Please confirm your email address to finish creating your Payment Methods account:

                %s

                The link is valid for %d hours and can be used once.
                If you did not create an account, you can ignore this email.
                """.formatted(fullName, link, validHours);
        String body = heading("Confirm your email address")
                + paragraph("Hello " + escape(fullName) + ",")
                + paragraph("Please confirm your email address to finish creating your Payment Methods account.")
                + button(link, "Confirm email address")
                + small("The link is valid for " + validHours + " hours and can be used once.")
                + fallbackLink(link);
        return new Email(subject, text,
                layout(subject, body, "If you did not create an account, you can safely ignore this email."));
    }

    static Email decision(String fullName, boolean approved, String comment, String dashboardUrl) {
        String subject = approved ? "Your payment method change was approved" : "Your payment method change was rejected";
        String outcome = approved
                ? "has been approved. Your new payment method is now active."
                : "has been rejected. Your current payment method stays active.";
        String label = approved ? "Comment" : "Reason";

        String text = """
                Hello %s,

                Your request to change your payment method %s
                %s
                You can see the details at %s
                """.formatted(fullName, outcome,
                comment == null ? "" : "%n%s: %s%n".formatted(label, comment), dashboardUrl);

        String body = statusBadge(approved)
                + heading(approved ? "Your change was approved" : "Your change was rejected")
                + paragraph("Hello " + escape(fullName) + ",")
                + paragraph("Your request to change your payment method " + outcome)
                + (comment == null ? "" : quote(label, comment))
                + button(dashboardUrl, "Open my dashboard");
        return new Email(subject, text, layout(subject, body, "You receive this email because you requested a change "
                + "to your payment method."));
    }

    // Grey page, white card, brand header and a short footer
    private static String layout(String title, String body, String footer) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>%s</title></head>
                <body style="margin:0;padding:0;background:#f5f6f8;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f5f6f8;padding:32px 12px;">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:560px;background:#ffffff;border:1px solid #e5e7eb;border-radius:12px;font-family:%s;color:%s;">
                        <tr><td style="background:%s;border-radius:12px 12px 0 0;padding:18px 28px;">
                          <span style="display:inline-block;width:32px;height:32px;line-height:32px;text-align:center;border-radius:8px;background:#ffffff;color:%s;font-weight:700;font-size:13px;">PM</span>
                          <span style="color:#ffffff;font-size:17px;font-weight:700;vertical-align:middle;margin-left:10px;">Payment Methods</span>
                        </td></tr>
                        <tr><td style="padding:32px 28px 8px;">%s</td></tr>
                        <tr><td style="padding:16px 28px 28px;border-top:1px solid #e5e7eb;font-size:12px;line-height:18px;color:%s;">%s</td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(escape(title), FONT, TEXT, BRAND, BRAND, body, MUTED, escape(footer));
    }

    private static String heading(String text) {
        return "<h1 style=\"margin:0 0 16px;font-size:22px;line-height:28px;color:%s;\">%s</h1>"
                .formatted(TEXT, escape(text));
    }

    private static String paragraph(String html) {
        return "<p style=\"margin:0 0 14px;font-size:15px;line-height:23px;\">%s</p>".formatted(html);
    }

    private static String small(String text) {
        return "<p style=\"margin:0 0 14px;font-size:13px;line-height:20px;color:%s;\">%s</p>"
                .formatted(MUTED, escape(text));
    }

    // A table cell with a background colour renders as a button in nearly every email client
    private static String button(String url, String label) {
        return """
                <table role="presentation" cellpadding="0" cellspacing="0" style="margin:8px 0 22px;">
                  <tr><td style="background:%s;border-radius:8px;">
                    <a href="%s" style="display:inline-block;padding:13px 26px;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none;">%s</a>
                  </td></tr>
                </table>
                """.formatted(BRAND, escape(url), escape(label));
    }

    private static String fallbackLink(String url) {
        return "<p style=\"margin:0 0 14px;font-size:12px;line-height:18px;color:%s;\">Button not working? "
                .formatted(MUTED)
                + "Copy this link into your browser:<br><a href=\"%s\" style=\"color:%s;word-break:break-all;\">%s</a></p>"
                .formatted(escape(url), BRAND, escape(url));
    }

    private static String statusBadge(boolean approved) {
        String background = approved ? "#ecfdf5" : "#fef2f2";
        String color = approved ? "#047857" : "#b91c1c";
        return "<span style=\"display:inline-block;margin:0 0 14px;padding:4px 12px;border-radius:999px;"
                + "background:%s;color:%s;font-size:13px;font-weight:700;\">%s</span>"
                .formatted(background, color, approved ? "&#10003; Approved" : "&#10005; Rejected");
    }

    private static String quote(String label, String text) {
        return """
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin:4px 0 20px;">
                  <tr><td style="background:#f5f6f8;border-left:3px solid %s;border-radius:0 8px 8px 0;padding:12px 16px;">
                    <div style="font-size:11px;font-weight:700;letter-spacing:0.06em;text-transform:uppercase;color:%s;margin-bottom:4px;">%s</div>
                    <div style="font-size:15px;line-height:22px;color:%s;">%s</div>
                  </td></tr>
                </table>
                """.formatted(BRAND, MUTED, escape(label), TEXT, escape(text));
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(value);
    }
}
