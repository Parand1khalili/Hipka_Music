package com.hipka.app.presentation.features.search

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People // ✨ ویژگی کیانا: ایمپورت آیکون مردم برای دکمه پیدا کردن دوستان
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton // ویژگی کیانا: ایمپورت دکمه اوت‌لایند
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hipka.app.R
import com.hipka.app.data.local.entity.SearchHistoryEntity
import com.hipka.app.domain.model.Song
import com.hipka.app.presentation.theme.HipkaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    likedSongIds: Set<String>,
    onSongClick: (Song) -> Unit,
    onLikeClick: (Song) -> Unit,
    onNavigateToDiscoverUsers: () -> Unit, // ✨ ویژگی کیانا: پارامتر جدید ناوبری برای اتصال به هاب اکتشاف کاربران
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(HipkaTheme.dimens.spaceM)
    ) {
        // فیلد سرچ اصلی
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onIntent(SearchIntent.QueryChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(id = R.string.search_hint)) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onIntent(SearchIntent.ClearSearch) }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(HipkaTheme.dimens.cornerM),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    viewModel.onIntent(SearchIntent.SearchSong(uiState.searchQuery))
                    keyboardController?.hide()
                }
            )
        )

        Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceS))

        // ردیف فیلتر چیپ‌ها
        Row(
            horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = uiState.selectedFilter == SearchFilter.ALL,
                onClick = { viewModel.onIntent(SearchIntent.ChangeFilter(SearchFilter.ALL)) },
                label = { Text(stringResource(id = R.string.search_filter_all)) }
            )
            FilterChip(
                selected = uiState.selectedFilter == SearchFilter.SONG,
                onClick = { viewModel.onIntent(SearchIntent.ChangeFilter(SearchFilter.SONG)) },
                label = { Text(stringResource(id = R.string.search_filter_songs)) }
            )
            FilterChip(
                selected = uiState.selectedFilter == SearchFilter.ARTIST,
                onClick = { viewModel.onIntent(SearchIntent.ChangeFilter(SearchFilter.ARTIST)) },
                label = { Text(stringResource(id = R.string.search_filter_artists)) }
            )
        }

        // ✨ شاهکار سناریوی جدید UX (ویژگی کیانا): دکمه شیک پیدا کردن دوستان فقط زمانی که باکس سرچ خالی است نمایش داده/لود می‌شود
        if (uiState.searchQuery.isEmpty()) {
            Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceS))
            OutlinedButton(
                onClick = onNavigateToDiscoverUsers,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HipkaTheme.dimens.cornerM)
            ) {
                Icon(
                    imageVector = Icons.Filled.People,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(HipkaTheme.dimens.spaceS))
                Text(text = stringResource(id = R.string.search_find_friends))
            }
        }

        Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceS))

        // مدیریت وضعیت‌های مختلف صفحه سرچ (بر اساس معماری بدون ارور شما)
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.searchQuery.isEmpty() && uiState.searchHistory.isNotEmpty() -> {
                Text(
                    text = stringResource(id = R.string.search_recent_searches),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = HipkaTheme.dimens.spaceS)
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.searchHistory, key = { it.query }) { historyItem ->
                        SearchHistoryItem(
                            item = historyItem,
                            onClick = {
                                viewModel.onIntent(SearchIntent.SearchSong(historyItem.query))
                                keyboardController?.hide()
                            },
                            onDelete = { viewModel.onIntent(SearchIntent.DeleteHistoryItem(historyItem.query)) }
                        )
                    }
                }
            }

            uiState.searchQuery.isEmpty() && uiState.searchHistory.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(id = R.string.search_type_to_search),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            uiState.hasSearchedBefore && uiState.searchResults.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(id = R.string.search_no_results_for, uiState.searchQuery),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS)
                ) {
                    // ترکیب وضعیت لایک از دیتابیس با نتایج سرچ
                    val resultsWithLikeStatus = uiState.searchResults.map { song ->
                        song.copy(isLiked = likedSongIds.contains(song.id))
                    }

                    items(resultsWithLikeStatus, key = { it.id }) { song ->
                        SearchResultItem(
                            song = song,
                            onClick = { onSongClick(song) },
                            onLikeClick = { onLikeClick(song) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHistoryItem(
    item: SearchHistoryEntity, // حفظ تغییر شما به Entity (نکته کیانا: نسخه تو دریافت مستقیم استرینگ برای حل قطعی ارورهای دیتا تایپ بود)
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = HipkaTheme.dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = item.query,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = HipkaTheme.dimens.spaceM)
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchResultItem(
    song: Song,
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(HipkaTheme.dimens.cornerS))
            .clickable(onClick = onClick)
            .padding(HipkaTheme.dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.coverImageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(HipkaTheme.dimens.albumCoverS)
                .clip(RoundedCornerShape(HipkaTheme.dimens.cornerS))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = HipkaTheme.dimens.spaceM)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onLikeClick,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = if (song.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Toggle Like",
                tint = if (song.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}