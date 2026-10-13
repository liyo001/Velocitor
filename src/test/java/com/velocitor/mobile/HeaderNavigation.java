package com.velocitor.mobile;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

/**
 * Page object representing the persistent top app header bar.
 */
public final class HeaderNavigation extends BasePage {
    private final By menuButton = AppiumBy.accessibilityId("open menu");
    private final By cartButton = AppiumBy.accessibilityId("cart badge");

    public HeaderNavigation(AndroidDriver driver) {
        super(driver);
    }

    public NavigationMenu openMenu() {
        click(menuButton);
        return new NavigationMenu(driver);
    }

    public CartPage openCart() {
        click(cartButton);
        return new CartPage(driver);
    }
}
