package com.velocitor.mobile;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

/**
 * Page object representing the Product Details Screen.
 */
public final class ProductDetailsPage extends BasePage {
    private final By addToCartButton = AppiumBy.accessibilityId("Add To Cart button");

    public ProductDetailsPage(AndroidDriver driver) {
        super(driver);
    }

    public void addToCart() {
        click(addToCartButton);
    }
}
