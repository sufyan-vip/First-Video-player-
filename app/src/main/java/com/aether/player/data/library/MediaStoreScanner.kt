package com.aether.player.data.library

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.aether.player.data.db.FolderEntity
import com.aether.player.data.db.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class MediaStoreScanner(private val context: Context) {
    suspend fun scan(
        excludedFolders: Set<String> = emptySet(),
        hideDotFiles: Boolean = false,
    ): Pair<List<VideoEntity>, List<FolderEntity>> =
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
                    val displayName = cursor.getString(nameCol)
                    if (hideDotFiles && displayName != null && displayName.startsWith(".")) {
                        continue
                    }
                    videos += VideoEntity(
                        id = "local-$id",
                        uri = uri,
                        title = displayName ?: "Video",
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

    /** Imports videos from a user-picked folder tree (Storage Access Framework). */
    suspend fun scanTree(treeUri: Uri): Pair<List<VideoEntity>, List<FolderEntity>> =
        withContext(Dispatchers.IO) {
            val videos = mutableListOf<VideoEntity>()
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val rootId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
                ?: return@withContext emptyList<VideoEntity>() to emptyList()
            fun traverse(parentId: String, trail: String, depth: Int) {
                if (depth > 6 || videos.size >= 3000) return
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
                val cursor = runCatching {
                    context.contentResolver.query(
                        childrenUri,
                        arrayOf(
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                            DocumentsContract.Document.COLUMN_MIME_TYPE,
                            DocumentsContract.Document.COLUMN_SIZE,
                            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                        ),
                        null,
                        null,
                        null,
                    )
                }.getOrNull() ?: return
                cursor.use { c ->
                    val idCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    val modCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    while (c.moveToNext()) {
                        if (videos.size >= 3000) return
                        val docId = if (idCol >= 0) c.getString(idCol) else null ?: continue
                        val name = if (nameCol >= 0) c.getString(nameCol) else null ?: continue
                        val mime = if (mimeCol >= 0) c.getString(mimeCol) else null
                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            traverse(docId, "$trail/$name", depth + 1)
                        } else if (mime?.startsWith("video/") == true || isVideoName(name)) {
                            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId).toString()
                            val folderName = trail.substringAfterLast('/').ifBlank { "Imported" }
                            val modified = if (modCol >= 0) c.getLong(modCol) else System.currentTimeMillis()
                            videos += VideoEntity(
                                id = "saf-$docId",
                                uri = docUri,
                                title = name,
                                path = docUri,
                                folderId = "saf-${trail.hashCode()}",
                                folderName = folderName,
                                durationMs = 0L,
                                sizeBytes = if (sizeCol >= 0) c.getLong(sizeCol) else 0L,
                                width = 0,
                                height = 0,
                                dateAdded = modified,
                                dateModified = modified,
                                mimeType = mime ?: "video/*",
                                bitrate = 0,
                                isNetwork = false,
                            )
                        }
                    }
                }
            }
            traverse(rootId, "Imported", 0)
            val folders = videos.groupBy { it.folderId ?: "saf-imports" }.map { (id, list) ->
                FolderEntity(
                    id = id,
                    name = list.first().folderName ?: "Imported",
                    path = null,
                    videoCount = list.size,
                )
            }
            videos to folders
        }

    private fun isVideoName(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
        return ext in setOf(
            "mp4", "mkv", "webm", "avi", "mov", "m4v", "3gp", "ts", "m2ts",
            "flv", "wmv", "mpg", "mpeg", "m3u8", "mpd", "ogv",
        )
    }
}
