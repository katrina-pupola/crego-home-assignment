package com.crego.backend.paymentmethod;

import com.crego.backend.common.CardNumbers;
import com.crego.backend.common.Ibans;

import java.util.Objects;

/** Payment method details in the form they are stored: last 4 digits only, normalized IBAN. */
public record MethodData(
        PaymentMethodType type,
        String cardLast4,
        CardBrand cardBrand,
        String cardHolder,
        String cardExpiry,
        String iban
) {

    /** Expects a request that has already passed validation. */
    public static MethodData from(PaymentMethodRequest request) {
        return switch (request.type()) {
            case CARD -> {
                String number = CardNumbers.normalize(request.cardNumber());
                yield new MethodData(PaymentMethodType.CARD, number.substring(number.length() - 4),
                        CardBrand.detect(number), request.cardHolder().trim(), request.cardExpiry(), null);
            }
            case BANK_ACCOUNT -> new MethodData(PaymentMethodType.BANK_ACCOUNT, null, null, null, null,
                    Ibans.normalize(request.iban()));
        };
    }

    /** True if both describe the same method (card holder compared case-insensitively). */
    public boolean isSameAs(MethodData other) {
        return type == other.type
                && Objects.equals(cardLast4, other.cardLast4)
                && cardBrand == other.cardBrand
                && Objects.equals(cardExpiry, other.cardExpiry)
                && (cardHolder == null ? other.cardHolder == null : cardHolder.equalsIgnoreCase(other.cardHolder))
                && Objects.equals(iban, other.iban);
    }
}
