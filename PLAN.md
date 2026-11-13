# 记念 Android 项目计划

## 1. 项目定位

「记念」是一款 Android 原生纪念日管理应用，面向 Android 8.0+ 设备。应用本地保存数据，不要求登录，不请求网络权限。

- 应用名：记念
- 包名：`com.jinian.anniversary`
- 最低支持：Android 8.0（minSdk 26）
- 目标 SDK：36
- 编译 SDK：37
- 当前版本：v1.0.0

## 2. 技术选型

| 类型 | 技术 |
| --- | --- |
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 导航 | Navigation Compose |
| 数据库 | Room |
| 本地设置 | DataStore Preferences |
| 提醒 | AlarmManager + BroadcastReceiver + Notification |
| 农历转换 | `cn.6tail:lunar:1.7.8` |
| 序列化 | kotlinx.serialization |
| 构建 | Gradle 9.9 / AGP 9.5.1 / JDK 17 |

## 3. 项目结构

```text
DaysMatter/
├─ app/
│  ├─ src/main/java/com/jinian/anniversary/
│  │  ├─ MainActivity.kt
│  │  ├─ JinianApplication.kt
│  │  ├─ JinianViewModel.kt
│  │  ├─ data/
│  │  │  ├─ backup/     # JSON 备份导入导出与校验
│  │  │  ├─ local/      # Room Entity、DAO、Database、Migration
│  │  │  ├─ repository/ # 纪念日数据仓库
│  │  │  └─ settings/   # 主题与外观设置
│  │  ├─ domain/
│  │  │  ├─ date/       # 农历转换、周年与倒计时计算
│  │  │  └─ model/      # 领域模型
│  │  ├─ reminder/      # 提醒调度、通知、系统广播 Receiver
│  │  └─ ui/
│  │     ├─ components/ # 输入框、流式文字、通用组件
│  │     ├─ detail/     # 详情页
│  │     ├─ editor/     # 新建与编辑页
│  │     ├─ home/       # 首页列表
│  │     ├─ settings/   # 设置页
│  │     └─ theme/      # 8 套主题、浅色/暗色排版
│  ├─ src/test/         # JVM 单元测试
│  └─ src/androidTest/  # Room instrumented 测试
├─ gradle/libs.versions.toml
└─ app/schemas/         # Room schema
```

## 4. 当前功能范围

### 已实现

- 纪念日新增、编辑、删除、详情查看。
- 标题、分类、备注、日期类型、提醒偏移、提醒时间、每年重复。
- 阳历与农历双类型。
- 农历闰月支持，不存在的农历三十自动回退到当月最后一天。
- 年度农历事件跨春节计算下一周年。
- 阳历 2 月 29 日在平年回退到 2 月 28 日。
- 倒计时、正计时、当天、第 X 天、N 周年展示。
- 年度事件额外显示“下个周年还有 Y 天”。
- 多提醒偏移：30、7、3、1、0 天与自定义偏移。
- 提醒时间设置，默认 09:00。
- AlarmManager 精准/非精准提醒。
- 开机、时间变化、时区变化、应用更新、精准权限变化后重排提醒。
- 长按卡片拖动排序，顺序持久化到 Room。
- 滑动删除与撤销。
- 8 套主题、浅色/暗色/跟随系统。
- JSON 导入导出，导入前完整校验并二次确认替换。
- 本地 Room 数据库持久化。
- 无登录、无网络权限。

### 暂不实现

- iOS / 桌面端
- 登录与云同步
- WebDAV
- 桌面小组件
- 图片分享
- 回收站

## 5. 数据模型

Room 表：`anniversaries`

| 字段 | 说明 |
| --- | --- |
| `id` | 自增主键 |
| `title` | 标题 |
| `dateType` | `SOLAR` 或 `LUNAR` |
| `originalSolarEpochDay` | 原始公历日期 |
| `lunarYear` | 农历年 |
| `lunarMonth` | 农历月 |
| `lunarDay` | 农历日 |
| `lunarIsLeap` | 是否闰月 |
| `category` | 分类 |
| `notes` | 备注 |
| `repeatYearly` | 是否每年重复 |
| `reminderOffsets` | 提前提醒天数，逗号分隔 |
| `reminderHour` | 提醒小时 |
| `reminderMinute` | 提醒分钟 |
| `createdAt` | 创建时间 |
| `updatedAt` | 更新时间 |
| `sortOrder` | 首页自定义排序 |

数据库版本：2。

迁移：

```text
1 -> 2：新增 sortOrder INTEGER NOT NULL DEFAULT 0
```

## 6. UI 页面

### 首页

- 品牌标题与事件数量。
- 卡片显示标题、分类、公历日期、农历日期、大数字天数。
- 支持点击进入详情。
- 支持长按拖动排序。
- 支持横滑删除与撤销。
- 底部 FAB 新建纪念日。

### 新建 / 编辑页

- 阳历 / 农历分段切换。
- 阳历 DatePicker。
- 农历年、月、日动态选择。
- 闰月开关，仅在目标农历年存在该闰月时可用。
- 标题、分类、备注。
- 分类快捷项：纪念日、生日、节日、旅行、其他。
- 每年重复开关。
- 提醒偏移多选与自定义偏移。
- 提醒时间 TimePicker。

### 详情页

- 标题、分类。
- 大数字天数与周年信息。
- 公历日期、农历日期、录入类型。
- 提醒规则。
- 备注。
- 复制日期信息。
- 删除。
- 编辑入口。

### 设置页

- 8 套主题。
- 浅色 / 暗色 / 跟随系统。
- 通知权限状态与跳转。
- 精准提醒权限状态与跳转。
- JSON 备份导出与导入。
- 关于信息。

## 7. 主题与设计规范

提供 8 套主题：

1. 蕉红 `#D4042D`
2. 暖阳橙 `#DB7A0E`
3. 矢车菊蓝 `#5A92E5`
4. 翠微 `#5E8034`
5. 碧青 `#41B5C2`
6. 柠檬黄 `#FACA2E`
7. 梅洛粉 `#EB6D98`
8. 皇家紫 `#722169`

设计规则：

- 禁用纯黑和明显边框。
- 浅色背景：`#f5f5f3` / `#fcfcfb`。
- 暗色背景：`#0b0b0a` / `#151515` / `#1a1a19` / `#20201e`。
- 暗色模式字重下降 40，例如 400 -> 360。
- 数字使用 `tnum` 等宽数字。
- 标题使用逐字流式显现。
- 长按拖动与滑动删除使用过阻尼弹簧。
- 保存 / 复制反馈使用欠阻尼弹簧。
- 复制成功 2.5 秒后自动恢复。

## 8. 提醒机制

- Manifest 声明：
  - `POST_NOTIFICATIONS`
  - `SCHEDULE_EXACT_ALARM`
  - `RECEIVE_BOOT_COMPLETED`
- Android 13+ 需要用户授权通知。
- Android 12+ 建议授权精准闹钟。
- 有精准权限时使用 `setExactAndAllowWhileIdle()`。
- 无精准权限时回退 `setAndAllowWhileIdle()`。
- 使用 `eventId + offsetDays` 生成稳定 PendingIntent。
- 提醒触发后自动安排下一次年度提醒。
- 开机、时间变化、时区变化、应用更新、精准权限变化后重排。

## 9. 备份策略

- 导出格式：JSON。
- 文件名：`记念-backup-YYYY-MM-DD.json`。
- 备份包含 `schemaVersion`、导出时间、事件列表和主题设置。
- 导入前校验版本、必填字段、日期合法性、农历一致性、提醒规则、时间戳和排序值。
- 导入需二次确认。
- 确认后在 Room 事务中替换全部事件数据。
- 导入成功后重排提醒。

## 10. 构建与测试

构建命令：

```powershell
$env:JAVA_HOME='D:\ProgramData\Codex\Tools\jdk-17'
$env:ANDROID_HOME='D:\ProgramData\Codex\Tools\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:GRADLE_USER_HOME='D:\ProgramData\Codex\Tools\gradle-cache'
.\gradlew.bat test lint assembleDebug --no-daemon
```

产物：

```text
app/build/outputs/apk/debug/app-debug.apk
```

当前已覆盖测试：

- 农历转换
- 闰月回退
- 农历三十回退
- 倒计时 / 正计时 / 当天
- 周年计算
- 阳历 2 月 29 日回退
- 提醒滚动
- 稳定 PendingIntent 请求码
- JSON 备份校验

## 11. 风险与限制

- 部分国产 ROM 会在电池优化或强制停止后暂停闹钟。
- 强制停止后系统不保证 AlarmManager 触发。
- 用户拒绝通知权限后，提醒仍会触发但不会显示通知。
- 用户拒绝精准提醒权限后，提醒可能延迟。
- 当前为 MVP，不包含云同步与多设备同步。