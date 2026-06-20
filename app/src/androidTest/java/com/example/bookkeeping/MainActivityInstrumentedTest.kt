package com.example.bookkeeping

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
    fun launchesBookkeepingHome() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val launch = instrumentation.runShell(
            "am start -W -n ${context.packageName}/${MainActivity::class.java.name}"
        )
        instrumentation.waitForIdleSync()
        Thread.sleep(1_000L)

        val windows = instrumentation.runShell("dumpsys window")
        assertTrue("Launch failed: $launch", launch.contains("Status: ok"))
        assertTrue(
            "Expected focused app to be ${context.packageName}, launch=$launch",
            windows.contains("mFocusedApp=ActivityRecord") &&
                windows.contains("${context.packageName}/.MainActivity")
        )
        assertFalse("Window dump should not show keyguard as active: $windows", windows.contains("deviceLocked=1"))
    }

    private fun android.app.Instrumentation.runShell(command: String): String {
        return uiAutomation.executeShellCommand(command).use { fd ->
            FileInputStream(fd.fileDescriptor).bufferedReader().use { it.readText() }
        }
    }
}
