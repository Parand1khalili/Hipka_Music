package com.hipka.app.presentation.navigation

import android.view.ViewTreeObserver
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.hipka.app.R

private data class BottomNavItem(
    val route: String,
    @StringRes val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// ✨ تشخیص ۱۰۰٪ دقیق کیبورد با استفاده از هسته اندروید (مقاوم در برابر باگ‌های سامسونگ)
@Composable
fun rememberKeyboardOpen(): Boolean {
    val view = LocalView.current
    var isKeyboardOpen by remember { mutableStateOf(false) }

    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val insets = ViewCompat.getRootWindowInsets(view)
            isKeyboardOpen = insets?.isVisible(WindowInsetsCompat.Type.ime()) ?: false
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }
    return isKeyboardOpen
}

@Composable
fun HipkaBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isKeyboardOpen = rememberKeyboardOpen()

    // ✨ ورود با انیمیشن نرم، اما خروج فوری (صفر میلی‌ثانیه) تا کیبورد نتواند آن را به بالا هل دهد
    AnimatedVisibility(
        visible = !isKeyboardOpen,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(durationMillis = 300)
        ),
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(durationMillis = 0) // ✨ زمان صفر برای جلوگیری از آمدن روی کیبورد
        )
    ) {
        val items = listOf(
            BottomNavItem(
                route = Screen.Home.route,
                titleResId = R.string.nav_home,
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home
            ),
            BottomNavItem(
                route = Screen.Search.route,
                titleResId = R.string.nav_search,
                selectedIcon = Icons.Filled.Search,
                unselectedIcon = Icons.Outlined.Search
            ),
            BottomNavItem(
                route = Screen.Downloads.route,
                titleResId = R.string.nav_downloads,
                selectedIcon = Icons.Filled.FileDownload,
                unselectedIcon = Icons.Outlined.FileDownload
            ),
            BottomNavItem(
                route = Screen.Playlists.route,
                titleResId = R.string.nav_playlists,
                selectedIcon = Icons.Filled.LibraryMusic,
                unselectedIcon = Icons.Outlined.LibraryMusic
            ),
            BottomNavItem(
                route = Screen.Profile.route,
                titleResId = R.string.nav_profile,
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person
            )
        )

        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 3.dp
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = stringResource(id = item.titleResId)
                        )
                    },
                    label = {
                        AutoResizedText(
                            text = stringResource(id = item.titleResId),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    selected = selected,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AutoResizedText(
    text: String,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    var resizedTextStyle by remember(text) { mutableStateOf(style) }
    var shouldDraw by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        modifier = modifier.drawWithContent {
            if (shouldDraw) {
                drawContent()
            }
        },
        softWrap = false,
        maxLines = 1,
        style = resizedTextStyle,
        onTextLayout = { result ->
            if (result.hasVisualOverflow) {
                resizedTextStyle = resizedTextStyle.copy(
                    fontSize = resizedTextStyle.fontSize * 0.9f
                )
            } else {
                shouldDraw = true
            }
        }
    )
}