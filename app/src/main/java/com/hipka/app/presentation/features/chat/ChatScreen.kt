package com.hipka.app.presentation.features.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // ایمپورت دکمه بازگشت
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hipka.app.R
import com.hipka.app.domain.model.Message
import com.hipka.app.presentation.theme.HipkaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBackClick: () -> Unit, // ✨ اضافه شدن اکشن بک برای خروج از چت
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat", fontWeight = FontWeight.Bold) },
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
                uiState.errorMessage != null && uiState.messages.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize().padding(HipkaTheme.dimens.spaceM),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = uiState.errorMessage.orEmpty())
                }
                else -> ChatContent(uiState = uiState, onIntent = viewModel::onIntent)
            }
        }
    }
}

@Composable
private fun ChatContent(
    uiState: ChatUiState,
    onIntent: (ChatIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true,
            contentPadding = PaddingValues(HipkaTheme.dimens.spaceM),
            verticalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS, Alignment.Bottom)
        ) {
            items(uiState.messages.reversed(), key = { it.id }) { message ->
                MessageBubble(message = message, isMine = message.senderId == uiState.currentUserId)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HipkaTheme.dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HipkaTheme.dimens.spaceS)
        ) {
            OutlinedTextField(
                value = uiState.draftText,
                onValueChange = { onIntent(ChatIntent.DraftChanged(it)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(id = R.string.chat_input_hint)) },
                singleLine = true
            )
            IconButton(onClick = { onIntent(ChatIntent.SendMessage) }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message, isMine: Boolean) {
    val alignment = if (isMine) Alignment.End else Alignment.Start
    val bubbleColor = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = HipkaTheme.dimens.cornerM,
                topEnd = HipkaTheme.dimens.cornerM,
                bottomStart = if (isMine) HipkaTheme.dimens.cornerM else HipkaTheme.dimens.cornerS,
                bottomEnd = if (isMine) HipkaTheme.dimens.cornerS else HipkaTheme.dimens.cornerM
            )
        ) {
            Text(
                text = message.text,
                color = textColor,
                modifier = Modifier.padding(
                    horizontal = HipkaTheme.dimens.spaceM,
                    vertical = HipkaTheme.dimens.spaceS
                )
            )
        }
    }
}