package com.jinian.anniversary.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jinian.anniversary.JinianViewModel
import com.jinian.anniversary.domain.date.AnniversaryCalculator
import com.jinian.anniversary.domain.date.LunarDateConverter
import com.jinian.anniversary.domain.model.Anniversary
import com.jinian.anniversary.domain.model.AnniversarySnapshot
import com.jinian.anniversary.ui.components.InlineIcon
import com.jinian.anniversary.ui.components.NumericText
import com.jinian.anniversary.ui.components.StreamingText
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    viewModel: JinianViewModel,
    onAdd: () -> Unit,
    onEvent: (Long) -> Unit,
    onSettings: () -> Unit,
) {
    val events by viewModel.events.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val converter = remember { LunarDateConverter() }
    val calculator = remember { AnniversaryCalculator(converter) }
    val today = remember { LocalDate.now() }
    val listState = rememberLazyListState()
    var draggingEventId by remember { mutableStateOf<Long?>(null) }

    val displays = remember(events, today) {
        events.map { event ->
            val snapshot = calculator.snapshot(event, today)
            Triple(event, snapshot, snapshot.nextAnniversaryDate ?: snapshot.originalDate)
        }.sortedBy { it.first.sortOrder }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 24.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        StreamingText(
                            text = "记念",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = if (events.isEmpty()) "0 个珍藏的日子" else "${events.size} 个珍藏的日子 · 长按拖动排序",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable(onClick = onSettings),
                        contentAlignment = Alignment.Center,
                    ) {
                        InlineIcon(icon = Icons.Rounded.Settings, contentDescription = "设置")
                    }
                }
            }

            if (displays.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 120.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("还没有纪念日", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "添加生日、周年、旅行或任何想记住的日子。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp, bottom = 24.dp),
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable(onClick = onAdd)
                                .padding(horizontal = 26.dp, vertical = 14.dp),
                        ) {
                            Text("新建纪念日", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }

            items(displays, key = { it.first.id }) { display ->
                AnniversaryCard(
                    event = display.first,
                    snapshot = display.second,
                    lunarLabel = converter.format(display.first.lunarDate),
                    today = today,
                    listState = listState,
                    isDragging = draggingEventId == display.first.id,
                    onDragStart = { draggingEventId = display.first.id },
                    onDragMove = { direction -> viewModel.move(display.first.id, direction) },
                    onDragEnd = { draggingEventId = null },
                    onClick = { onEvent(display.first.id) },
                    onDelete = {
                        viewModel.delete(display.first)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "已删除「${display.first.title}」",
                                actionLabel = "撤销",
                                duration = androidx.compose.material3.SnackbarDuration.Long,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restore(display.first)
                            }
                        }
                    },
                )
            }
        }

        FloatingActionButton(
            onClick = onAdd,
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(68.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "新增纪念日", modifier = Modifier.size(30.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 104.dp),
        )
    }
}

@Composable
private fun AnniversaryCard(
    event: Anniversary,
    snapshot: AnniversarySnapshot,
    lunarLabel: String,
    today: LocalDate,
    listState: LazyListState,
    isDragging: Boolean,
    onDragStart: () -> Unit,
    onDragMove: (Int) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val offsetX = remember { Animatable(0f) }
    val verticalOffset = remember(event.id) { Animatable(0f) }
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    val cardWidth = with(LocalDensity.current) { 360.dp.toPx() }
    val isOccurrenceToday = snapshot.nextAnniversaryDate == today
    val isToday = if (event.repeatYearly) isOccurrenceToday else snapshot.isToday
    val mainText = when {
        isToday -> "就是今天"
        snapshot.isFuture -> "还有 ${snapshot.signedDays} 天"
        else -> "已经 ${-snapshot.signedDays} 天"
    }
    val dayNumber = if (event.repeatYearly) snapshot.daysUntilNextAnniversary ?: 0L else snapshot.inclusiveDayNumber
    val yearLabel = when {
        snapshot.completedYears > 0 -> "${snapshot.completedYears} 周年"
        !snapshot.isFuture -> "未满 1 周年"
        else -> ""
    }

    Box(modifier = modifier.fillMaxWidth().zIndex(if (isDragging) 1f else 0f)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(end = 24.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            InlineIcon(icon = Icons.Rounded.DeleteOutline, contentDescription = "删除")
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = offsetX.value
                    translationY = verticalOffset.value
                    scaleX = if (isDragging) 1.03f else 1f
                    scaleY = if (isDragging) 1.03f else 1f
                    shadowElevation = if (isDragging) with(density) { 18.dp.toPx() } else 0f
                }
                .pointerInput(event.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDragStart()
                            scope.launch { verticalOffset.snapTo(0f) }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch { verticalOffset.snapTo(verticalOffset.value + dragAmount.y) }
                            val itemHeight = listState.layoutInfo.visibleItemsInfo
                                .firstOrNull { it.key == event.id }?.size?.toFloat() ?: 1f
                            if (kotlin.math.abs(verticalOffset.value) >= itemHeight * 0.55f) {
                                onDragMove(if (verticalOffset.value > 0f) 1 else -1)
                                scope.launch { verticalOffset.snapTo(0f) }
                            }
                        },
                        onDragEnd = {
                            onDragEnd()
                            scope.launch {
                                verticalOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(dampingRatio = 1f, stiffness = 1200f),
                                )
                            }
                        },
                        onDragCancel = {
                            onDragEnd()
                            scope.launch {
                                verticalOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(dampingRatio = 1f, stiffness = 1200f),
                                )
                            }
                        },
                    )
                }
                .pointerInput(event.id) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = true },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                        },
                        onDragEnd = {
                            dragging = false
                            scope.launch {
                                if (kotlin.math.abs(offsetX.value) > cardWidth * 0.35f) {
                                    offsetX.animateTo(
                                        targetValue = -cardWidth,
                                        animationSpec = spring(dampingRatio = 1f, stiffness = 1200f),
                                    )
                                    onDelete()
                                } else {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(dampingRatio = 1f, stiffness = 1200f),
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            dragging = false
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(dampingRatio = 1f, stiffness = 1200f),
                                )
                            }
                        },
                    )
                }
                .background(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(28.dp))
                .clickable(onClick = onClick),
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StreamingText(
                        text = event.title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (event.category.isNotBlank()) {
                        Text(
                            text = event.category,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }

                Text(
                    text = "公历 ${snapshot.originalDate.year} 年 ${snapshot.originalDate.monthValue} 月 ${snapshot.originalDate.dayOfMonth} 日",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    text = lunarLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(top = 14.dp),
                ) {
                    NumericText(
                        text = if (isToday) "今天" else dayNumber.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = if (isToday) "" else "天",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = mainText,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (yearLabel.isNotEmpty()) {
                            Text(
                                text = yearLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }

                if (event.repeatYearly && snapshot.daysUntilNextAnniversary != null && !isToday) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InlineIcon(
                            icon = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        NumericText(
                            text = "下个周年还有 ${snapshot.daysUntilNextAnniversary} 天",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}