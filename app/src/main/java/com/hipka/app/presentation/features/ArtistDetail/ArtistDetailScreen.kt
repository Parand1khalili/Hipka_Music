package com.hipka.app.presentation.features.ArtistDetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hipka.app.R
import com.hipka.app.domain.model.Song
import com.hipka.app.presentation.theme.HipkaTheme
import com.hipka.app.presentation.features.ArtistDetail.ArtistDetailUiState
import com.hipka.app.presentation.features.ArtistDetail.ArtistDetailIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistName: String,
    imageUrl: String,
    likedSongIds: Set<String>,
    onBackClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onLikeClick: (Song) -> Unit,
    onShuffleClick: (List<Song>) -> Unit,
    viewModel: ArtistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(artistName, imageUrl) {
        viewModel.onIntent(ArtistDetailIntent.LoadArtistData(artistName, imageUrl))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = uiState.artistName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = HipkaTheme.dimens.spaceXL)
                ) {
                    // ۱. هدر پروفایل هنرمند (عکس بزرگ دایره‌ای + نام + دکمه شافل)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(HipkaTheme.dimens.spaceM),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = uiState.imageUrl,
                                contentDescription = uiState.artistName,
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(id = R.drawable.ic_logo_fa),
                                error = painterResource(id = R.drawable.ic_logo_fa),
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(CircleShape)
                            )

                            Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceM))

                            Text(
                                text = uiState.artistName,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            Spacer(modifier = Modifier.height(HipkaTheme.dimens.spaceM))

                            // دکمه Shuffle Play
                            Button(
                                onClick = { onShuffleClick(uiState.songs) },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Shuffle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Shuffle Play", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // ۲. لیست آهنگ‌های هنرمند
                    items(uiState.songs) { song ->
                        val isLiked = likedSongIds.contains(song.id)
                        ArtistSongRowItem(
                            song = song.copy(isLiked = isLiked),
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
private fun ArtistSongRowItem(
    song: Song,
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = HipkaTheme.dimens.spaceM, vertical = HipkaTheme.dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.coverImageUrl,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(HipkaTheme.dimens.cornerS))
        )

        Spacer(modifier = Modifier.width(HipkaTheme.dimens.spaceM))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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

        IconButton(onClick = onLikeClick) {
            Icon(
                imageVector = if (song.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Like",
                tint = if (song.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}