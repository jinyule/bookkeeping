package com.example.bookkeeping.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bookkeeping.BookkeepingRepository
import com.example.bookkeeping.LanguageMode
import com.example.bookkeeping.R
import com.example.bookkeeping.SettingsStore
import kotlinx.coroutines.launch

@Composable
internal fun SettingsScreen(
    repository: BookkeepingRepository,
    onAutomation: () -> Unit,
    onCategories: () -> Unit,
    onImportDone: (String) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsStore(context) }
    var languageMode by remember { mutableStateOf(store.languageMode) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                runCatching { repository.importFrom(uri) }
                    .onSuccess { onImportDone(resources.getString(R.string.import_success, it.imported, it.skipped)) }
                    .onFailure { onImportDone(resources.getString(R.string.import_failed, it.message.orEmpty())) }
            }
        }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { TopBar(title = stringResource(R.string.settings_title)) }
        item {
            CardBlock {
                Text(stringResource(R.string.settings_language), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        LanguageMode.SYSTEM to R.string.language_follow_system,
                        LanguageMode.ZH to R.string.language_zh,
                        LanguageMode.EN to R.string.language_en
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = languageMode == mode,
                            onClick = {
                                languageMode = mode
                                store.languageMode = mode
                                (context as? Activity)?.recreate()
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(label), maxLines = 1) }
                        )
                    }
                }
            }
        }
        item {
            SettingsRow(Icons.Default.AutoAwesome, stringResource(R.string.settings_auto), stringResource(R.string.automation_desc)) {
                onAutomation()
            }
            SettingsRow(Icons.Default.Notifications, stringResource(R.string.settings_notifications), stringResource(R.string.notification_permission_desc)) {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            SettingsRow(
                Icons.AutoMirrored.Filled.ReceiptLong,
                stringResource(R.string.settings_categories),
                stringResource(R.string.category_management_tip),
                modifier = Modifier.testTag("action_categories")
            ) { onCategories() }
            SettingsRow(Icons.Default.UploadFile, stringResource(R.string.settings_import), stringResource(R.string.import_support)) {
                importLauncher.launch(arrayOf("text/*", "text/csv", "application/vnd.ms-excel", "application/octet-stream"))
            }
        }
        item {
            CardBlock {
                Text(stringResource(R.string.import_title), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.import_support), color = Color(0xFF65706D), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.import_format), color = Color(0xFF2E7D74), style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.settings_privacy), fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.privacy_desc), color = Color(0xFF65706D), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
