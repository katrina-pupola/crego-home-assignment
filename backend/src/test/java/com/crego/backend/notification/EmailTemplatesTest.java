package com.crego.backend.notification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplatesTest {

    @Test
    void verificationEmailHasButtonAndPlainTextFallback() {
        EmailTemplates.Email email = EmailTemplates.verification("Anna Ozola", "http://app/verify?token=abc", 24);

        assertThat(email.html()).contains("href=\"http://app/verify?token=abc\"").contains("Confirm email address");
        assertThat(email.text()).contains("http://app/verify?token=abc").doesNotContain("<");
    }

    @Test
    void userInputIsEscapedInHtml() {
        EmailTemplates.Email email = EmailTemplates.decision("<script>alert(1)</script>", false,
                "Name <b>does not</b> match", "http://app");

        assertThat(email.html())
                .doesNotContain("<script>")
                .contains("&lt;script&gt;")
                .contains("Name &lt;b&gt;does not&lt;/b&gt; match");
    }

    @Test
    void decisionEmailShowsOutcomeAndReason() {
        EmailTemplates.Email rejected = EmailTemplates.decision("Anna", false, "IBAN owner does not match", "http://app");

        assertThat(rejected.subject()).isEqualTo("Your payment method change was rejected");
        assertThat(rejected.html()).contains("Rejected").contains("IBAN owner does not match");
        assertThat(rejected.text()).contains("Reason: IBAN owner does not match");
    }
}
