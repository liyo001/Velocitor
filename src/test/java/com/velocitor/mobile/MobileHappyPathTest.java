package com.velocitor.mobile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Representative mobile smoke tests covering Sauce Labs "My Demo App" (Android).
 * Deliberately tagged with @Tag("mobile") and excluded from default Maven runs
 * so that CI/local API suites remain fast and decoupled from emulator availability.
 */
@Tag("mobile")
class MobileHappyPathTest extends MobileTestBase {

    @Test
    @DisplayName("Login: valid user can sign in and return to catalog")
    void validUserCanSignIn() {
        HeaderNavigation header = new HeaderNavigation(driver);
        LoginPage loginPage = header.openMenu().navigateToLogin();

        loginPage.signIn("bob@example.com", "10203040");

        CatalogPage catalog = new CatalogPage(driver);
        assertTrue(catalog.isCatalogDisplayed(), "Catalog should be displayed after successful login");
    }

    @Test
    @DisplayName("Login: invalid user sees descriptive authentication error")
    void invalidUserSeesAnError() {
        HeaderNavigation header = new HeaderNavigation(driver);
        LoginPage loginPage = header.openMenu().navigateToLogin();

        loginPage.signIn("nobody@example.com", "wrong-password");

        assertTrue(loginPage.isErrorMessageDisplayed(),
                "Error banner should be displayed for invalid credentials");
    }

    @Test
    @DisplayName("Cart: user can select a catalog item, add to cart, and verify cart contents")
    void userCanAddAProductAndVerifyCart() {
        CatalogPage catalog = new CatalogPage(driver);
        ProductDetailsPage productPage = catalog.selectProduct("Sauce Labs Backpack");

        productPage.addToCart();

        HeaderNavigation header = new HeaderNavigation(driver);
        CartPage cart = header.openCart();

        assertTrue(cart.isItemInCart("Sauce Labs Backpack"), "Added product should appear in the cart");
        assertTrue(cart.isProceedToCheckoutButtonDisplayed(), "Proceed to checkout should be enabled in cart");
    }

    @Test
    @DisplayName("Checkout: user can proceed from cart to checkout and complete shipping address step")
    void userCanProceedToCheckoutAndFillShippingDetails() {
        // Pre-requisite: ensure user is logged in
        HeaderNavigation header = new HeaderNavigation(driver);
        LoginPage loginPage = header.openMenu().navigateToLogin();
        loginPage.signIn("bob@example.com", "10203040");

        // Add product to cart
        CatalogPage catalog = new CatalogPage(driver);
        ProductDetailsPage productPage = catalog.selectProduct("Sauce Labs Backpack");
        productPage.addToCart();

        // Proceed to Checkout
        CartPage cart = header.openCart();
        CheckoutPage checkout = cart.proceedToCheckout();

        assertTrue(checkout.isShippingAddressFormDisplayed(), "Shipping address form should be displayed");

        checkout.fillShippingAddress(
                "Jane Doe",
                "123 Quality Assurance Way",
                "San Francisco",
                "California",
                "94105",
                "United States"
        );

        checkout.clickToPayment();
        assertTrue(checkout.isPaymentMethodScreenDisplayed(), "Navigated to payment method step");
    }
}
