package com.crego.backend.paymentmethod;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Checks the fields required by the chosen type: card details for CARD, IBAN for BANK_ACCOUNT. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PaymentMethodRequestValidator.class)
public @interface ValidPaymentMethod {

    String message() default "Invalid payment method";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
