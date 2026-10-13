package com.velocitor.support;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.Map;
import java.util.List;

import static io.restassured.RestAssured.given;

/** Keeps endpoint mechanics out of test intent without hiding Rest Assured entirely. */
public final class BookerClient {
    public Response authenticate(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/auth");
    }

    public String createToken() {
        return authenticate("admin", "password123")
                .then().statusCode(200)
                .extract().path("token");
    }

    public Response createBooking(Map<String, Object> booking) {
        return given().contentType(ContentType.JSON).body(booking).when().post("/booking");
    }

    public Response getBooking(int id) {
        return given().when().get("/booking/{id}", id);
    }

    public List<Integer> findBookings(String parameter, String value) {
        return given().queryParam(parameter, value)
                .when().get("/booking")
                .then().statusCode(200)
                .extract().path("bookingid");
    }

    public List<Integer> findBookings(Map<String, ?> queryParams) {
        return given().queryParams(queryParams)
                .when().get("/booking")
                .then().statusCode(200)
                .extract().path("bookingid");
    }

    public Response replaceBooking(int id, Map<String, Object> booking, String token) {
        var spec = given().contentType(ContentType.JSON).body(booking);
        if (token != null) {
            spec.cookie("token", token);
        }
        return spec.when().put("/booking/{id}", id);
    }

    public Response updateBooking(int id, Map<String, Object> fields, String token) {
        var spec = given().contentType(ContentType.JSON).body(fields);
        if (token != null) {
            spec.cookie("token", token);
        }
        return spec.when().patch("/booking/{id}", id);
    }

    public Response deleteBooking(int id, String token) {
        var spec = given();
        if (token != null) {
            spec.cookie("token", token);
        }
        return spec.when().delete("/booking/{id}", id);
    }
}
