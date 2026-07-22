package com.hipka.app.presentation.features.profile

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hipka.app.R
import com.hipka.app.domain.model.User
import com.hipka.app.presentation.theme.HipkaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToFollowedUsers: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onIntent(ProfileIntent.Retry)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(id = R.string.settings_title)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                else -> ProfileContent(
                    uiState = uiState,
                    onNavigateToFollowedUsers = onNavigateToFollowedUsers,
                    onUpgradePremium = { viewModel.onIntent(ProfileIntent.UpgradePremium) },
                    onLogout = { viewModel.onIntent(ProfileIntent.Logout) }
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onNavigateToFollowedUsers: (String) -> Unit,
    onUpgradePremium: () -> Unit,
    onLogout: () -> Unit
) {
    val user = uiState.currentUser ?: return

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
            AssistChip(
                onClick = {},
                label = { Text(stringResource(id = R.string.premium)) },
                leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }

        Spacer(Modifier.height(HipkaTheme.dimens.spaceM))

        // آمار فالوورها و فالوئینگ‌ها
        Row(
            horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

        // کارت خرید/فعال‌سازی اشتراک پریمیوم
        PremiumCard(
            isPremium = user.isPremium,
            isProcessing = uiState.isUpgradingPremium,
            onUpgradeClick = onUpgradePremium
        )

        Spacer(Modifier.height(HipkaTheme.dimens.spaceL))

        // دکمه خروج از حساب کاربری (Log Out)
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.logout),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun PremiumCard(isPremium: Boolean, isProcessing: Boolean, onUpgradeClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(HipkaTheme.dimens.spaceM)) {
            Text(
                text = if (isPremium) stringResource(id = R.string.premium_active_desc)
                else stringResource(id = R.string.premium_free_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(HipkaTheme.dimens.spaceS))
            Button(
                onClick = onUpgradeClick,
                enabled = !isPremium && !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isProcessing) {
                    Text(stringResource(id = R.string.premium_processing))
                } else if (isPremium) {
                    Text(stringResource(id = R.string.premium_active_button))
                } else {
                    Text(stringResource(id = R.string.premium_upgrade_button))
                }
            }
        }
    }
}

@Composable
private fun UserAvatar(user: User, size: Dp = 40.dp) {
    val avatarUrl = user.avatarUrl

    if (!avatarUrl.isNullOrBlank() && avatarUrl.startsWith("http")) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        val drawableRes = when (avatarUrl) {
            "avatar_female" -> R.drawable.avatar_female
            "avatar_male" -> R.drawable.avatar_male
            else -> R.drawable.avatar_male
        }

        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
        )
    }
}