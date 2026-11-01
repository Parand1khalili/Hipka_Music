package com.hipka.app

import com.hipka.app.data.remote.api.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class AudiusTrackTest {
    private val track = AudiusTrack("abc123", "Track", AudiusUser("Artist"),
        streamable = true, gated = false)

    @Test fun publicTrackUsesStableProviderUrlAndNamespacedId() {
        val song = requireNotNull(track.toSongDto())
        assertEquals("audius:abc123", song.id)
        assertEquals("https://api.audius.co/v1/tracks/abc123/stream?app_name=HipkaMusic", song.audioUrl)
        assertEquals("Artist", song.artistName)
    }

    @Test fun unavailableAndGatedTracksAreExcluded() {
        assertNull(track.copy(streamable = false).toSongDto())
        assertNull(track.copy(gated = true).toSongDto())
        assertNull(track.copy(id = "../../private").toSongDto())
    }

    @Test fun missingAccessFlagsFailClosed() {
        val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<AudiusTracks>(
            """{"data":[{"id":"abc123","title":"Track","user":{"name":"Artist"},"unknown":42}]}""")
        assertNull(parsed.data.single().toSongDto())
        assertFalse(parsed.data.single().downloadable)
    }

    @Test fun singleTrackEndpointReturnsAnObjectNotAnArray() {
        val parsed = Json.decodeFromString<AudiusTrackResponse>(
            """{"data":{"id":"abc123","title":"Track","user":{"name":"Artist"},"is_streamable":true,"is_stream_gated":false}}""")
        assertEquals("audius:abc123", parsed.data?.toSongDto()?.id)
    }

    @Test fun parsesOptionalArtworkAndNullReleaseDate() {
        val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<AudiusTracks>(
            """{"data":[{"id":"abc123","title":"Track","user":{"name":"Artist"},"is_streamable":true,"is_stream_gated":false,"release_date":null,"artwork":null}]}""")
        val song = requireNotNull(parsed.data.single().toSongDto())
        assertEquals("", song.coverImageUrl)
        assertEquals("", song.releaseDate)
    }
}
