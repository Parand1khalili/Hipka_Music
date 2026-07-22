package com.hipka.app.presentation.features.home

import com.hipka.app.domain.model.Playlist
import com.hipka.app.domain.model.Song
import com.hipka.app.domain.model.User

data class HomeUiState(
    val carouselSongs: List<Song> = emptyList(),
    val popularSongs: List<Song> = emptyList(),
    val newReleases: List<Song> = emptyList(),
    val globalPlaylists: List<Playlist> = emptyList(),
    val localPlaylists: List<Playlist> = emptyList(),
    val currentUser: User? = null,
    val isLoading: Boolean = true,
    /** آفلاین هستیم و محتوای نمایش‌داده‌شده از کش محلی می‌آید */
    val isShowingCachedData: Boolean = false,
    /** آفلاین هستیم و هیچ چیزی هم در کش نیست */
    val isOfflineWithNoCache: Boolean = false,
    val errorMessage: String? = null
)

sealed interface HomeIntent {
    data object RefreshHome : HomeIntent
}
