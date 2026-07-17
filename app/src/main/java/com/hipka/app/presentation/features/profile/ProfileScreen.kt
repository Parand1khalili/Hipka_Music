package com.hipka.app.presentation.features.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hipka.app.R
import com.hipka.app.core.locale.LocaleManager
import com.hipka.app.data.local.datastore.ThemeMode
import com.hipka.app.domain.model.User
import com.hipka.app.presentation.main.MainIntent
import com.hipka.app.presentation.main.MainUiState
import com.hipka.app.presentation.theme.HipkaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    mainUiState: MainUiState,
    onMainIntent: (MainIntent) -> Unit,
    onNavigateToFollowedUsers: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ✨ هماهنگ‌سازی لایف‌سایکل: هربار کاربر وارد این تب می‌شود، اطلاعات به صورت زنده ریفرش می‌شوند
    LaunchedEffect(Unit) {
        viewModel.onIntent(ProfileIntent.Retry)
    }

    when {
        uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        uiState.currentUser == null -> DemoUserPicker(
            users = uiState.allUsers,
            onPick = { viewModel.onIntent(ProfileIntent.SelectDemoUser(it)) }
        )
        else -> ProfileContent(
            user = uiState.currentUser!!,
            uiState = uiState,
            mainUiState = mainUiState,
            onMainIntent = onMainIntent,
            onNavigateToFollowedUsers = onNavigateToFollowedUsers,
            onLogout = { viewModel.onIntent(ProfileIntent.Logout) }
        )
    }
}

@Composable
private fun DemoUserPicker(users: List<User>, onPick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(HipkaTheme.dimens.spaceM)) {
        Text(
            text = stringResource(id = R.string.demo_user_picker_title),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(HipkaTheme.dimens.spaceM))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS)) {
            items(users, key = { it.id }) { user ->
                ListItem(
                    headlineContent = { Text(user.name) },
                    leadingContent = { UserAvatar(user) },
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onPick(user.id) }
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    user: User,
    uiState: ProfileUiState,
    mainUiState: MainUiState,
    onMainIntent: (MainIntent) -> Unit,
    onNavigateToFollowedUsers: (String) -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(HipkaTheme.dimens.spaceM),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UserAvatar(user, size = HipkaTheme.dimens.albumCoverM)
        Spacer(Modifier.height(HipkaTheme.dimens.spaceS))
        Text(text = user.name, style = MaterialTheme.typography.titleLarge)
        if (user.isPremium) {
            Spacer(Modifier.height(HipkaTheme.dimens.spaceXS))
            AssistChip(onClick = {}, label = { Text(stringResource(id = R.string.premium)) })
        }

        Spacer(Modifier.height(HipkaTheme.dimens.spaceM))

        // 📊 بخش آمار فالوور و فالووینگ با استایل کاملاً همسان، بولد و زیبا
        Row(
            horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // کلیک روی فالوورها
            Text(
                text = "${uiState.followerIds.size} ${stringResource(id = R.string.followers)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onNavigateToFollowedUsers("followers") }
                    .padding(HipkaTheme.dimens.spaceXS)
            )
            Text(
                text = "•",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            // کلیک روی فالووینگ‌ها
            Text(
                text = "${uiState.followingIds.size} ${stringResource(id = R.string.following)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onNavigateToFollowedUsers("following") }
                    .padding(HipkaTheme.dimens.spaceXS)
            )
        }

        Spacer(Modifier.height(HipkaTheme.dimens.spaceL))

        Text(text = stringResource(id = R.string.language), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(HipkaTheme.dimens.spaceXS))
        Row(horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS)) {
            FilterChip(
                selected = mainUiState.languageCode == LocaleManager.LANGUAGE_ENGLISH,
                onClick = { onMainIntent(MainIntent.ChangeLanguage(LocaleManager.LANGUAGE_ENGLISH)) },
                label = { Text("English") }
            )
            FilterChip(
                selected = mainUiState.languageCode == LocaleManager.LANGUAGE_PERSIAN,
                onClick = { onMainIntent(MainIntent.ChangeLanguage(LocaleManager.LANGUAGE_PERSIAN)) },
                label = { Text("فارسی") }
            )
        }

        Spacer(Modifier.height(HipkaTheme.dimens.spaceM))

        Text(text = stringResource(id = R.string.theme), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(HipkaTheme.dimens.spaceXS))
        Row(horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS)) {
            FilterChip(
                selected = mainUiState.themeMode == ThemeMode.LIGHT,
                onClick = { onMainIntent(MainIntent.ChangeThemeMode(ThemeMode.LIGHT)) },
                label = { Text(stringResource(id = R.string.theme_light)) }
            )
            FilterChip(
                selected = mainUiState.themeMode == ThemeMode.DARK,
                onClick = { onMainIntent(MainIntent.ChangeThemeMode(ThemeMode.DARK)) },
                label = { Text(stringResource(id = R.string.theme_dark)) }
            )
            FilterChip(
                selected = mainUiState.themeMode == ThemeMode.SYSTEM,
                onClick = { onMainIntent(MainIntent.ChangeThemeMode(ThemeMode.SYSTEM)) },
                label = { Text(stringResource(id = R.string.theme_system)) }
            )
        }

        Spacer(Modifier.height(HipkaTheme.dimens.spaceXL))

        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(id = R.string.switch_demo_user))
        }
    }
}

@Composable
private fun UserAvatar(user: User, size: Dp = 40.dp) {
    AsyncImage(
        model = user.avatarUrl,
        contentDescription = user.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
    )
}