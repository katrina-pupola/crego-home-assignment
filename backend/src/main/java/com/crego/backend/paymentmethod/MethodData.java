package com.crego.backend.paymentmethod;

import com.crego.backend.common.CardNumbers;
import com.crego.backend.common.Ibans;

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
}
