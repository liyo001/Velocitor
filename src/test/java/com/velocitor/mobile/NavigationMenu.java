package com.velocitor.mobile;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

/**
 * Page object representing the side drawer menu in Sauce Labs My Demo App.
 */
public final class NavigationMenu extends BasePage {
    private final By loginMenuItem = AppiumBy.accessibilityId("menu item log in");
    private final By catalogMenuItem = AppiumBy.accessibilityId("menu item catalog");
    private final By logoutMenuItem = AppiumBy.accessibilityId("menu item log out");

    public NavigationMenu(AndroidDriver driver) {
        super(driver);
    }

    public LoginPage navigateToLogin() {
        click(loginMenuItem);
        return new LoginPage(driver);
    }

    public CatalogPage navigateToCatalog() {
        click(catalogMenuItem);
        return new CatalogPage(driver);
    }

    public void logOut() {
        click(logoutMenuItem);
    }

    public boolean isLogoutOptionVisible() {
        return isDisplayed(logoutMenuItem);
    }
}
