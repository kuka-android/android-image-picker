package com.esafirm.imagepicker.features.fileloader

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.esafirm.imagepicker.features.ImagePickerConfig
import com.esafirm.imagepicker.features.common.ImageLoaderListener
import com.esafirm.imagepicker.helper.ImagePickerUtils
import com.esafirm.imagepicker.model.Folder
import com.esafirm.imagepicker.model.Image
import java.io.File
import java.util.ArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class DefaultImageFileLoader(private val context: Context) : ImageFileLoader {

    private var executor: ExecutorService? = null

    override fun loadDeviceImages(
        config: ImagePickerConfig,
        listener: ImageLoaderListener
    ) {
        val isFolderMode = config.isFolderMode
        val includeVideo = config.isIncludeVideo
        val onlyVideo = config.isOnlyVideo
        val includeAnimation = config.isIncludeAnimation
        val excludedImages = config.excludedImages

        getExecutorService().execute(
            ImageLoadRunnable(
                context.applicationContext,
                isFolderMode,
                onlyVideo,
                includeVideo,
                includeAnimation,
                excludedImages,
                listener
            )
        )
    }

    override fun abortLoadImages() {
        executor?.shutdown()
        executor = null
    }

    private fun getExecutorService(): ExecutorService {
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor()
        }
        return executor!!
    }

    private class ImageLoadRunnable(
        private val context: Context,
        private val isFolderMode: Boolean,
        private val onlyVideo: Boolean,
        private val includeVideo: Boolean,
        private val includeAnimation: Boolean,
        private val excludedImages: List<File>?,
        private val listener: ImageLoaderListener
    ) : Runnable {

        companion object {
            private const val DEFAULT_FOLDER_NAME = "SDCARD"
        }

        private val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        @SuppressLint("InlinedApi")
        private fun queryData(): Cursor? {
            val sourceUri = getSourceUri()

            val type = MediaStore.Files.FileColumns.MEDIA_TYPE

            val selection = when {
                onlyVideo -> "${type}=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}"
                includeVideo -> "$type=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE} OR $type=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}"
                // Empty because we query from image media store
                else -> ""
            }

            val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

            return context.contentResolver.query(
                sourceUri, projection,
                selection, null, sortOrder
            )
        }

        private fun getSourceUri(): Uri {
            return if (onlyVideo || includeVideo) {
                MediaStore.Files.getContentUri("external")
            } else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        private fun cursorToImage(cursor: Cursor): Image? {
            val path = cursor.getString(cursor.getColumnIndex(projection[2]))
            val file = makeSafeFile(path) ?: return null
            if (excludedImages != null && excludedImages.contains(file)) return null

            // Exclude GIF when we don't want it
            if (!includeAnimation) {
                if (ImagePickerUtils.isGifFormat(path)) return null
            }

            val id = cursor.getLong(cursor.getColumnIndex(projection[0]))
            val name = cursor.getString(cursor.getColumnIndex(projection[1]))

            if (name != null) {
                return Image(id, name, path)
            }
            return null
        }

        private fun processData(cursor: Cursor?) {
            if (cursor == null) {
                listener.onFailed(NullPointerException())
                return
            }

            val result: MutableList<Image> = ArrayList()
            val folderMap: MutableMap<String, Folder> = mutableMapOf()

            if (cursor.moveToFirst()) {
                do {
                    val image = cursorToImage(cursor)

                    if (image != null) {
                        result.add(image)

                        // Load folders
                        if (!isFolderMode) continue
                        var bucket = cursor.getString(cursor.getColumnIndex(projection[3]))
                        if (bucket == null) {
                            val parent = File(image.path).parentFile
                            bucket = if (parent != null) parent.name else DEFAULT_FOLDER_NAME
                        }

                        if (bucket != null) {
                            var folder = folderMap[bucket]
                            if (folder == null) {
                                folder = Folder(bucket)
                                folderMap[bucket] = folder
                            }
                            folder.images.add(image)
                        }
                    }

                } while (cursor.moveToNext())
            }
            cursor.close()

            val folders = folderMap.values.toList()
            listener.onImageLoaded(result, folders)
        }

        override fun run() {
            processData(queryData())
        }
    }

    companion object {
        private fun makeSafeFile(path: String?): File? {
            return if (path == null || path.isEmpty()) {
                null
            } else try {
                File(path)
            } catch (ignored: Exception) {
                null
            }
        }
    }
}
