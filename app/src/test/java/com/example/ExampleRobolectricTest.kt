package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.IslandSettingsRepository
import com.example.model.IslandThemePreset
import com.example.system.FormatUtils
import com.example.system.PermissionHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appNameAndFormattersAreValid() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Dynamic Island Pro", context.getString(R.string.app_name))
        assertEquals("0 B/s", FormatUtils.formatBytesPerSec(0L))
        assertEquals("512 B/s", FormatUtils.formatBytesPerSec(512L))
        assertEquals("10 KB/s", FormatUtils.formatBytesPerSec(10240L))
        assertEquals("2.0 MB/s", FormatUtils.formatBytesPerSec(2L * 1024L * 1024L))
        assertEquals("1:05", FormatUtils.formatDurationMs(65_000L))
        assertEquals("01:05", FormatUtils.formatElapsedSeconds(65L))
    }

    @Test
    fun settingsRepositoryPersistsCustomization() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = IslandSettingsRepository(context)
        repo.applyThemePreset(IslandThemePreset.NEON)
        repo.updateDimensions(widthDp = 240f, heightDp = 44f, cornerRadiusDp = 20f)
        val saved = repo.settingsFlow.first()
        assertEquals(IslandThemePreset.NEON, saved.themePreset)
        assertEquals(240f, saved.pillWidthDp)
        assertEquals(44f, saved.pillHeightDp)
        assertEquals(20f, saved.cornerRadiusDp)
    }

    @Test
    fun dashboardRendersAndNavigatesAcrossAllTabs() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("master_island_switch").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_list").performScrollToNode(hasTestTag("dynamic_island_pill"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dynamic_island_pill").assertIsDisplayed()
        composeRule.onNodeWithTag("dynamic_island_pill").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_studio").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Customization Studio").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_modules").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Media Session Controller").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_setup").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Permission Setup Manager").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [29, 34])
    fun permissionHelperWorksOnAndroid10AndAndroid14() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val status = PermissionHelper.inspectPermissions(context)
        assertTrue(status.sdkInt == 29 || status.sdkInt == 34)
        assertTrue(status.androidVersionLabel.isNotBlank())
    }
}
