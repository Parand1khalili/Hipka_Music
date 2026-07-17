package com.hipka.app.presentation.features.followedusers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hipka.app.R
import com.hipka.app.presentation.theme.HipkaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowedUsersScreen(
    onOpenChat: (String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: FollowedUsersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                // ✨ اصلاح باگ UX: متصل شدن عنوان به سیستم چندزبانگی (Discover People / کشف کاربران)
                title = {
                    Text(
                        text = stringResource(id = R.string.nav_discover_users),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.errorMessage != null -> Box(
                    modifier = Modifier.fillMaxSize().padding(HipkaTheme.dimens.spaceM),
                    contentAlignment = Alignment.Center
                ) {
                    Text(uiState.errorMessage.orEmpty())
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(HipkaTheme.dimens.spaceM),
                    verticalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS)
                ) {
                    items(uiState.users, key = { it.id }) { user ->
                        val isFollowing = user.id in uiState.followingIds
                        ListItem(
                            headlineContent = { Text(user.name) },
                            supportingContent = {
                                Text(
                                    text = stringResource(id = R.string.tap_to_chat_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            leadingContent = {
                                AsyncImage(
                                    model = user.avatarUrl,
                                    contentDescription = user.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(HipkaTheme.dimens.albumCoverS)
                                        .clip(CircleShape)
                                )
                            },
                            trailingContent = {
                                FilledTonalButton(
                                    onClick = { viewModel.onIntent(FollowedUsersIntent.ToggleFollow(user.id)) }
                                ) {
                                    Text(
                                        text = if (isFollowing) {
                                            stringResource(id = R.string.btn_unfollow)
                                        } else {
                                            stringResource(id = R.string.btn_follow)
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.clickable { onOpenChat(user.id) }
                        )
                    }
                }
            }
        }
    }
}