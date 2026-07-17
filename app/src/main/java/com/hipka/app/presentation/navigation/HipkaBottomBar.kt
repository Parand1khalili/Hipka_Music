package com.hipka.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.hipka.app.R

private data class BottomNavEntry(
    val screen: Screen,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavEntries: List<BottomNavEntry> = listOf(
    BottomNavEntry(Screen.Home, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavEntry(Screen.Search, R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavEntry(Screen.Downloads, R.string.nav_downloads, Icons.Filled.CloudDownload, Icons.Outlined.CloudDownload),
    BottomNavEntry(Screen.Playlists, R.string.nav_playlists, Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    BottomNavEntry(Screen.Profile, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person)
)

@Composable
fun HipkaBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavEntries.forEach { entry ->
            val selected = currentDestination?.hierarchy?.any { it.route == entry.screen.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(entry.screen.route) {
                        // پاک کردن پشته صفحات تا ریشه اصلی گراف برنامه
                        popUpTo(navController.graph.findStartDestination().id) {
                            // تغییر مهم: وضعیت صفحات فرعی (مثل لایک‌ها) را ذخیره نکن تا هوم ریست شود
                            saveState = false
                        }
                        // جلوگیری از باز شدن چند باره یک صفحه تکراری روی هم
                        launchSingleTop = true
                        // تغییر مهم: وضعیت قبلی را بازیابی نکن تا همیشه به ریشه اصلی تب هدایت شویم
                        restoreState = false
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) entry.selectedIcon else entry.unselectedIcon,
                        contentDescription = stringResource(id = entry.labelRes)
                    )
                },
                label = { Text(text = stringResource(id = entry.labelRes)) }
            )
        }
    }
}