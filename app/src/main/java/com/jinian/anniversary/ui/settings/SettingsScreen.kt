package com.jinian.anniversary.ui.settings

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jinian.anniversary.JinianViewModel
import com.jinian.anniversary.data.backup.BackupPayload
import com.jinian.anniversary.data.settings.ThemeColor
import com.jinian.anniversary.data.settings.ThemeMode
import com.jinian.anniversary.ui.components.InlineIcon
import com.jinian.anniversary.ui.components.StreamingText
import com.jinian.anniversary.ui.components.TonalOptionChip
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: JinianViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val message by viewModel.uiMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<BackupPayload?>(null) }

    val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    var notificationsEnabled by remember { mutableStateOf(notificationManager.areNotificationsEnabled()) }
    var exactAlarmEnabled by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notificationsEnabled = notificationManager.areNotificationsEnabled() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = viewModel.exportJson().getOrThrow()
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("无法写入备份文件")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val content = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                } ?: error("无法读取备份文件")
                viewModel.validateImport(content).getOrThrow()
            }.onSuccess { payload -> pendingImport = payload }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 20.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                InlineIcon(icon = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
            }
            StreamingText(
                text = "设置",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 14.dp),
            )
        }

        Card("主题色") {
            ThemeColor.entries.forEachIndexed { index, color ->
                if (index % 2 == 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                        ColorOption(
                            color = ThemeColor.entries[index],
                            selected = settings.themeColor == color,
                            onClick = { viewModel.setThemeColor(color) },
                            modifier = Modifier.weight(1f),
                        )
                        val next = ThemeColor.entries.getOrNull(index + 1)
                        if (next != null) {
                            ColorOption(
                                color = next,
                                selected = settings.themeColor == next,
                                onClick = { viewModel.setThemeColor(next) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        Card("外观") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 14.dp),
            ) {
                listOf(
                    ThemeMode.SYSTEM to "跟随系统",
                    ThemeMode.LIGHT to "浅色",
                    ThemeMode.DARK to "暗色",
                ).forEach { (mode, label) ->
                    TonalOptionChip(
                        selected = settings.themeMode == mode,
                        label = label,
                        onClick = { viewModel.setThemeMode(mode) },
                    )
                }
            }
        }

        Card("权限") {
            PermissionRow(
                title = "通知权限",
                description = "Android 13 及以上需要授权后才能显示提醒。",
                enabled = notificationsEnabled,
                buttonText = if (notificationsEnabled) "已开启" else "去授权",
                onClick = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        notificationsEnabled = notificationManager.areNotificationsEnabled()
                    }
                },
            )
            PermissionRow(
                title = "精准提醒",
                description = "关闭后仍会使用非精准提醒，可能延迟几分钟。",
                enabled = exactAlarmEnabled,
                buttonText = if (exactAlarmEnabled) "已开启" else "去设置",
                onClick = {
                    if (Build.VERSION.SDK_INT >= 31) {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                    }
                    exactAlarmEnabled = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()
                },
            )
            Text(
                text = "部分系统会在电池优化或强制停止后暂停闹钟，请在系统设置中允许自启动并保持应用未停止。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp),
            )
        }

        Card("备份") {
            BackupAction("导出备份") { exportLauncher.launch("记念-backup-${java.time.LocalDate.now()}.json") }
            BackupAction("导入备份", destructive = true) {
                importLauncher.launch(arrayOf("application/json"))
            }
            Text(
                text = "导入前会完整校验 JSON；确认后替换全部事件与主题设置。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Card("关于") {
            Text("记念 · Android MVP", style = MaterialTheme.typography.titleMedium)
            Text(
                "本地应用，无需登录，不请求网络权限。数据保存在本机。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(24.dp),
    )
    message?.let { text ->
        LaunchedEffect(text) {
            snackbarHostState.showSnackbar(text)
            viewModel.consumeMessage()
        }
    }

    pendingImport?.let { payload ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("确认导入") },
            text = { Text("将替换现有全部数据，是否继续？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.import(payload) { pendingImport = null }
                        pendingImport = null
                    },
                ) { Text("替换全部") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(22.dp),
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
    Spacer(modifier = Modifier.height(18.dp))
}

@Composable
private fun ColorOption(
    color: ThemeColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(android.graphics.Color.parseColor(color.hex).let { androidx.compose.ui.graphics.Color(it) }),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(color.label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = color.hex,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    enabled: Boolean,
    buttonText: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 18.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        TonalOptionChip(
            selected = enabled,
            label = buttonText,
            onClick = onClick,
        )
    }
}

@Composable
private fun BackupAction(label: String, destructive: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}