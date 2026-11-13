package com.jinian.anniversary.data.backup

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackupValidatorTest {
    private val validJson = """
        {
          "schemaVersion": 1,
          "exportedAt": "2026-11-10T09:00:00",
          "events": [
            {
              "id": 1,
              "title": "测试",
              "dateType": "SOLAR",
              "originalSolarEpochDay": 20765,
              "lunarYear": 2026,
              "lunarMonth": 9,
              "lunarDay": 30,
              "lunarIsLeap": false,
              "category": "纪念日",
              "notes": "",
              "repeatYearly": true,
              "reminderOffsets": "7,3,0",
              "reminderHour": 9,
              "reminderMinute": 0,
              "createdAt": 1,
              "updatedAt": 1
            }
          ],
          "themeColor": "BANANA_RED",
          "themeMode": "SYSTEM"
        }
    """.trimIndent()

    @Test
    fun `有效备份可以完整解码`() {
        val result = BackupValidator().validate(validJson)
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().events).hasSize(1)
    }

    @Test
    fun `非法 JSON 会失败`() {
        assertThat(BackupValidator().validate("{").isFailure).isTrue()
    }

    @Test
    fun `版本不兼容会失败`() {
        val result = BackupValidator().validate(validJson.replace("\"schemaVersion\": 1", "\"schemaVersion\": 2"))
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `农历字段与公历日期不一致会失败`() {
        val result = BackupValidator().validate(validJson.replace("\"lunarDay\": 30", "\"lunarDay\": 29"))
        assertThat(result.isFailure).isTrue()
    }
}