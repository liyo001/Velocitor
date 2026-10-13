package com.velocitor.mobile;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

/**
 * Page object representing the Shopping Cart Screen.
 */
public final class CartPage extends BasePage {
    private final By proceedToCheckoutButton = AppiumBy.accessibilityId("Proceed To Checkout button");

    public CartPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isItemInCart(String productName) {
        return isDisplayed(AppiumBy.accessibilityId(productName));
    }

    public boolean isProceedToCheckoutButtonDisplayed() {
        return isDisplayed(proceedToCheckoutButton);
    }

    public CheckoutPage proceedToCheckout() {
        click(proceedToCheckoutButton);
        return new CheckoutPage(driver);
    }
}
