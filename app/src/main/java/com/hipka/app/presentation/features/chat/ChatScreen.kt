package com.hipka.app.presentation.features.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hipka.app.R
import com.hipka.app.domain.model.Message
import com.hipka.app.domain.model.MessageStatus
import com.hipka.app.domain.model.Song
import com.hipka.app.presentation.theme.HipkaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBackClick: () -> Unit,
    onPlaySong: (Song) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible && uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.peerUser?.name ?: "Chat",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (uiState.isPeerTyping) {
                            Text(
                                text = stringResource(id = R.string.chat_typing_indicator),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            when {
                uiState.isLoading -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                uiState.errorMessage != null && uiState.messages.isEmpty() -> Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(HipkaTheme.dimens.spaceM),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = uiState.errorMessage.orEmpty())
                }
                else -> ChatMessages(
                    uiState = uiState,
                    listState = listState,
                    onSongClick = { songId ->
                        uiState.sharedSongs[songId]?.let(onPlaySong)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            ChatComposer(uiState = uiState, onIntent = viewModel::onIntent)
        }

        if (uiState.isSongPickerOpen) {
            SongPickerSheet(
                songs = uiState.availableSongs,
                onDismiss = { viewModel.onIntent(ChatIntent.ToggleSongPicker) },
                onPick = { song -> viewModel.onIntent(ChatIntent.ShareSong(song)) }
            )
        }
    }
}

@Composable
private fun ChatMessages(
    uiState: ChatUiState,
    listState: LazyListState,
    onSongClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        reverseLayout = true,
        contentPadding = PaddingValues(horizontal = HipkaTheme.dimens.spaceM, vertical = HipkaTheme.dimens.spaceS),
        verticalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS)
    ) {
        items(uiState.messages.reversed(), key = { it.id }) { message ->
            MessageBubble(
                message = message,
                isMine = message.senderId == uiState.currentUserId,
                sharedSong = message.sharedSongId?.let { uiState.sharedSongs[it] },
                onSongClick = { message.sharedSongId?.let(onSongClick) }
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isMine: Boolean,
    sharedSong: Song?,
    onSongClick: () -> Unit
) {
    val bubbleColor = if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    // تعیین حداقل عرض برای کارت‌های آهنگ جهت جلوگیری از فشرده شدن
    val minWidth = if (message.sharedSongId != null) 250.dp else 60.dp

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(min = minWidth, max = 290.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                if (message.sharedSongId != null) {
                    // 🎵 کارت موزیک به استایل تلگرام
                    TelegramAudioCard(
                        song = sharedSong,
                        textColor = textColor,
                        onPlayClick = onSongClick
                    )
                } else {
                    Text(
                        text = message.text,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (isMine) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MessageStatusIcon(status = message.status, tint = textColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun TelegramAudioCard(
    song: Song?,
    textColor: Color,
    onPlayClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlayClick)
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // کاور گرد آهنگ با دکمه Play
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                if (!song?.coverImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = song?.coverImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // لایه تیره برای مشخص شدن دکمه Play
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // اطلاعات نام آهنگ و خواننده
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song?.title ?: stringResource(R.string.loading),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song?.artistName ?: "Hipka Music",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MessageStatusIcon(status: MessageStatus, tint: Color) {
    val icon = when (status) {
        MessageStatus.SENDING -> Icons.Filled.Schedule
        MessageStatus.SENT -> Icons.Filled.Check
        MessageStatus.READ -> Icons.Filled.DoneAll
    }
    val iconTint = if (status == MessageStatus.READ) MaterialTheme.colorScheme.primary else tint.copy(alpha = 0.6f)
    Icon(
        imageVector = icon,
        contentDescription = status.name,
        tint = iconTint,
        modifier = Modifier.size(15.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatComposer(uiState: ChatUiState, onIntent: (ChatIntent) -> Unit) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onIntent(ChatIntent.ToggleSongPicker) }) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = stringResource(id = R.string.chat_share_song_cd),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            TextField(
                value = uiState.draftText,
                onValueChange = { onIntent(ChatIntent.DraftChanged(it)) },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                placeholder = { Text(stringResource(id = R.string.chat_message_hint)) },
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            IconButton(
                onClick = { onIntent(ChatIntent.SendMessage) },
                enabled = uiState.draftText.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = if (uiState.draftText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongPickerSheet(
    songs: List<Song>,
    onDismiss: () -> Unit,
    onPick: (Song) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(HipkaTheme.dimens.spaceM)) {
            Text(
                text = stringResource(id = R.string.chat_pick_song_title),
                style = MaterialTheme.typography.titleMedium
            )
            Column(modifier = Modifier.padding(top = HipkaTheme.dimens.spaceS)) {
                if (songs.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.chat_no_songs_to_share),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = HipkaTheme.dimens.spaceM)
                    )
                } else {
                    songs.forEach { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(song) }
                                .padding(vertical = HipkaTheme.dimens.spaceS),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = song.coverImageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(HipkaTheme.dimens.albumCoverS)
                                    .clip(RoundedCornerShape(HipkaTheme.dimens.cornerS))
                            )
                            Column(modifier = Modifier.padding(start = HipkaTheme.dimens.spaceM)) {
                                Text(text = song.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = song.artistName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}