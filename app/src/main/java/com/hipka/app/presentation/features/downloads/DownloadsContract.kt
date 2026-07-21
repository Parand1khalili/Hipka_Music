package com.hipka.app.presentation.features.downloads

import com.hipka.app.domain.model.Song

enum class DownloadsSortOrder { TITLE, ARTIST }

data class DownloadsState(
    val isLoading: Boolean = true,
    val songs: List<Song> = emptyList(),
    val sortOrder: DownloadsSortOrder = DownloadsSortOrder.TITLE,
    val error: String? = null
)

sealed interface DownloadsIntent {
    data class DeleteDownload(val songId: String) : DownloadsIntent
    data class ChangeSortOrder(val sortOrder: DownloadsSortOrder) : DownloadsIntent
}
