package com.aether.player.data.library

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class StreamMeta(
    val videoCodec: String?,
    val audioCodec: String?,
    val fps: Float?,
    val sampleRateHz: Int?,
    val channels: Int?,
)

sealed interface FileResult {
    data object Done : FileResult
    data class Consent(val sender: IntentSender) : FileResult
    data class Error(val message: String) : FileResult
}

/** Real file operations with Android 10+ consent handling, SAF copy, and stream probing. */
class MediaOps(private val context: Context) {

    suspend fun rename(uriString: String, newName: String): FileResult = withContext(Dispatchers.IO) {
        val uri = uriString.toUri()
        if (uri.scheme != ContentResolver_SCHEME) {
            return@withContext FileResult.Error("Only library videos can be renamed")
        }
        val clean = newName.trim().takeIf { it.isNotEmpty() }
            ?: return@withContext FileResult.Error("Name is empty")
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, clean)
            put(MediaStore.Video.Media.TITLE, clean.substringBeforeLast('.'))
        }
        try {
            val rows = context.contentResolver.update(uri, values, null, null)
            if (rows > 0) FileResult.Done else FileResult.Error("The system refused the rename")
        } catch (e: SecurityException) {
            consentOrError(e, listOf(uri), write = true)
        } catch (t: Throwable) {
            FileResult.Error(t.message ?: "Rename failed")
        }
    }

    suspend fun delete(uriString: String): FileResult = withContext(Dispatchers.IO) {
        val uri = uriString.toUri()
        try {
            val rows = context.contentResolver.delete(uri, null, null)
            if (rows > 0) FileResult.Done else FileResult.Error("The system refused the delete")
        } catch (e: SecurityException) {
            consentOrError(e, listOf(uri), write = false)
        } catch (t: Throwable) {
            FileResult.Error(t.message ?: "Delete failed")
        }
    }

    private fun consentOrError(e: SecurityException, uris: List<Uri>, write: Boolean): FileResult {
        if (Build.VERSION.SDK_INT >= 29 && e is RecoverableSecurityException) {
            return FileResult.Consent(e.userAction.actionIntent.intentSender)
        }
        if (Build.VERSION.SDK_INT >= 30) {
            val pending = if (write) {
                MediaStore.createWriteRequest(context.contentResolver, uris)
            } else {
                MediaStore.createDeleteRequest(context.contentResolver, uris)
            }
            return FileResult.Consent(pending.intentSender)
        }
        return FileResult.Error(e.message ?: "Permission denied")
    }

    /** Copies a video into a user-picked folder tree. Returns the new document URI, or null. */
    suspend fun copyToTree(sourceUri: String, treeUri: Uri, displayName: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val treeDoc = DocumentsContract.getTreeDocumentId(treeUri)
                val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDoc)
                val mime = context.contentResolver.getType(sourceUri.toUri()) ?: "video/mp4"
                val dest = DocumentsContract.createDocument(
                    context.contentResolver,
                    parentUri,
                    mime,
                    displayName.ifBlank { "aether-copy.mp4" },
                ) ?: return@runCatching null
                context.contentResolver.openInputStream(sourceUri.toUri())?.use { input ->
                    context.contentResolver.openOutputStream(dest)?.use { output ->
                        input.copyTo(output)
                    } ?: return@runCatching null
                } ?: return@runCatching null
                dest.toString()
            }.getOrNull()
        }

    /** Best-effort codec / fps / sample-rate probing. Null fields mean "unknown on this device". */
    suspend fun probe(uriString: String): StreamMeta = withContext(Dispatchers.IO) {
        var videoCodec: String? = null
        var audioCodec: String? = null
        var sampleRate: Int? = null
        var channels: Int? = null
        runCatching {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(context, uriString.toUri(), null)
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = runCatching { format.getString(MediaFormat.KEY_MIME) }.getOrNull()
                        ?: continue
                    when {
                        mime.startsWith("video/") && videoCodec == null -> {
                            videoCodec = shortCodec(mime)
                        }
                        mime.startsWith("audio/") && audioCodec == null -> {
                            audioCodec = shortCodec(mime)
                            sampleRate = intOrNull(format, MediaFormat.KEY_SAMPLE_RATE)
                            channels = intOrNull(format, MediaFormat.KEY_CHANNEL_COUNT)
                        }
                    }
                }
            } finally {
                runCatching { extractor.release() }
            }
        }
        var fps: Float? = null
        runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uriString.toUri())
                // Key 32 = METADATA_KEY_VIDEO_FRAME_COUNT; referenced by value so this
                // compiles on every level (unknown keys safely return null at runtime).
                val frames = retriever.extractMetadata(32)?.toLongOrNull()
                val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                fps = if (frames != null && dur != null && dur > 0) {
                    frames * 1000f / dur
                } else {
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()
                }
            } finally {
                runCatching { retriever.release() }
            }
        }
        StreamMeta(videoCodec, audioCodec, fps, sampleRate, channels)
    }

    private fun intOrNull(format: MediaFormat, key: String): Int? =
        runCatching { if (format.containsKey(key)) format.getInteger(key) else null }.getOrNull()

    private fun shortCodec(mime: String): String = when (mime.lowercase(Locale.US)) {
        "video/avc" -> "H.264"
        "video/hevc" -> "H.265"
        "video/x-vnd.on2.vp8" -> "VP8"
        "video/x-vnd.on2.vp9" -> "VP9"
        "video/av01" -> "AV1"
        "video/mp4v-es" -> "MPEG-4"
        "video/3gpp", "video/3gpp2" -> "H.263"
        "video/mpeg2" -> "MPEG-2"
        "video/x-ms-wmv", "video/wmv" -> "WMV"
        "audio/mp4a-latm" -> "AAC"
        "audio/mpeg" -> "MP3"
        "audio/ac3" -> "AC-3"
        "audio/eac3", "audio/eac3-joc" -> "E-AC-3"
        "audio/vorbis" -> "Vorbis"
        "audio/opus" -> "Opus"
        "audio/flac" -> "FLAC"
        "audio/raw" -> "PCM"
        "audio/g711-alaw", "audio/g711-mlaw" -> "G.711"
        else -> mime.substringAfter('/').uppercase(Locale.US).take(16)
    }

    companion object {
        private const val ContentResolver_SCHEME = "content"
    }
}
