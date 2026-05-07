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

## 使用教程

### 第一步：配置学期

打开「设置」→ 设置**开始上课时间**（学期第一周周一）和**本学期总周数**。应用会自动根据当前日期计算你处在第几周。

### 第二步：导入课表

App 支持三种方式导入课程：

#### 方式一：从教务系统导入（推荐）

1. 底部 Tab 切换到「用户」→ 点击「从教务系统导入」
2. 应用内嵌浏览器自动加载学校 WebVPN 地址
3. 输入学号和密码登录教务系统（青果/强智）
4. 登录后导航到「学期理论课表」页面
5. 点击底部「解析当前课表」按钮
6. 预览无误后点击「确认导入」

> 需要连接校园网或学校 VPN 才能访问 WebVPN。

#### 方式二：导入 Excel 课表

1. 打开「设置」→ 点击「导入 Excel 课表」
2. 选择 `.xlsx` 格式的课表文件
3. 支持两种 Excel 格式：
   - **列表格式** — 列包含课程名称、教师、教室、星期、节次、周次等
   - **网格格式** — 行为节次、列为星期，单元格内包含课程信息
4. 预览确认后点击「确认导入」

#### 方式三：手动添加课程

1. 在课表页点击空白格，或点击 `+` 按钮
2. 填写课程信息：
   - **课程名称** — 必填
   - **授课教师、上课教室** — 选填
   - **上课日期** — 点击选择星期
   - **上课节次** — 下拉选择开始/结束节次
   - **上课周次** — 填写周次表达式，支持格式：
     - `1-13` — 连续周（第1周到第13周）
     - `3` — 单独一周
     - `1-4,6,8,10,12,14` — 混合模式
     - `1-13(单)` — 单周上课
     - `1-13(双)` — 双周上课
   - **标签颜色** — 区分不同课程

### 日常使用

- **切换周次** — 在课表页左右滑动切换周次
- **查看详情** — 点击课程卡片查看/编辑
- **删除课程** — 点击卡片进入编辑页删除，或通过菜单「清空课表」一键清空

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
