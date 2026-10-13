package com.velocitor.mobile;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

/**
 * Page object representing the Products Catalog Screen.
 */
public final class CatalogPage extends BasePage {
    private final By productsScreen = AppiumBy.accessibilityId("Products screen");

    public CatalogPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isCatalogDisplayed() {
        return isDisplayed(productsScreen);
    }

    public ProductDetailsPage selectProduct(String productName) {
        click(AppiumBy.accessibilityId(productName));
        return new ProductDetailsPage(driver);
    }
}
