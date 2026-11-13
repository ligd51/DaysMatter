package com.jinian.anniversary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jinian.anniversary.data.backup.BackupManager
import com.jinian.anniversary.data.backup.BackupPayload
import com.jinian.anniversary.data.settings.AppSettings
import com.jinian.anniversary.data.settings.ThemeColor
import com.jinian.anniversary.data.settings.ThemeMode
import com.jinian.anniversary.domain.model.Anniversary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JinianViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as JinianApplication).container
    private val backupManager = container.backupManager

    val events: StateFlow<List<Anniversary>> = container.anniversaryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    init {
        viewModelScope.launch {
            container.anniversaryRepository.repairLegacyEventIds()
            container.reminderScheduler.rescheduleAll(container.anniversaryRepository.getAll())
        }
    }

    fun consumeMessage() {
        _uiMessage.value = null
    }

    fun save(
        event: Anniversary,
        original: Anniversary?,
        onSaved: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val normalized = event.copy(
                createdAt = original?.createdAt ?: now,
                updatedAt = now,
                sortOrder = original?.sortOrder ?: 0L,
            )
            original?.let { container.reminderScheduler.cancel(it) }
            val id = container.anniversaryRepository.save(normalized)
            normalized.reminderOffsets.forEach {
                container.reminderScheduler.schedule(normalized.copy(id = id), it)
            }
            onSaved(id)
        }
    }

    fun move(eventId: Long, direction: Int) {
        viewModelScope.launch {
            container.anniversaryRepository.move(eventId, direction)
        }
    }

    fun delete(event: Anniversary) {
        viewModelScope.launch {
            container.reminderScheduler.cancel(event)
            container.anniversaryRepository.delete(event.id)
        }
    }

    fun restore(event: Anniversary) {
        viewModelScope.launch {
            container.anniversaryRepository.restore(event)
            event.reminderOffsets.forEach { offset ->
                container.reminderScheduler.schedule(event, offset)
            }
        }
    }

    fun setThemeColor(color: ThemeColor) {
        viewModelScope.launch { container.settingsRepository.setThemeColor(color) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { container.settingsRepository.setThemeMode(mode) }
    }

    suspend fun exportJson(): Result<String> = runCatching {
        backupManager.exportJson()
    }.onFailure { _uiMessage.value = it.message ?: "导出失败" }

    fun validateImport(content: String): Result<BackupPayload> =
        backupManager.validate(content).onFailure {
            _uiMessage.value = it.message ?: "备份校验失败"
        }

    fun import(payload: BackupPayload, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                backupManager.import(payload)
                container.reminderScheduler.rescheduleAll(container.anniversaryRepository.getAll())
            }.onSuccess {
                _uiMessage.value = "导入成功"
                onDone()
            }.onFailure {
                _uiMessage.value = it.message ?: "导入失败"
            }
        }
    }
}