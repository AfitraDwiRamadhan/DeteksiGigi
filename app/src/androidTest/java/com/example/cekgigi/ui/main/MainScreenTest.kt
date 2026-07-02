package com.cekgigi.app.ui.dashboard

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.cekgigi.app.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the Dashboard (Main Screen).
 */
@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(DashboardActivity::class.java)

    @Test
    fun dashboard_displaysCorrectTitle() {
        // Check if dashboard title is displayed
        onView(withText(R.string.dashboard_title))
            .check(matches(isDisplayed()))
    }

    @Test
    fun dashboard_displaysCaptureButton() {
        // Check if the "Skrining Sekarang" card is displayed
        onView(withId(R.id.cardAmbilFoto))
            .check(matches(isDisplayed()))
    }

    @Test
    fun dashboard_displaysWelcomeMessage() {
        // Check if greeting message is displayed
        onView(withText(R.string.halo_user))
            .check(matches(isDisplayed()))
    }
}
