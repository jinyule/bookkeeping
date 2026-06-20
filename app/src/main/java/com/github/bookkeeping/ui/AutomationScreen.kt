package com.github.bookkeeping.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.bookkeeping.DebugNotificationPoster
import com.github.bookkeeping.R
import com.github.bookkeeping.SettingsStore
import kotlinx.coroutines.delay

@Composable
internal fun AutomationScreen(
    onFixture: () -> Unit,
    onToast: (String) -> Unit,
    onReload: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }
    val testNotificationBlocked = stringResource(R.string.test_notification_blocked)
    val testNotificationSent = stringResource(R.string.test_notification_sent)
    var paused by remember { mutableStateOf(store.automaticPaused) }
    var notificationEnabled by remember { mutableStateOf(store.isNotificationListenerEnabled()) }
    var accessibilityEnabled by remember { mutableStateOf(store.isAccessibilityEnabled()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) {
        while (true) {
            notificationEnabled = store.isNotificationListenerEnabled()
            accessibilityEnabled = store.isAccessibilityEnabled()
            delay(1_000L)
        }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { TopBar(title = stringResource(R.string.automation_title)) }
        item {
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (paused) Color(0xFFFFEFE8) else Color(0xFFEAF6EF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = if (paused) Color(0xFFE07A5F) else Color(0xFF2E7D74))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(if (paused) R.string.automation_status_paused else R.string.automation_status_on), fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.automation_desc), color = Color(0xFF65706D), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = !paused, onCheckedChange = {
                        paused = !it
                        store.automaticPaused = paused
                    })
                }
            }
        }
        item {
            PermissionCard(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.notification_permission_title),
                desc = stringResource(R.string.notification_permission_desc),
                enabled = notificationEnabled,
                onOpen = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            )
        }
        item {
            PermissionCard(
                icon = Icons.Default.Accessibility,
                title = stringResource(R.string.accessibility_permission_title),
                desc = stringResource(R.string.accessibility_permission_desc),
                enabled = accessibilityEnabled,
                onOpen = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            )
        }
        item {
            Text(
                stringResource(R.string.source_title),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        items(sourceItems()) { source ->
            SourceToggleRow(store, source)
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        onToast(testNotificationBlocked)
                    } else if (DebugNotificationPoster.postWeChatPayment(context)) {
                        onToast(testNotificationSent)
                        onReload()
                    }
                }) {
                    Icon(Icons.Default.Notifications, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_post_test))
                }
            }
            Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Button(onClick = onFixture, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                    Icon(Icons.Default.Accessibility, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_parse_fixture))
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(icon: ImageVector, title: String, desc: String, enabled: Boolean, onOpen: () -> Unit) {
    CardBlock {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(desc, color = Color(0xFF65706D), style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onOpen) {
                Text(if (enabled) stringResource(R.string.action_open) else stringResource(R.string.action_enable))
            }
        }
    }
}

@Composable
private fun SourceToggleRow(store: SettingsStore, source: SourceUi) {
    var enabled by remember(source.key) { mutableStateOf(store.isSourceEnabled(source.key)) }
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(source.title), fontWeight = FontWeight.SemiBold)
                Text(stringResource(source.desc), color = Color(0xFF65706D), style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = enabled, onCheckedChange = {
                enabled = it
                store.setSourceEnabled(source.key, it)
            })
        }
    }
}
