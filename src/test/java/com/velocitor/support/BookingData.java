package com.velocitor.support;

import java.time.LocalDate;
import java.util.Map;

public final class BookingData {
    private BookingData() {
    }

    public static Map<String, Object> aBooking(String firstName, String lastName) {
        return aBooking(
                firstName,
                lastName,
                275,
                true,
                LocalDate.now().plusDays(14).toString(),
                LocalDate.now().plusDays(18).toString(),
                "Breakfast"
        );
    }

    public static Map<String, Object> aBooking(
            String firstName,
            String lastName,
            int totalPrice,
            boolean depositPaid,
            String checkin,
            String checkout,
            String additionalNeeds) {
        return Map.of(
                "firstname", firstName,
                "lastname", lastName,
                "totalprice", totalPrice,
                "depositpaid", depositPaid,
                "bookingdates", Map.of(
                        "checkin", checkin,
                        "checkout", checkout
                ),
                "additionalneeds", additionalNeeds
        );
    }
}
