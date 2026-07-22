package com.hipka.app.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

/**
 * جایگزین AsyncImage برای کاور آهنگ/پلی‌لیست. بخشی از داده دموی سرور لینک کاور
 * مرده (۴۰۴) دارد؛ به‌جای فضای خالی، یک آیکون موزیک پیش‌فرض نمایش داده می‌شود
 * تا حین بارگذاری یا در صورت شکست، UI هرگز خالی نماند.
 */
@Composable
fun CoverImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
            loading = { CoverPlaceholderIcon() },
            error = { CoverPlaceholderIcon() },
            success = { SubcomposeAsyncImageContent() }
        )
    }
}

@Composable
private fun CoverPlaceholderIcon() {
    Icon(
        imageVector = Icons.Default.MusicNote,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    )
}
