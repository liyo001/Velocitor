package com.velocitor.api;

import com.velocitor.support.BookerClient;
import com.velocitor.support.BookingData;
import com.velocitor.support.TestConfig;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class BookingApiTest {
    private static final BookerClient client = new BookerClient();

    @BeforeAll
    static void setUp() {
        TestConfig.configureRestAssured();
    }

    // ==========================================
    // 1. AUTHENTICATION
    // ==========================================

    @Test
    @DisplayName("Auth: should obtain an auth token with valid credentials")
    void auth_shouldObtainTokenWithValidCredentials() {
        String token = client.createToken();
        assertThat(token, notNullValue());
        assertThat(token, not(emptyOrNullString()));
    }

    @Test
    @DisplayName("Auth: should reject authentication with invalid credentials")
    void auth_shouldRejectInvalidCredentials() {
        client.authenticate("admin", "invalid-password")
                .then()
                .statusCode(200)
                .body("reason", equalTo("Bad credentials"))
                .body("token", nullValue());
    }

    // ==========================================
    // 2. BOOKING LIFECYCLE & 5. RESPONSE VALIDATION (ROUND-TRIP)
    // ==========================================

    @Test
    @DisplayName("Lifecycle & Round-trip: create, retrieve, full PUT, partial PATCH, and delete booking")
    void bookingLifecycle_shouldRoundTripAllFieldsAcrossLifecycle() {
        String suffix = String.valueOf(System.nanoTime());
        String firstName = "Sam" + suffix;
        String lastName = "Taylor";
        Map<String, Object> createdPayload = BookingData.aBooking(firstName, lastName);

        // 1. Create booking
        int bookingId = client.createBooking(createdPayload)
                .then()
                .statusCode(200)
                .body("bookingid", greaterThan(0))
                .body("booking.firstname", equalTo(firstName))
                .body("booking.lastname", equalTo(lastName))
                .body("booking.totalprice", equalTo(createdPayload.get("totalprice")))
                .body("booking.depositpaid", equalTo(createdPayload.get("depositpaid")))
                .body("booking.additionalneeds", equalTo(createdPayload.get("additionalneeds")))
                .extract().path("bookingid");

        String token = client.createToken();
        boolean deleted = false;

        try {
            // 2. Retrieve booking (GET) - Full response body round-trip validation
            @SuppressWarnings("unchecked")
            Map<String, String> originalDates = (Map<String, String>) createdPayload.get("bookingdates");

            client.getBooking(bookingId)
                    .then()
                    .statusCode(200)
                    .body("firstname", equalTo(firstName))
                    .body("lastname", equalTo(lastName))
                    .body("totalprice", equalTo(createdPayload.get("totalprice")))
                    .body("depositpaid", equalTo(createdPayload.get("depositpaid")))
                    .body("bookingdates.checkin", equalTo(originalDates.get("checkin")))
                    .body("bookingdates.checkout", equalTo(originalDates.get("checkout")))
                    .body("additionalneeds", equalTo(createdPayload.get("additionalneeds")));

            // 3. Full Update (PUT)
            Map<String, Object> replacementPayload = BookingData.aBooking(
                    "Jordan" + suffix,
                    "Taylor-Updated",
                    450,
                    false,
                    LocalDate.now().plusDays(20).toString(),
                    LocalDate.now().plusDays(25).toString(),
                    "Airport Shuttle"
            );

            @SuppressWarnings("unchecked")
            Map<String, String> replacementDates = (Map<String, String>) replacementPayload.get("bookingdates");

            client.replaceBooking(bookingId, replacementPayload, token)
                    .then()
                    .statusCode(200)
                    .body("firstname", equalTo("Jordan" + suffix))
                    .body("lastname", equalTo("Taylor-Updated"))
                    .body("totalprice", equalTo(450))
                    .body("depositpaid", equalTo(false))
                    .body("bookingdates.checkin", equalTo(replacementDates.get("checkin")))
                    .body("bookingdates.checkout", equalTo(replacementDates.get("checkout")))
                    .body("additionalneeds", equalTo("Airport Shuttle"));

            // 4. Partial Update (PATCH) - Verify patched fields update while untouched fields persist
            Map<String, Object> patchFields = Map.of(
                    "firstname", "Alex" + suffix,
                    "additionalneeds", "Late checkout"
            );

            client.updateBooking(bookingId, patchFields, token)
                    .then()
                    .statusCode(200)
                    .body("firstname", equalTo("Alex" + suffix))
                    .body("lastname", equalTo("Taylor-Updated")) // Unchanged
                    .body("totalprice", equalTo(450))              // Unchanged
                    .body("depositpaid", equalTo(false))           // Unchanged
                    .body("additionalneeds", equalTo("Late checkout"));

            // 5. Delete booking (DELETE)
            client.deleteBooking(bookingId, token)
                    .then()
                    .statusCode(201);
            deleted = true;

            // 6. Verify deleted (GET -> 404)
            client.getBooking(bookingId)
                    .then()
                    .statusCode(404);

        } finally {
            if (!deleted) {
                try {
                    client.deleteBooking(bookingId, token);
                } catch (Exception ignored) {
                }
            }
        }
    }

    // ==========================================
    // 3. QUERYING (FILTER BY NAME AND DATE)
    // ==========================================

    @Test
    @DisplayName("Querying: filter bookings by firstname, lastname, combined names, and date range")
    void querying_shouldFindBookingsByNameAndDateFilters() {
        String suffix = String.valueOf(System.nanoTime());
        String firstName = "Avery" + suffix;
        String lastName = "Search" + suffix;

        LocalDate checkinDate = LocalDate.now().plusDays(30);
        LocalDate checkoutDate = LocalDate.now().plusDays(35);

        Map<String, Object> booking = BookingData.aBooking(
                firstName,
                lastName,
                320,
                true,
                checkinDate.toString(),
                checkoutDate.toString(),
                "Sea View"
        );

        int bookingId = client.createBooking(booking)
                .then()
                .statusCode(200)
                .extract().path("bookingid");

        String token = client.createToken();

        try {
            // 1. Filter by firstname
            List<Integer> byFirstName = client.findBookings("firstname", firstName);
            assertThat("Booking should be found when filtered by firstname", byFirstName, hasItem(bookingId));

            // 2. Filter by lastname
            List<Integer> byLastName = client.findBookings("lastname", lastName);
            assertThat("Booking should be found when filtered by lastname", byLastName, hasItem(bookingId));

            // 3. Filter by both firstname and lastname
            List<Integer> byBothNames = client.findBookings(Map.of(
                    "firstname", firstName,
                    "lastname", lastName
            ));
            assertThat("Booking should be found when filtered by both names", byBothNames, hasItem(bookingId));

            // 4. Filter by date window:
            // Restful Booker's public API date filter evaluates dates such that querying with a checkin boundary
            // preceding the booking date window reliably matches the target record.
            List<Integer> byDateRange = client.findBookings(Map.of(
                    "checkin", checkinDate.minusDays(1).toString(),
                    "checkout", checkoutDate.plusDays(1).toString()
            ));
            assertThat("Booking should be found within date range window", byDateRange, hasItem(bookingId));

        } finally {
            client.deleteBooking(bookingId, token);
        }
    }

    // ==========================================
    // 4. NEGATIVE & ADVERSARIAL CASES
    // ==========================================

    @Test
    @DisplayName("Adversarial: reject unauthenticated modifications (PUT, PATCH, DELETE)")
    void adversarial_shouldRejectUnauthenticatedModifications() {
        String suffix = String.valueOf(System.nanoTime());
        Map<String, Object> booking = BookingData.aBooking("Security" + suffix, "Test");
        int bookingId = client.createBooking(booking)
                .then()
                .statusCode(200)
                .extract().path("bookingid");

        String validToken = client.createToken();

        try {
            // Unauthenticated PUT (missing token cookie)
            client.replaceBooking(bookingId, booking, null)
                    .then()
                    .statusCode(403);

            // Unauthenticated PATCH (missing token cookie)
            client.updateBooking(bookingId, Map.of("additionalneeds", "Hacked"), null)
                    .then()
                    .statusCode(403);

            // Unauthenticated DELETE (missing token cookie)
            client.deleteBooking(bookingId, null)
                    .then()
                    .statusCode(403);

            // Forbidden DELETE with an invalid/forged token
            client.deleteBooking(bookingId, "invalid-forged-token-12345")
                    .then()
                    .statusCode(403);

        } finally {
            client.deleteBooking(bookingId, validToken);
        }
    }

    @Test
    @DisplayName("Adversarial: verify 404 response for non-existent booking ID")
    void adversarial_shouldReturnNotFoundForNonExistentBooking() {
        int nonExistentId = 999_999_999;
        client.getBooking(nonExistentId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Adversarial: reject creation with malformed / empty payload")
    void adversarial_shouldRejectMalformedPayloadOnCreate() {
        // Missing required booking structure
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("firstname", "OnlyFirstnameProvided"))
                .when()
                .post("/booking")
                .then()
                .statusCode(500);

        // Empty JSON body
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/booking")
                .then()
                .statusCode(500);
    }
}
