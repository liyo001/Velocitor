# SDET Take-Home Assignment — Velocitor Solutions

This repository provides an enterprise-ready automation suite covering **API Test Automation** with Rest Assured and Java, **Mobile UI Automation** with Appium and Java (Page Object Model), and a **CI/CD Quality Gate Pipeline** via GitHub Actions.

---

## Table of Contents
1. [Tech Stack & Architecture](#tech-stack--architecture)
2. [Part 1 — API Automation Suite (Rest Assured + JUnit 5)](#part-1--api-automation-suite-rest-assured--junit-5)
   - [Setup & Run Instructions](#setup--run-instructions)
   - [Coverage Details](#coverage-details)
   - [Independence & Isolation Strategy](#independence--isolation-strategy)
   - [Target API Quirk Analysis](#target-api-quirk-analysis)
3. [Part 2 — Mobile UI Automation (Appium + Java)](#part-2--mobile-ui-automation-appium--java)
   - [Suite Design & Architectural Choices](#suite-design--architectural-choices)
   - [Page Objects & Sample Tests](#page-objects--sample-tests)
   - [Running with a Local Emulator](#running-with-a-local-emulator)
4. [Part 3 — CI Pipeline & Quality Gate Evolution (Bonus)](#part-3--ci-pipeline--quality-gate-evolution-bonus)
   - [GitHub Actions Workflow](#github-actions-workflow)
   - [Evolving into a Production Quality Gate](#evolving-into-a-production-quality-gate)
   - [Handling Flaky Tests](#handling-flaky-tests)
   - [Mobile CI Strategy: Local Emulators vs. Device Cloud](#mobile-ci-strategy-local-emulators-vs-device-cloud)
   - [Keeping Feedback Fast as Suites Scale](#keeping-feedback-fast-as-suites-scale)
5. [Design Decisions & Trade-Offs](#design-decisions--trade-offs)
6. [What I'd Do Next With More Time](#what-id-do-next-with-more-time)
7. [Notes on AI Assistance](#notes-on-ai-assistance)

---

## Tech Stack & Architecture

- **Language & Runtime:** Java 17 (LTS)
- **API Testing:** Rest Assured 5.5.0, Hamcrest Matchers 2.2
- **Mobile UI Testing:** Appium Java Client 9.2.3, Selenium Support 4.26.0 (BOM managed)
- **Test Runner:** JUnit 5 (Jupiter 5.11.3)
- **Build & Reporting:** Maven 3.9+ with included Maven Wrapper (`./mvnw`), Maven Surefire Plugin 3.5.2
- **Continuous Integration:** GitHub Actions (`.github/workflows/api-tests.yml`)

---

## Part 1 — API Automation Suite (Rest Assured + JUnit 5)

Target API: [Restful Booker](https://restful-booker.herokuapp.com)

### Setup & Run Instructions

**Prerequisites:** JDK 17+ installed. A Maven wrapper (`./mvnw`) is included, so a pre-installed Maven binary is optional.

To run the full API test suite against the default public environment:
```bash
./mvnw clean test
# or if using system maven:
mvn clean test
```

To run a specific test method in isolation:
```bash
./mvnw test -Dtest=BookingApiTest#bookingLifecycle_shouldRoundTripAllFieldsAcrossLifecycle
```

To target a staging or alternative environment:
```bash
# Via Environment Variable:
BASE_URL=https://staging.restful-booker.example.com ./mvnw test

# Via Maven System Property:
./mvnw test -Dbase.url=https://staging.restful-booker.example.com
```

### Coverage Details

1. **Authentication (`/auth`):**
   - `auth_shouldObtainTokenWithValidCredentials`: Obtains token using `admin / password123` and verifies token presence.
   - `auth_shouldRejectInvalidCredentials`: Validates that invalid credentials return `reason: Bad credentials` and no token.
2. **Booking Lifecycle & Round-Trip Response Validation:**
   - `bookingLifecycle_shouldRoundTripAllFieldsAcrossLifecycle`:
     - **Create (POST):** Creates a booking; verifies 200 OK and response body matches request.
     - **Retrieve (GET):** Validates complete field-by-field round trip (`firstname`, `lastname`, `totalprice`, `depositpaid`, `bookingdates.checkin`, `bookingdates.checkout`, `additionalneeds`).
     - **Full Update (PUT):** Updates all fields with auth token cookie; verifies all updated fields.
     - **Partial Update (PATCH):** Updates specific fields (`firstname`, `additionalneeds`) with auth token; asserts patched fields updated while unpatched fields remain unchanged.
     - **Delete (DELETE):** Removes booking with auth token; verifies 201 Created status.
     - **Post-Delete Verification (GET):** Verifies booking ID now returns 404 Not Found.
3. **Querying Filter by Name and Date:**
   - `querying_shouldFindBookingsByNameAndDateFilters`:
     - Filters by `firstname`.
     - Filters by `lastname`.
     - Filters by combined `firstname` and `lastname`.
     - Filters by date range window (`checkin` and `checkout`).
4. **Adversarial & Negative Cases:**
   - `adversarial_shouldRejectUnauthenticatedModifications`:
     - Verifies PUT without token returns 403 Forbidden.
     - Verifies PATCH without token returns 403 Forbidden.
     - Verifies DELETE without token returns 403 Forbidden.
     - Verifies DELETE with forged/invalid token returns 403 Forbidden.
   - `adversarial_shouldReturnNotFoundForNonExistentBooking`:
     - Verifies GET on non-existent booking ID (`999999999`) returns 404 Not Found.
   - `adversarial_shouldRejectMalformedPayloadOnCreate`:
     - Verifies POST with missing required fields returns 500.
     - Verifies POST with empty body `{}` returns 500.

### Independence & Isolation Strategy

- **Zero Test Coupling:** Each test provisions its own booking with unique identifiers using `System.nanoTime()` suffix (e.g., `Sam172891...`).
- **Guaranteed Cleanup:** All created bookings are tracked and cleaned up in `finally` blocks, ensuring no leaked state on public demo APIs even if assertions fail midway.
- **Configurable Environment:** `TestConfig.java` evaluates System properties (`-Dbase.url`), environment variables (`BASE_URL`), and falls back to `src/test/resources/config.properties`.

### Target API Quirk Analysis

During automated verification against Restful Booker, we identified specific API behaviors:
- **Date Boundary Filtering:** Restful Booker's `checkin` query filter has a known implementation quirk where exact equality on checkin date can drop records due to strict date boundary comparison. Querying a date window (e.g. `checkin` 1 day prior, or date range) provides deterministic retrieval.
- **Delete Status Code:** The API returns `201 Created` upon successful `DELETE /booking/{id}` rather than `200` or `204`.
- **Bad Auth Response Code:** Unsuccessful POST to `/auth` returns `200 OK` with a JSON payload `{"reason": "Bad credentials"}` rather than a `401 Unauthorized`.
- **Adversarial Auth Status:** Unauthorized PUT/PATCH/DELETE calls return `403 Forbidden`.

---

## Part 2 — Mobile UI Automation (Appium + Java)

Target Application: **Sauce Labs "My Demo App" (Android)**

### Suite Design & Architectural Choices

1. **Page Object Model (POM):**
   - High cohesion and separation of concerns: tests express business user journeys; Page Objects encapsulate locators and driver mechanics.
   - Clean page hierarchy:
     - `BasePage`: Encapsulates driver actions and explicit wait strategies (`WebDriverWait`).
     - `HeaderNavigation`: Controls the persistent top navigation bar (menu button, cart badge).
     - `NavigationMenu`: Controls side drawer navigation (navigating to Login, Catalog, Log Out).
     - `LoginPage`: Encapsulates authentication input fields, button clicks, and error banner verifications.
     - `CatalogPage`: Encapsulates product listing and selection.
     - `ProductDetailsPage`: Encapsulates product details and adding items to cart.
     - `CartPage`: Encapsulates cart review and proceeding to checkout.
     - `CheckoutPage`: Encapsulates shipping address form filling, continuing to payment, and payment screen verification.
2. **Locator Strategy:**
   - Accessibility IDs (`AppiumBy.accessibilityId(...)` / `content-desc`) are prioritized as the primary locator strategy. They are platform-resilient, fast, and encourage accessible app development.
3. **Explicit Synchronization:**
   - Rather than fragile `Thread.sleep()` or global implicit waits, `BasePage` employs `WebDriverWait` with `ExpectedConditions` (`visibilityOfElementLocated`, `elementToBeClickable`).
4. **Execution Tagging & Isolation:**
   - Mobile tests are annotated with `@Tag("mobile")` and excluded from default Maven test execution (`<excluded.groups>mobile</excluded.groups>`).
   - Running `./mvnw test` executes the fast API suite without demanding an Appium server or Android emulator.
   - Activating the `mobile` Maven profile (`-Pmobile`) enables the mobile test suite.

### Page Objects & Sample Tests

The sample suite (`MobileHappyPathTest.java`) covers:
1. `validUserCanSignIn`: Opens menu -> navigates to login -> signs in with valid credentials (`bob@example.com` / `10203040`) -> verifies return to Catalog.
2. `invalidUserSeesAnError`: Navigates to login -> submits invalid credentials -> verifies error message: *"Username and password do not match any user in this service."*
3. `userCanAddAProductAndVerifyCart`: Selects "Sauce Labs Backpack" from Catalog -> adds to cart -> opens Cart -> validates product name in cart and checkout button visibility.
4. `userCanProceedToCheckoutAndFillShippingDetails`: Adds item to cart -> proceeds to checkout -> fills complete shipping address (Full Name, Address, City, State, Zip, Country) -> taps "To Payment" -> verifies payment method screen navigation.

### Running with a Local Emulator

1. Install Android SDK, platform-tools, and launch an Android emulator (e.g., Pixel 7, Android 13).
2. Download Sauce Labs My Demo App APK (`mda-*.apk`) from [Sauce Labs Mobile Releases](https://github.com/saucelabs/my-demo-app-android/releases).
3. Start Appium server (v2.x with `uiautomator2` driver):
   ```bash
   appium
   ```
4. Run the mobile suite:
   ```bash
   DEMO_APP_PATH=/absolute/path/to/mda.apk \
   APPIUM_URL=http://127.0.0.1:4723 \
   ./mvnw test -Pmobile
   ```

---

## Part 3 — CI Pipeline & Quality Gate Evolution (Bonus)

### GitHub Actions Workflow

Located at [`.github/workflows/api-tests.yml`](file:///.github/workflows/api-tests.yml):
- **Triggers:** Push to `main`, Pull Requests targeting `main`, and manual execution (`workflow_dispatch`).
- **Job 1: `api-suite`:**
  - Runs on standard Ubuntu runner with JDK 17 and Maven caching.
  - Executes `./mvnw -B test` against Restful Booker.
  - Automatically captures and uploads Surefire test reports via `actions/upload-artifact@v4` on pass or fail (`if: always()`).
  - Fails the build immediately if any API test fails.
- **Job 2: `mobile-suite-cloud`:**
  - Designed for Device Cloud execution (Sauce Labs / BrowserStack).
  - Depends on `api-suite` passing (`needs: api-suite`).
  - Configured with device cloud capabilities, secret credentials, and execution via `./mvnw -B test -Pmobile`.

---

### Evolving into a Production Quality Gate

To evolve this pipeline into a high-confidence enterprise quality gate:

#### 1. What Should Block a Merge
- **Deterministic P0/P1 Suites:** Fast unit tests, API integration suites, and critical smoke flows must be 100% green.
- **Contract & Schema Testing:** Pact or JSON Schema validation to detect breaking API changes before code merges.
- **Static Analysis & Security:** Zero blocker/critical SonarQube issues, zero high-severity CVEs in dependencies (`dependency-check`).
- **Strict Gating:** PRs cannot merge if the API regression suite fails. Non-blocking suites (visual regression, expansive device compatibility) run asynchronously.

#### 2. Handling Flaky Tests
- **The Quarantine Pattern:**
  - Flaky tests are quarantined into a `@Tag("flaky")` group that runs in a non-blocking diagnostic pipeline.
  - Quarantined tests must have an assigned owner and Jira ticket with a 14-day SLA to fix or permanently delete.
- **Telemetry & Flake Tracking:**
  - Track flakiness metrics via test analytics dashboards (e.g., Allure TestOps, Datadog CI Visibility) showing pass rate history and duration spikes.
- **Smart Retries:**
  - Do **not** use retries on local runs or in PR gates to hide bugs.
  - In post-merge or device cloud runs, allow max 1 retry with failure artifact logging so developers can inspect intermittent timing/infrastructure glitches.

#### 3. Mobile CI Strategy: Local Emulators vs. Device Cloud
- **Local Emulators on CI Runners:**
  - *Drawbacks:* Standard GitHub Actions Linux runners lack nested KVM virtualization (or charge premium rates for larger hardware). Emulators take 15–20 minutes to boot, experience severe CPU throttling, and have high network timeout rates.
- **Device Cloud (Sauce Labs / BrowserStack / AWS Device Farm):**
  - *Benefits:* Instant availability across real hardware, hundreds of Android/iOS OS versions, battery/network simulation, parallel execution, and built-in video/log capture.
  - *Recommended Strategy:*
    1. **Pre-Merge (PR Gate):** Run unit tests, Robolectric tests, and API tests on the runner (~3–5 minutes).
    2. **Post-Merge / Nightly:** Trigger mobile smoke and regression on a device cloud matrix (~10–15 minutes).

#### 4. Keeping Feedback Fast as Suites Scale
- **The Testing Pyramid:** Push coverage down to API and component layers; keep mobile UI tests focused strictly on end-to-end user journeys.
- **Parallelization & Sharding:**
  - Use Maven Surefire forks (`<forkCount>4</forkCount>`) and JUnit 5 concurrent execution (`junit.jupiter.execution.parallel.enabled = true`).
  - Shard test runs across multiple GitHub Actions runner matrix jobs.
- **Test Impact Analysis (TIA):** Run only tests that exercise code paths altered in the pull request diff, saving full regression for nightly runs.

---

## Design Decisions & Trade-Offs

1. **Client Wrapper (`BookerClient`) vs. Raw Rest Assured in Tests:**
   - *Decision:* Created a lightweight client wrapper for endpoint URLs, headers, and payload serialization while leaving assertions inside test methods.
   - *Trade-off:* Avoided over-engineering full DTO models for simple JSON maps while keeping test methods clean, readable, and focused on assertions.
2. **Selenium BOM Version Pinning:**
   - *Decision:* Added `selenium-bom` (version `4.26.0`) in `dependencyManagement`.
   - *Trade-off:* Appium `java-client:9.2.3` declares an open version range `[4.19.0, 5.0)`. Without explicit version pinning, Maven resolves latest Selenium 4.51+ where legacy classes (`ContextAware`, `LocationContext`) were removed, causing compilation failure. Pinning guarantees zero compilation breakage on any machine.
3. **Maven Wrapper Inclusion:**
   - *Decision:* Generated and included `./mvnw` and `.mvn/wrapper/`.
   - *Trade-off:* Eliminates "works on my machine" issues for evaluators who have JDK 17 installed but no pre-configured Maven 3.9 CLI.

---

## What I'd Do Next With More Time

1. **JSON Schema Validation:** Integrate `io.rest-assured:json-schema-validator` to enforce strict response contracts on every endpoint.
2. **Allure / Extent Reports:** Add Allure JUnit 5 listeners with automatic request/response filter attachments and failed mobile screenshot capture.
3. **Pojos / Builders with Lombok:** Migrate request maps to Jackson/Lombok POJO models for compile-time type safety.
4. **Appium Visual & Gesture Testing:** Add swipe/scroll helpers for dynamic Android views and Appium Inspector-verified device fallbacks.
5. **Dockerized Mock Environment:** Provide a `docker-compose.yml` spinning up a local mock instance of Restful Booker for zero-latency, offline testing.

---

## Notes on AI Assistance

AI assistance (Claude / Gemini via Antigravity) was utilized to:
1. Review the assignment specification and checklist against project implementation.
2. Troubleshoot and identify the Selenium 4.51 transitive dependency conflict with Appium 9.2.3.
3. Inspect and verify Restful Booker's live query parameter boundary behavior via HTTP probes.
4. Scaffold Page Object structures and format this comprehensive technical documentation.

All code, assertions, test logic, architectural trade-offs, and quality gate designs were thoroughly verified, executed, and validated locally.
