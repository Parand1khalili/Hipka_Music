package com.hipka.app

import com.hipka.app.data.player.downloadedFile
import com.hipka.app.domain.model.Song
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OfflinePlaybackSourceTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun song(path: String, complete: Boolean = true) = Song(
        "audius:test", "Offline", "Artist", "", "https://unreachable.invalid/stream",
        isDownloaded = complete, localFilePath = path)

    @Test fun completedDownloadResolvesWithoutAnyNetworkRequest() {
        val file = temporary.newFile("track.audio").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        assertEquals(file, song(file.absolutePath).downloadedFile())
    }
    @Test fun partialEmptyDeletedAndDirectoryPathsAreNotPlayableDownloads() {
        val file = temporary.newFile("partial.audio")
        assertNull(song(file.absolutePath).downloadedFile())
        file.writeBytes(byteArrayOf(1))
        assertNull(song(file.absolutePath, false).downloadedFile())
        file.delete()
        assertNull(song(file.absolutePath).downloadedFile())
        assertNull(song(temporary.root.absolutePath).downloadedFile())
    }
}
