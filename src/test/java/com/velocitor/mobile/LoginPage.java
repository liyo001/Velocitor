package com.velocitor.mobile;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

/**
 * Page object for the Login Screen in Sauce Labs My Demo App.
 */
public final class LoginPage extends BasePage {
    private final By usernameInput = AppiumBy.accessibilityId("Username input field");
    private final By passwordInput = AppiumBy.accessibilityId("Password input field");
    private final By loginButton = AppiumBy.accessibilityId("Login button");
    private final By errorMessage = AppiumBy.accessibilityId("Username and password do not match any user in this service.");

    public LoginPage(AndroidDriver driver) {
        super(driver);
    }

    public void enterUsername(String user) {
        type(usernameInput, user);
    }

    public void enterPassword(String pass) {
        type(passwordInput, pass);
    }

    public void clickLogin() {
        click(loginButton);
    }

    public void signIn(String user, String pass) {
        enterUsername(user);
        enterPassword(pass);
        clickLogin();
    }

    public boolean isErrorMessageDisplayed() {
        return isDisplayed(errorMessage);
    }

    public String getErrorMessageText() {
        return getText(errorMessage);
    }
}
