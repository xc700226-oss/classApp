package com.classapp.schedule.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.classapp.schedule.data.model.Course
import com.classapp.schedule.ui.course.AddCourseScreen
import com.classapp.schedule.ui.course.CourseViewModel
import com.classapp.schedule.ui.profile.ProfileScreen
import com.classapp.schedule.ui.schedule.ScheduleScreen
import com.classapp.schedule.ui.schoolimport.ImportWebViewScreen
import com.classapp.schedule.ui.settings.SettingsScreen
import com.classapp.schedule.ui.theme.*
import com.google.gson.Gson

object Routes {
    const val SCHEDULE = "schedule"
    const val ADD_COURSE = "add_course/{defaultDay}"
    const val EDIT_COURSE = "edit_course/{courseJson}"
    const val SETTINGS = "settings"
    const val IMPORT_WEBVIEW = "import_webview"
    const val PROFILE = "profile"

    fun addCourse(defaultDay: Int? = null) = "add_course/${defaultDay ?: -1}"
    fun editCourse(course: Course) = "edit_course/${Gson().toJson(course)}"

    // 底部导航 tab 路由
    val bottomNavRoutes = setOf(SCHEDULE, PROFILE)
}

// ── 底部导航 tab 定义 ──
private data class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomNavTabs = listOf(
    BottomNavTab(Routes.SCHEDULE, "课程表", Icons.Default.DateRange),
    BottomNavTab(Routes.PROFILE, "用户", Icons.Default.Person),
)

@Composable
fun AppNavGraph(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.bottomNavRoutes

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(Routes.SCHEDULE) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SCHEDULE,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.SCHEDULE) {
                ScheduleScreen(
                    onAddCourse = { day -> navController.navigate(Routes.addCourse(day)) },
                    onEditCourse = { course -> navController.navigate(Routes.editCourse(course)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(
                route = Routes.ADD_COURSE,
                arguments = listOf(navArgument("defaultDay") { type = NavType.IntType })
            ) { backStackEntry ->
                val defaultDay = backStackEntry.arguments?.getInt("defaultDay")?.let { if (it == -1) null else it }
                val courseViewModel: CourseViewModel = viewModel()
                courseViewModel.initForAdd(defaultDay)
                AddCourseScreen(onBack = { navController.popBackStack() }, viewModel = courseViewModel)
            }

            composable(
                route = Routes.EDIT_COURSE,
                arguments = listOf(navArgument("courseJson") { type = NavType.StringType })
            ) { backStackEntry ->
                val courseJson = backStackEntry.arguments?.getString("courseJson") ?: return@composable
                val course = Gson().fromJson(courseJson, Course::class.java)
                val courseViewModel: CourseViewModel = viewModel()
                courseViewModel.initForEdit(course)
                AddCourseScreen(onBack = { navController.popBackStack() }, viewModel = courseViewModel)
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onImportWebView = { navController.navigate(Routes.IMPORT_WEBVIEW) }
                )
            }

            composable(Routes.IMPORT_WEBVIEW) {
                ImportWebViewScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    onNavigateToImport = { navController.navigate(Routes.IMPORT_WEBVIEW) }
                )
            }
        }
    }
}

// ── 底部导航栏 ──
@Composable
private fun BottomNavBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit
) {
    Surface(
        color = AppBackground,
        tonalElevation = 0.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavTabs.forEach { tab ->
                val isSelected = currentRoute == tab.route
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clickable { onTabSelected(tab.route) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (isSelected) NavSelected else NavUnselected,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tab.label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) NavSelected else NavUnselected
                    )
                }
            }
        }
    }
}
