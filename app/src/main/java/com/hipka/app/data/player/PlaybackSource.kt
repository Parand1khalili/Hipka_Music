package com.hipka.app.data.player

import com.hipka.app.domain.model.Song
import java.io.File

/** Only completed, non-empty downloads may replace a remote stream. */
internal fun Song.downloadedFile(): File? = localFilePath
    ?.takeIf { isDownloaded }
    ?.let(::File)
    ?.takeIf { it.isFile && it.length() > 0 }
