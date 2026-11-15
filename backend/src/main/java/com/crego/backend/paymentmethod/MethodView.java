package com.crego.backend.paymentmethod;

import com.crego.backend.common.Ibans;

/** Payment method as shown in API responses, with the number masked. */
public record MethodView(
        PaymentMethodType type,
        CardBrand cardBrand,
        String cardHolder,
        String cardExpiry,
        String maskedNumber
) {

    public static MethodView from(MethodData data) {
        String masked = data.type() == PaymentMethodType.CARD
                ? "**** " + data.cardLast4()
                : Ibans.mask(data.iban());
        return new MethodView(data.type(), data.cardBrand(), data.cardHolder(), data.cardExpiry(), masked);
    }
}
