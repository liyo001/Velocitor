package com.velocitor.support;

import io.restassured.RestAssured;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Small, explicit configuration boundary so tests do not bake in an environment. */
public final class TestConfig {
    private TestConfig() {
    }

    public static void configureRestAssured() {
        String sysProp = System.getProperty("base.url", System.getProperty("BASE_URL"));
        String envVar = System.getenv("BASE_URL");
        if (envVar == null) {
            envVar = System.getenv("base.url");
        }

        String baseUrl = (sysProp != null && !sysProp.isBlank()) ? sysProp
                : (envVar != null && !envVar.isBlank() ? envVar : readDefaultBaseUrl());

        RestAssured.baseURI = baseUrl;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    public static String getBaseUrl() {
        if (RestAssured.baseURI == null || RestAssured.baseURI.isBlank()) {
            configureRestAssured();
        }
        return RestAssured.baseURI;
    }

    private static String readDefaultBaseUrl() {
        Properties properties = new Properties();
        try (InputStream stream = TestConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (stream == null) {
                return "https://restful-booker.herokuapp.com";
            }
            properties.load(stream);
            return properties.getProperty("base.url", "https://restful-booker.herokuapp.com");
        } catch (IOException exception) {
            return "https://restful-booker.herokuapp.com";
        }
    }
}
