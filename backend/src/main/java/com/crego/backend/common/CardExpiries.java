package com.crego.backend.common;

import java.time.YearMonth;

public final class CardExpiries {

    private CardExpiries() {
    }

    /** "MM/YY", valid through the end of that month. */
    public static boolean isValid(String expiry, YearMonth now) {
        if (expiry == null || !expiry.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            return false;
        }
        int month = Integer.parseInt(expiry.substring(0, 2));
        int year = 2000 + Integer.parseInt(expiry.substring(3));
        return !YearMonth.of(year, month).isBefore(now);
    }
}
