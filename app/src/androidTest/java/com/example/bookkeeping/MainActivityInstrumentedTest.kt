package com.example.bookkeeping

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentedTest {
    @Test
    fun launchesAndBottomNavigationWorks() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val launch = instrumentation.runShell(
            "am start -W -n ${context.packageName}/${MainActivity::class.java.name}"
        )
        waitForTag(SCREEN_HOME)

        val windows = instrumentation.runShell("dumpsys window")
        assertTrue("Launch failed: $launch", launch.contains("Status: ok"))
        assertTrue(
            "Expected focused app to be ${context.packageName}",
            windows.contains("mFocusedApp=ActivityRecord") &&
                windows.contains("${context.packageName}/.MainActivity")
        )
        assertFalse("Window dump should not show keyguard as active: $windows", windows.contains("deviceLocked=1"))

        tapTag(NAV_REPORTS)
        waitForTag(SCREEN_REPORTS)

        tapTag(NAV_HOME)
        waitForTag(SCREEN_HOME)

        tapTag(NAV_REPORTS)
        waitForTag(SCREEN_REPORTS)
        tapTag(NAV_AUTOMATION)
        waitForTag(SCREEN_AUTOMATION)

        tapTag(NAV_REPORTS)
        waitForTag(SCREEN_REPORTS)
        tapTag(NAV_SETTINGS)
        waitForTag(SCREEN_SETTINGS)

        tapTag(NAV_REPORTS)
        waitForTag(SCREEN_REPORTS)
        tapTag(NAV_ADD)
        waitForTag(ADD_BILL_SHEET)

        instrumentation.runShell("input keyevent BACK")
        waitForTagGone(ADD_BILL_SHEET)

        tapTag(NAV_AUTOMATION)
        waitForTag(SCREEN_AUTOMATION)

        tapTag(NAV_HOME)
        waitForTag(SCREEN_HOME)

        tapTag(NAV_AUTOMATION)
        waitForTag(SCREEN_AUTOMATION)
        tapTag(NAV_REPORTS)
        waitForTag(SCREEN_REPORTS)

        tapTag(NAV_AUTOMATION)
        waitForTag(SCREEN_AUTOMATION)
        tapTag(NAV_SETTINGS)
        waitForTag(SCREEN_SETTINGS)

        tapTag(NAV_AUTOMATION)
        waitForTag(SCREEN_AUTOMATION)
        tapTag(NAV_ADD)
        waitForTag(ADD_BILL_SHEET)

        instrumentation.runShell("input keyevent BACK")
        waitForTagGone(ADD_BILL_SHEET)

        tapTag(NAV_HOME)
        waitForTag(SCREEN_HOME)
        tapTag(ACTION_SEARCH)
        waitForTag(SCREEN_SEARCH)
        tapTag(NAV_REPORTS)
        waitForTag(SCREEN_REPORTS)

        tapTag(NAV_SETTINGS)
        waitForTag(SCREEN_SETTINGS)
        tapTag(ACTION_CATEGORIES)
        waitForTag(SCREEN_CATEGORIES)
        tapTag(NAV_AUTOMATION)
        waitForTag(SCREEN_AUTOMATION)
    }

    private fun tapTag(tag: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val node = waitForTag(tag)
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        instrumentation.runShell("input tap ${bounds.centerX()} ${bounds.centerY()}")
        instrumentation.waitForIdleSync()
        Thread.sleep(500L)
    }

    private fun waitForTag(tag: String, timeoutMs: Long = 5_000L): AccessibilityNodeInfo {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val resourceIds = tag.resourceIds()
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastRootPackage: CharSequence? = null
        while (System.currentTimeMillis() < deadline) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            lastRootPackage = root?.packageName
            val node = root?.findByAnyViewId(resourceIds)
            if (node != null && node.isVisibleToUser) return node
            Thread.sleep(100L)
        }
        throw AssertionError("Could not find visible node with resource id in $resourceIds, activeRootPackage=$lastRootPackage")
    }

    private fun waitForTagGone(tag: String, timeoutMs: Long = 5_000L) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val resourceIds = tag.resourceIds()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            val node = root?.findByAnyViewId(resourceIds)
            if (node == null || !node.isVisibleToUser) return
            Thread.sleep(100L)
        }
        throw AssertionError("Node with resource id in $resourceIds was still visible")
    }

    private fun String.resourceIds(): Set<String> {
        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        return setOf(this, "$packageName:id/$this")
    }

    private fun AccessibilityNodeInfo.findByAnyViewId(resourceIds: Set<String>): AccessibilityNodeInfo? {
        if (viewIdResourceName in resourceIds) return this
        for (index in 0 until childCount) {
            val child = getChild(index) ?: continue
            val match = child.findByAnyViewId(resourceIds)
            if (match != null) return match
        }
        return null
    }

    private fun android.app.Instrumentation.runShell(command: String): String {
        return uiAutomation.executeShellCommand(command).use { fd ->
            FileInputStream(fd.fileDescriptor).bufferedReader().use { it.readText() }
        }
    }

    private companion object {
        const val NAV_HOME = "nav_home"
        const val NAV_REPORTS = "nav_reports"
        const val NAV_ADD = "nav_add"
        const val NAV_AUTOMATION = "nav_automation"
        const val NAV_SETTINGS = "nav_settings"
        const val SCREEN_HOME = "screen_home"
        const val SCREEN_REPORTS = "screen_reports"
        const val SCREEN_AUTOMATION = "screen_automation"
        const val SCREEN_SETTINGS = "screen_settings"
        const val SCREEN_SEARCH = "screen_search"
        const val SCREEN_CATEGORIES = "screen_categories"
        const val ADD_BILL_SHEET = "add_bill_sheet"
        const val ACTION_SEARCH = "action_search"
        const val ACTION_CATEGORIES = "action_categories"
    }
}
