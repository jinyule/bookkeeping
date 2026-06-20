package com.example.bookkeeping

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.core.content.edit
import com.example.bookkeeping.service.BookkeepingAccessibilityService
import com.example.bookkeeping.service.BookkeepingNotificationListenerService

class SettingsStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("bookkeeping_settings", Context.MODE_PRIVATE)

    var languageMode: String
        get() = prefs.getString("languageMode", LanguageMode.SYSTEM) ?: LanguageMode.SYSTEM
        set(value) = prefs.edit { putString("languageMode", value) }

    var automaticPaused: Boolean
        get() = prefs.getBoolean("automaticPaused", false)
        set(value) = prefs.edit { putBoolean("automaticPaused", value) }

    fun isSourceEnabled(sourceKey: String): Boolean =
        prefs.getBoolean("source.$sourceKey.enabled", true)

    fun setSourceEnabled(sourceKey: String, enabled: Boolean) {
        prefs.edit { putBoolean("source.$sourceKey.enabled", enabled) }
    }

    fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            ?: return false
        val component = ComponentName(context, BookkeepingNotificationListenerService::class.java).flattenToString()
        return flat.split(':').any { it.equals(component, ignoreCase = true) }
    }

    fun isAccessibilityEnabled(): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_accessibility_services")
            ?: return false
        val component = ComponentName(context, BookkeepingAccessibilityService::class.java).flattenToString()
        return flat.split(':').any { it.equals(component, ignoreCase = true) }
    }
}
