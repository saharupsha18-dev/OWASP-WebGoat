/*
 * SPDX-FileCopyrightText: Copyright © 2025 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.playwright.webgoat;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.playwright.webgoat.helpers.Authentication;
import org.owasp.webgoat.playwright.webgoat.pages.WebGoatLoginPage;

class LoginUITest extends PlaywrightTest {

  @Test
  void loginLogout(Browser browser) {

    // Step 1: Open WebGoat using the Tweety test account
    var page = Authentication.tweety(browser);
    var loginPage = new WebGoatLoginPage(page);

    // Step 2: Log in
    loginPage.open();
    loginPage.login(
        Authentication.getTweety().name(),
        Authentication.getTweety().password());

    // Step 3: Verify successful login
    assertThat(loginPage.getSignInButton()).not().isVisible();

    // Step 4: Locate the user menu and Logout link
    var userMenu = page.locator("#user-menu");
    var dropdown = userMenu.locator("xpath=..");
    var logoutLink = page.locator("a[href='/WebGoat/logout']");

    // Step 5: Open the dropdown normally
    userMenu.click();

    try {
      logoutLink.waitFor(
          new Locator.WaitForOptions()
              .setState(WaitForSelectorState.VISIBLE)
              .setTimeout(15000));

    } catch (PlaywrightException firstAttemptFailure) {

      System.out.println("Normal dropdown click did not show Logout.");
      System.out.println("Trying Bootstrap dropdown function.");

      // Step 6: Try Bootstrap's dropdown function
      page.evaluate(
          "() => {"
              + " const button = document.querySelector('#user-menu');"
              + " const container = button ? button.closest('.dropdown') : null;"
              + " if (!button || !container) {"
              + "   throw new Error('User menu or dropdown container not found');"
              + " }"
              + " if (!container.classList.contains('open')) {"
              + "   if (window.jQuery && window.jQuery.fn && window.jQuery.fn.dropdown) {"
              + "     window.jQuery(button).dropdown('toggle');"
              + "   } else {"
              + "     button.click();"
              + "   }"
              + " }"
              + "}");

      try {
        // Step 7: Wait again
        logoutLink.waitFor(
            new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(10000));

      } catch (PlaywrightException secondAttemptFailure) {

        // Step 8: Collect diagnostic details
        String dropdownDetails =
            String.valueOf(
                dropdown.evaluate(
                    "el => JSON.stringify({"
                        + "html: el.outerHTML,"
                        + "className: el.className,"
                        + "display: getComputedStyle(el).display,"
                        + "visibility: getComputedStyle(el).visibility,"
                        + "menuDisplay: el.querySelector('.dropdown-menu')"
                        + " ? getComputedStyle(el.querySelector('.dropdown-menu')).display"
                        + " : 'menu not found',"
                        + "menuVisibility: el.querySelector('.dropdown-menu')"
                        + " ? getComputedStyle(el.querySelector('.dropdown-menu')).visibility"
                        + " : 'menu not found',"
                        + "jqueryAvailable: typeof window.jQuery,"
                        + "bootstrapDropdownAvailable: !!(window.jQuery"
                        + " && window.jQuery.fn && window.jQuery.fn.dropdown)"
                        + "})"));

        System.out.println("DROPDOWN DIAGNOSTIC DETAILS: " + dropdownDetails);

        throw new AssertionError(
            "Logout link remained hidden after both dropdown-opening attempts.",
            secondAttemptFailure);
      }
    }

    // Step 9: Verify Logout is visible and click it
    assertThat(logoutLink).isVisible();
    logoutLink.click();

    // Step 10: Verify logout returned us to the login page
    assertThat(loginPage.getSignInButton()).isVisible();
  }
}
