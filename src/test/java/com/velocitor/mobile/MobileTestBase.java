package com.velocitor.mobile;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.net.MalformedURLException;
import java.net.URI;

abstract class MobileTestBase {
    protected AndroidDriver driver;

    @BeforeEach
    void startDriver() throws MalformedURLException {
        String appPath = required("DEMO_APP_PATH");
        UiAutomator2Options options = new UiAutomator2Options()
                .setDeviceName(System.getenv().getOrDefault("ANDROID_DEVICE", "Android Emulator"))
                .setApp(appPath)
                .setAutoGrantPermissions(true);
        String platformVersion = System.getenv("ANDROID_VERSION");
        if (platformVersion != null && !platformVersion.isBlank()) {
            options.setPlatformVersion(platformVersion);
        }
        driver = new AndroidDriver(URI.create(System.getenv().getOrDefault("APPIUM_URL", "http://127.0.0.1:4723")).toURL(), options);
    }

    @AfterEach
    void stopDriver() {
        if (driver != null) {
            driver.quit();
        }
    }

    private String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must point to the My Demo App APK");
        }
        return value;
    }
}
