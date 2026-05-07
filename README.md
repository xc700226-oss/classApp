# ClassApp 课程表

大学生课表管理 Android 应用，基于 Kotlin + Jetpack Compose + Material 3 构建。

## 功能

- **周视图课表** — 按周次展示课程，左右滑动 / 点选快速切换
- **课程管理** — 添加、编辑、删除课程（课程名、教师、教室、节次、周次范围）
- **单双周过滤** — 支持全部 / 单周 / 双周上课
- **节次分组** — 自动按 1-2、3-4、5-6、7-8、9-10 节分组展示
- **本周高亮** — 「今天」按钮一键回到当前周，周次选择器高亮当前周
- **学期配置** — 自定义学期开始日期和总周数，自动计算当前周
- **数据持久化** — Room 本地数据库，离线可用
- **颜色标签** — 10 种配色，课程卡片按颜色区分
- **深色主题** — 跟随系统自动切换浅色 / 深色模式

## 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM (ViewModel + Repository + Flow) |
| 数据库 | Room (SQLite) + KSP |
| 导航 | Navigation Compose |
| 构建 | Gradle Kotlin DSL + Compose BOM 2024.06 |

## 项目结构

```
app/src/main/java/com/classapp/schedule/
├── ClassApp.kt              # Application 入口，持数据库实例
├── MainActivity.kt          # 单 Activity 入口
├── data/
│   ├── model/Course.kt      # 课程实体 (Room @Entity)
│   ├── local/
│   │   ├── CourseDao.kt     # 数据库访问对象 (Room @Dao)
│   │   └── AppDatabase.kt   # Room 数据库定义
│   └── repository/
│       └── ScheduleRepository.kt
├── ui/
│   ├── theme/               # Material 3 主题（颜色、排版）
│   ├── components/
│   │   ├── CourseCard.kt    # 课程卡片组件
│   │   ├── EmptySlot.kt     # 空白占位组件
│   │   └── WeekSelector.kt  # 周次选择器组件
│   ├── schedule/
│   │   ├── ScheduleScreen.kt    # 主课表页面
│   │   └── ScheduleViewModel.kt
│   ├── course/
│   │   ├── AddCourseScreen.kt   # 添加/编辑课程页面
│   │   └── CourseViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt    # 设置页面
│   │   └── SettingsViewModel.kt
│   └── navigation/
│       └── NavGraph.kt          # 页面路由
└── util/
    └── WeekUtils.kt             # 周次计算工具类
```

## 下载

前往 [GitHub Releases](https://github.com/xc700226-oss/classApp/releases) 下载最新 APK 安装包。

> 最低要求：Android 8.0 (API 26)

## 快速开始

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更新版本
- JDK 17
- Android SDK 34

### 构建运行

1. 用 Android Studio 打开 `classApp/` 目录
2. 等待 Gradle Sync 完成
3. 连接设备或启动模拟器（最低 API 26）
4. 点击 Run

或使用命令行：

```bash
cd classApp
./gradlew assembleDebug
```

## 使用说明

1. **首次使用** — 打开「设置」设置学期开始日期
2. **添加课程** — 点击右下角「添加课程」按钮，填写课程信息
3. **切换周次** — 顶部的周次选择器点选，或使用左右箭头
4. **编辑课程** — 点击课表中的课程卡片
5. **删除课程** — 在编辑页面删除，或长按卡片（功能预留）

## 数据模型

```kotlin
Course(
    id: Long,           // 自增主键
    name: String,       // 课程名称
    teacher: String,    // 授课教师
    classroom: String,  // 上课教室
    dayOfWeek: Int,     // 星期 (1=周一 .. 7=周日)
    startSlot: Int,     // 开始节次 (1-based)
    endSlot: Int,       // 结束节次
    weeks: String,      // 上课周次 ("1-13" / "1-4,6,8,10,12,14" / "3" / "1-13(单)")
    colorIndex: Int     // 颜色索引
)
```

## License

MIT
