package com.hipka.app.presentation.navigation

sealed class Screen(val route: String) {

    // مقصدهای منوی پایین صفحه (Bottom Navigation)
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Downloads : Screen("downloads")
    data object Playlists : Screen("playlists")
    data object Profile : Screen("profile")

    // صفحات فرعی و ثانویه برنامه
    data object NowPlaying : Screen("now_playing")
    data object Settings : Screen("settings")
    data object FollowedUsers : Screen("followed_users")
    data object LikedSongs : Screen("liked_songs")
    data object RecentlyPlayed : Screen("recently_played")

    data object ChatList : Screen("chat_list")
    data object ChatConversation : Screen("chat/{peerUserId}") {
        const val ARG_PEER_USER_ID = "peerUserId"
        fun createRoute(peerUserId: String) = "chat/$peerUserId"
    }

    companion object {
        val bottomNavItems: List<Screen> = listOf(Home, Search, Downloads, Playlists, Profile)
    }

    object SeeAll : Screen("see_all/{section}") {
        fun createRoute(section: String) = "see_all/$section"
    }
    data object TopArtists : Screen("top_artists")
}