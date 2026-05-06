package com.classapp.schedule.ui.theme

import androidx.compose.ui.graphics.Color

// ── 主色调 ──
val AppBackground = Color(0xFFFFF5F5)       // 浅粉色背景
val AppSurface = Color(0xFFFFFFFF)          // 白色卡片
val AppPrimary = Color(0xFFE03A3A)          // 红色主色
val AppPrimaryDark = Color(0xFF8B3A3A)      // 暗红/棕红
val AppAccent = Color(0xFFA02020)           // 深粉色

// ── 文字颜色 ──
val TextPrimary = Color(0xFF333333)         // 深灰标题
val TextSecondary = Color(0xFF444444)       // 中灰正文
val TextTertiary = Color(0xFF666666)        // 浅灰副文字

// ── 分隔线 ──
val DividerColor = Color(0xFFEEEEEE)

// ── 星期栏 ──
val DaySelected = Color(0xFFE03A3A)
val DayUnselected = Color(0xFF666666)

// ── 极窄屏标签 ──
val NarrowTagBg = Color(0xFFFFE4E4)
val NarrowTagText = Color(0xFFA02020)

// ── 底部导航 ──
val NavSelected = Color(0xFFE03A3A)
val NavUnselected = Color(0xFF666666)

// ── 课程颜色 (低饱和度色块，24 色) ──
val CourseColors = listOf(
    Color(0xFF8AB4F8), // 浅蓝
    Color(0xFFB8CC99), // 浅绿
    Color(0xFFDDA0A0), // 浅粉
    Color(0xFFD4C46A), // 浅黄
    Color(0xFFB0A8D8), // 浅紫
    Color(0xFFF0B888), // 浅橙
    Color(0xFF7BC8C4), // 浅青
    Color(0xFFC8A2C8), // 浅紫红
    Color(0xFFA8C8A0), // 薄荷绿
    Color(0xFFE6A0A0), // 玫瑰
    Color(0xFFA0B8E0), // 雾蓝
    Color(0xFFC4D830), // 黄绿
    Color(0xFFD4A07A), // 驼色
    Color(0xFF9DC183), // 草绿
    Color(0xFF80CBC4), // 青绿
    Color(0xFFE8B0A0), // 杏色
    Color(0xFFA8D8B0), // 嫩绿
    Color(0xFFCF94DA), // 淡紫
    Color(0xFF81D4FA), // 天蓝
    Color(0xFFFFCC80), // 橙黄
    Color(0xFFCE93D8), // 紫丁香
    Color(0xFF80DEEA), // 冰蓝
    Color(0xFFEF9A9A), // 珊瑚粉
    Color(0xFFA5D6A7), // 翠绿
)

// ── 保留兼容：CourseColorBackgrounds (同 CourseColors，用于旧代码兼容) ──
val CourseColorBackgrounds = CourseColors
