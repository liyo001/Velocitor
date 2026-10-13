package com.velocitor.mobile;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

/**
 * Page object representing the Checkout Screen (Shipping Address & Navigation to Payment).
 */
public final class CheckoutPage extends BasePage {
    private final By fullNameInput = AppiumBy.accessibilityId("Full Name* input field");
    private final By addressLine1Input = AppiumBy.accessibilityId("Address Line 1* input field");
    private final By cityInput = AppiumBy.accessibilityId("City* input field");
    private final By stateRegionInput = AppiumBy.accessibilityId("State/Region input field");
    private final By zipCodeInput = AppiumBy.accessibilityId("Zip Code* input field");
    private final By countryInput = AppiumBy.accessibilityId("Country* input field");
    private final By toPaymentButton = AppiumBy.accessibilityId("To Payment button");
    private final By cardNumberInput = AppiumBy.accessibilityId("Card Number* input field");

    public CheckoutPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isShippingAddressFormDisplayed() {
        return isDisplayed(fullNameInput);
    }

    public void fillShippingAddress(
            String fullName,
            String address1,
            String city,
            String state,
            String zip,
            String country) {
        type(fullNameInput, fullName);
        type(addressLine1Input, address1);
        type(cityInput, city);
        type(stateRegionInput, state);
        type(zipCodeInput, zip);
        type(countryInput, country);
    }

    public void clickToPayment() {
        click(toPaymentButton);
    }

    public boolean isPaymentMethodScreenDisplayed() {
        return isDisplayed(cardNumberInput);
    }
}
