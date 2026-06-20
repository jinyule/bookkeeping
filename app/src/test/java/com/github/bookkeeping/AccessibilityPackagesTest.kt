package com.github.bookkeeping

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityPackagesTest {
    @Test
    fun neverProcessesSelfPackage() {
        assertFalse(AccessibilityPackages.shouldProcess(AccessibilityPackages.SELF))
    }

    @Test
    fun processesKnownPaymentPackages() {
        assertTrue(AccessibilityPackages.shouldProcess("com.tencent.mm"))
        assertTrue(AccessibilityPackages.shouldProcess("com.eg.android.AlipayGphone"))
        assertTrue(AccessibilityPackages.shouldProcess("com.unionpay"))
        assertTrue(AccessibilityPackages.shouldProcess("com.vivo.wallet"))
    }

    @Test
    fun rejectsUnknownPackages() {
        assertFalse(AccessibilityPackages.shouldProcess("com.example.bank"))
        assertFalse(AccessibilityPackages.shouldProcess("com.android.systemui"))
        assertFalse(AccessibilityPackages.shouldProcess(null))
        assertFalse(AccessibilityPackages.shouldProcess(""))
    }
}
