package com.hipka.app.presentation.features.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hipka.app.R

@Composable
fun LocalLibraryScreen(onLikes: () -> Unit, onRecent: () -> Unit, onDownloads: () -> Unit, onSettings: () -> Unit, onLegacySignIn: (() -> Unit)? = null) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.local_library_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.local_library_description), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(40.dp))
                Text(stringResource(R.string.audius_source), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.audius_description), style = MaterialTheme.typography.bodyMedium)
            }
        }
        LibraryAction(stringResource(R.string.library_favorites), Icons.Default.Favorite, onLikes)
        LibraryAction(stringResource(R.string.library_recent), Icons.Default.History, onRecent)
        LibraryAction(stringResource(R.string.downloads_title), Icons.Default.Download, onDownloads)
        LibraryAction(stringResource(R.string.library_settings), Icons.Default.Settings, onSettings)
        onLegacySignIn?.let { action -> TextButton(onClick = action) { Text(stringResource(R.string.legacy_demo_sign_in)) } }
    }
}

@Composable
private fun LibraryAction(label: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ListItem(headlineContent = { Text(label) }, leadingContent = { Icon(icon, contentDescription = null) })
    }
}
