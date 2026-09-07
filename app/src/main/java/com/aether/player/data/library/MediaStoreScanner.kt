package com.aether.player.data.library

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.aether.player.data.db.FolderEntity
import com.aether.player.data.db.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreScanner(private val context: Context) {
    suspend fun scan(excludedFolders: Set<String> = emptySet()): Pair<List<VideoEntity>, List<FolderEntity>> =
        withContext(Dispatchers.IO) {
            val videos = mutableListOf<VideoEntity>()
            val collection = if (Build.VERSION.SDK_INT >= 29) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.BUCKET_ID,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.BITRATE,
            )
            val sort = "${MediaStore.Video.Media.DATE_ADDED} DESC"
            context.contentResolver.query(collection, projection, null, null, sort)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val wCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val hCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val modCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val bitrateCol = cursor.getColumnIndex(MediaStore.Video.Media.BITRATE)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id).toString()
                    val path = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val folderName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) else path?.substringBeforeLast('/')
                    val folderId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) else folderName
                    if (folderName != null && excludedFolders.any { folderName.contains(it, true) || path?.contains(it, true) == true }) {
                        continue
                    }
                    videos += VideoEntity(
                        id = "local-$id",
                        uri = uri,
                        title = cursor.getString(nameCol) ?: "Video",
                        path = path,
                        folderId = folderId,
                        folderName = folderName,
                        durationMs = cursor.getLong(durCol),
                        sizeBytes = cursor.getLong(sizeCol),
                        width = cursor.getInt(wCol),
                        height = cursor.getInt(hCol),
                        dateAdded = cursor.getLong(addedCol) * 1000L,
                        dateModified = cursor.getLong(modCol) * 1000L,
                        mimeType = if (mimeCol >= 0) cursor.getString(mimeCol) else "video/*",
                        bitrate = if (bitrateCol >= 0) cursor.getInt(bitrateCol) else 0,
                        isNetwork = false,
                    )
                }
            }
            val folders = videos.groupBy { it.folderId ?: "unknown" }.map { (id, list) ->
                FolderEntity(
                    id = id,
                    name = list.first().folderName ?: "Folder",
                    path = list.first().path?.substringBeforeLast('/'),
                    videoCount = list.size,
                )
            }
            videos to folders
        }
}
