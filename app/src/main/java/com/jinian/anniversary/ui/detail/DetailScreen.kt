package com.jinian.anniversary.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jinian.anniversary.JinianViewModel
import com.jinian.anniversary.domain.date.AnniversaryCalculator
import com.jinian.anniversary.domain.date.LunarDateConverter
import com.jinian.anniversary.ui.components.InlineIcon
import com.jinian.anniversary.ui.components.NumericText
import com.jinian.anniversary.ui.components.StreamingText
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DetailScreen(
    viewModel: JinianViewModel,
    eventId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val events by viewModel.events.collectAsState()
    val event = events.firstOrNull { it.id == eventId }
    val converter = remember { LunarDateConverter() }
    val calculator = remember { AnniversaryCalculator(converter) }
    val today = remember { LocalDate.now() }
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2500)
            copied = false
        }
    }

    if (event == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackButton(onBack)
                Text(
                    text = "纪念日不存在",
                    modifier = Modifier.padding(start = 14.dp),
                )
            }
        }
        return
    }

    val snapshot = calculator.snapshot(event, today)
    val solarText = "${snapshot.originalDate.year} 年 ${snapshot.originalDate.monthValue} 月 ${snapshot.originalDate.dayOfMonth} 日"
    val lunarText = converter.format(event.lunarDate)

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
            BackButton(onBack)
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { onEdit(event.id) },
                contentAlignment = Alignment.Center,
            ) {
                InlineIcon(icon = Icons.Rounded.Edit, contentDescription = "编辑")
            }
        }

        StreamingText(
            text = event.title,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = event.category.ifBlank { "未分类" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(22.dp),
        ) {
            Column {
                val mainDays = snapshot.daysUntilNextAnniversary ?: kotlin.math.abs(snapshot.signedDays)
                val mainLabel = when {
                    snapshot.nextAnniversaryDate == today -> "就是今天"
                    snapshot.isFuture -> "还有 ${snapshot.signedDays} 天"
                    else -> "已经 ${-snapshot.signedDays} 天"
                }
                Text(mainLabel, style = MaterialTheme.typography.titleMedium)
                NumericText(
                    text = if (snapshot.nextAnniversaryDate == today) "今天" else mainDays.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (snapshot.completedYears > 0) "${snapshot.completedYears} 周年" else "未满 1 周年",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (snapshot.isToday || snapshot.nextAnniversaryDate == today) {
                    NumericText(
                        text = "第 ${snapshot.inclusiveDayNumber} 天",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        InfoCard(
            title = "日期信息",
            lines = listOf(
                "公历 $solarText",
                lunarText,
                if (event.dateType == com.jinian.anniversary.domain.model.DateType.LUNAR) "录入类型：农历" else "录入类型：阳历",
            ),
        )
        InfoCard(
            title = "提醒",
            lines = listOf(
                "提醒时间：%02d:%02d".format(java.util.Locale.CHINA, event.reminderHour, event.reminderMinute),
                if (event.reminderOffsets.isEmpty()) "未设置提醒"
                else event.reminderOffsets.joinToString("、") { if (it == 0) "当天" else "提前 $it 天" },
                if (event.repeatYearly) "每年重复" else "仅提醒一次",
            ),
        )
        if (event.notes.isNotBlank()) {
            InfoCard(title = "备注", lines = listOf(event.notes))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 22.dp, bottom = 40.dp),
        ) {
            ActionButton(
                label = if (copied) "已复制" else "复制日期",
                selected = copied,
                icon = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                modifier = Modifier.weight(1f),
                onClick = {
                    val text = "${event.title}\n公历 $solarText\n$lunarText"
                    context.getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText("jinian", text))
                    copied = true
                },
            )
            ActionButton(
                label = "删除",
                selected = false,
                icon = Icons.Rounded.DeleteOutline,
                modifier = Modifier.weight(1f),
                onClick = {
                    viewModel.delete(event)
                    onBack()
                },
            )
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
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
}

@Composable
private fun InfoCard(title: String, lines: List<String>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(22.dp),
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            lines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) colors.primary else colors.surfaceContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InlineIcon(
                icon = icon,
                contentDescription = null,
                tint = if (selected) colors.onPrimary else colors.onSurface,
            )
            Text(
                text = label,
                color = if (selected) colors.onPrimary else colors.onSurface,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}