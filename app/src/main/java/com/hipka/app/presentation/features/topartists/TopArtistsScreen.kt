package com.hipka.app.presentation.features.topartists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hipka.app.R
import com.hipka.app.core.util.formatPlayCount
import com.hipka.app.domain.model.Artist
import com.hipka.app.presentation.theme.HipkaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopArtistsScreen(
    onBackClick: () -> Unit,
    onArtistClick: (Artist) -> Unit,
    viewModel: TopArtistsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.home_quick_artists), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                uiState.errorMessage != null && uiState.artists.isEmpty() -> {
                    Text(
                        text = uiState.errorMessage ?: "",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(HipkaTheme.dimens.spaceM),
                        verticalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS)
                    ) {
                        itemsIndexed(uiState.artists) { index, artist ->
                            ArtistRowItem(
                                rank = index + 1,
                                artist = artist,
                                onArtistClick = onArtistClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistRowItem(
    rank: Int,
    artist: Artist,
    onArtistClick: (Artist) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onArtistClick(artist) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HipkaTheme.dimens.spaceS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ۱. شماره رتبه (۱، ۲، ۳ ...)
            Text(
                text = "$rank",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(36.dp)
            )

            Spacer(modifier = Modifier.width(HipkaTheme.dimens.spaceXS))

            // ۲. عکس پروفایل دایره‌ای هنرمند
            AsyncImage(
                model = artist.imageUrl,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.ic_logo_fa),
                error = painterResource(id = R.drawable.ic_logo_fa),
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(HipkaTheme.dimens.spaceM))

            // ۳. نام هنرمند و تعداد پخش فرمت‌شده
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${artist.totalPlayCount.formatPlayCount()} " + stringResource(id = R.string.play_count_suffix),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}