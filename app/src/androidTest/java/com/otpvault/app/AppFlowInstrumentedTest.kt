package com.otpvault.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * First-launch flow smoke test: splash -> onboarding -> PIN creation.
 * Full add/view/copy/delete flows are exercised in manual QA + the
 * repository tests, because they require a seeded PIN state.
 */
@RunWith(AndroidJUnit4::class)
class AppFlowInstrumentedTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test fun firstLaunchShowsOnboarding() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Get Started").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Get Started").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Create PIN").assertIsDisplayed()
    }
}
