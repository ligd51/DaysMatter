package com.jinian.anniversary.ui.editor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jinian.anniversary.JinianViewModel
import com.jinian.anniversary.domain.date.LunarDateConverter
import com.jinian.anniversary.domain.model.Anniversary
import com.jinian.anniversary.domain.model.DateType
import com.jinian.anniversary.ui.components.InlineIcon
import com.jinian.anniversary.ui.components.NoBorderTextField
import com.jinian.anniversary.ui.components.StreamingText
import com.jinian.anniversary.ui.components.TonalOptionChip
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val CATEGORY_OTHER = "其他"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: JinianViewModel,
    eventId: Long,
    formResetKey: String = "editor-$eventId",
    onBack: () -> Unit,
) {
    val events by viewModel.events.collectAsState()
    val original = if (eventId > 0L) events.firstOrNull { it.id == eventId } else null
    val converter = remember { LunarDateConverter() }
    val formKey = formResetKey
    val today = remember { LocalDate.now() }
    val defaultLunar = remember(today, converter) { converter.solarToLunar(today) }

    var title by remember(formKey) { mutableStateOf(original?.title ?: "") }
    var category by remember(formKey) { mutableStateOf(original?.category ?: "纪念日") }
    var notes by remember(formKey) { mutableStateOf(original?.notes ?: "") }
    var dateType by remember(formKey) { mutableStateOf(original?.dateType ?: DateType.SOLAR) }
    var solarDate by remember(formKey) {
        mutableStateOf(original?.let { LocalDate.ofEpochDay(it.originalSolarEpochDay) } ?: today)
    }
    var lunarDate by remember(formKey) {
        mutableStateOf(original?.lunarDate ?: defaultLunar)
    }
    var repeatYearly by remember(formKey) { mutableStateOf(original?.repeatYearly ?: true) }
    var reminderOffsets by remember(formKey) {
        mutableStateOf(original?.reminderOffsets ?: listOf(0))
    }
    var reminderHour by remember(formKey) { mutableStateOf(original?.reminderHour ?: 9) }
    var reminderMinute by remember(formKey) { mutableStateOf(original?.reminderMinute ?: 0) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var customOffset by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    val monthOptions = remember(lunarDate.year) { converter.monthOptions(lunarDate.year) }
    val dayCount = remember(lunarDate.year, lunarDate.month, lunarDate.isLeap) {
        converter.dayCount(lunarDate.year, lunarDate.month, lunarDate.isLeap)
    }

    LaunchedEffect(lunarDate.year, lunarDate.month) {
        val lunarYear = LunarDateConverter().let { it }
        val options = converter.monthOptions(lunarDate.year)
        if (lunarDate.isLeap && options.none { it.month == lunarDate.month && it.isLeap }) {
            lunarDate = lunarDate.copy(isLeap = false)
        }
    }
    LaunchedEffect(dayCount) {
        if (lunarDate.day > dayCount) lunarDate = lunarDate.copy(day = dayCount)
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = solarDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
        yearRange = LunarDateConverter.MIN_LUNAR_YEAR..LunarDateConverter.MAX_LUNAR_YEAR,
    )
    val timePickerState = rememberTimePickerState(
        initialHour = reminderHour,
        initialMinute = reminderMinute,
        is24Hour = true,
    )

    fun switchDateType(newType: DateType) {
        if (newType == dateType) return
        if (newType == DateType.LUNAR) {
            lunarDate = converter.solarToLunar(solarDate)
        } else {
            solarDate = converter.lunarToSolar(converter.normalize(lunarDate))
        }
        dateType = newType
    }

    fun toggleOffset(offset: Int) {
        reminderOffsets = if (offset in reminderOffsets) {
            reminderOffsets.filterNot { it == offset }
        } else {
            (reminderOffsets + offset).distinct().sortedDescending()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .imePadding()
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
                text = if (eventId == -1L) "新建纪念日" else "编辑纪念日",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 14.dp),
            )
        }

        SectionTitle("标题")
        NoBorderTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = "例如：结婚纪念日",
            modifier = Modifier.padding(top = 10.dp),
        )

        SectionTitle("类型")
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 10.dp),
        ) {
            TonalOptionChip(
                selected = dateType == DateType.SOLAR,
                label = "阳历",
                onClick = { switchDateType(DateType.SOLAR) },
            )
            TonalOptionChip(
                selected = dateType == DateType.LUNAR,
                label = "农历",
                onClick = { switchDateType(DateType.LUNAR) },
            )
        }

        if (dateType == DateType.SOLAR) {
            val formatter = DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日", java.util.Locale.CHINA)
            SectionTitle("阳历日期")
            PickerField(
                label = solarDate.format(formatter),
                onClick = { showDatePicker = true },
                modifier = Modifier.padding(top = 10.dp),
            )
        } else {
            SectionTitle("农历年份")
            PickerField(
                label = "${lunarDate.year} 年",
                onClick = { showDatePicker = false },
                options = (LunarDateConverter.MIN_LUNAR_YEAR..LunarDateConverter.MAX_LUNAR_YEAR).map(Int::toString),
                selected = lunarDate.year.toString(),
                onSelect = { lunarDate = lunarDate.copy(year = it.toInt()) },
                modifier = Modifier.padding(top = 10.dp),
            )
            Row(modifier = Modifier.padding(top = 14.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionTitle("月份")
                    PickerField(
                        label = monthOptions.firstOrNull { it.month == lunarDate.month && it.isLeap == lunarDate.isLeap }?.label
                            ?: "${lunarDate.month} 月",
                        options = monthOptions.map { it.label },
                        selected = monthOptions.firstOrNull { it.month == lunarDate.month && it.isLeap == lunarDate.isLeap }?.label,
                        onSelect = { selected ->
                            val option = monthOptions.first { it.label == selected }
                            lunarDate = lunarDate.copy(month = option.month, isLeap = option.isLeap)
                        },
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SectionTitle("日期")
                    val dayOptions = (1..dayCount).map(Int::toString)
                    PickerField(
                        label = "${lunarDate.day} 日",
                        options = dayOptions,
                        selected = lunarDate.day.toString(),
                        onSelect = { lunarDate = lunarDate.copy(day = it.toInt()) },
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 14.dp),
            ) {
                Text("该年有闰月", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = lunarDate.isLeap,
                    onCheckedChange = { checked ->
                        val available = monthOptions.any { it.month == lunarDate.month && it.isLeap }
                        if (available) lunarDate = lunarDate.copy(isLeap = checked)
                    },
                )
            }
        }

        SectionTitle("分类")
        NoBorderTextField(
            value = category,
            onValueChange = { category = it },
            placeholder = "自由输入分类",
            modifier = Modifier.padding(top = 10.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            listOf("纪念日", "生日", "节日", "旅行", CATEGORY_OTHER).forEach { option ->
                TonalOptionChip(
                    selected = category == option,
                    label = option,
                    onClick = { category = option },
                    modifier = Modifier.width(0.dp),
                )
            }
        }

        SectionTitle("备注")
        NoBorderTextField(
            value = notes,
            onValueChange = { notes = it },
            placeholder = "可记录地点、意义或礼物灵感",
            singleLine = false,
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.padding(top = 10.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 22.dp),
        ) {
            Column {
                Text("每年重复", style = MaterialTheme.typography.titleMedium)
                Text(
                    "按原始日的年度对应日提醒与展示",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = repeatYearly, onCheckedChange = { repeatYearly = it })
        }

        SectionTitle("提前提醒")
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
        ) {
            listOf(30, 7, 3, 1, 0).forEach { offset ->
                TonalOptionChip(
                    selected = offset in reminderOffsets,
                    label = if (offset == 0) "当天" else "提前 $offset 天",
                    onClick = { toggleOffset(offset) },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            NoBorderTextField(
                value = customOffset,
                onValueChange = { value ->
                    customOffset = value.filter(Char::isDigit).take(4)
                },
                placeholder = "自定义提前天数",
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = {
                    val value = customOffset.toIntOrNull()
                    if (value != null && value in 0..3650) {
                        toggleOffset(value)
                        customOffset = ""
                    }
                },
                modifier = Modifier.height(52.dp),
            ) {
                Text("添加")
            }
        }
        if (reminderOffsets.isNotEmpty()) {
            Text(
                "已选：${reminderOffsets.joinToString("、") { if (it == 0) "当天" else "提前 $it 天" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        SectionTitle("提醒时间")
        PickerField(
            label = "%02d:%02d".format(java.util.Locale.CHINA, reminderHour, reminderMinute),
            onClick = { showTimePicker = true },
            modifier = Modifier.padding(top = 10.dp),
        )

        Spacer(modifier = Modifier.height(34.dp))
        Button(
            onClick = {
                if (title.isBlank() || saving) return@Button
                saving = true
                val effectiveSolar = if (dateType == DateType.SOLAR) {
                    solarDate
                } else {
                    converter.lunarToSolar(converter.normalize(lunarDate))
                }
                val event = Anniversary(
                    id = if (eventId == -1L) 0L else eventId,
                    title = title.trim(),
                    dateType = dateType,
                    originalSolarEpochDay = effectiveSolar.toEpochDay(),
                    lunarDate = converter.solarToLunar(effectiveSolar),
                    category = category.trim(),
                    notes = notes.trim(),
                    repeatYearly = repeatYearly,
                    reminderOffsets = reminderOffsets.distinct().sortedDescending(),
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    createdAt = original?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
                viewModel.save(event, original) { onBack() }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(22.dp),
        ) {
            if (saving) {
                val scale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 380f),
                    label = "save_check",
                )
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "已保存",
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale },
                )
            } else {
                Text("保存", style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(modifier = Modifier.height(36.dp))
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val instant = java.time.Instant.ofEpochMilli(millis)
                        solarDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    reminderHour = timePickerState.hour
                    reminderMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 22.dp),
    )
}

@Composable
private fun PickerField(
    label: String,
    onClick: (() -> Unit)? = null,
    options: List<String>? = null,
    selected: String? = null,
    onSelect: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (options == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(enabled = onClick != null) { onClick?.invoke() }
                .padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        return
    }

    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect?.invoke(option)
                        expanded = false
                    },
                )
            }
        }
    }
}