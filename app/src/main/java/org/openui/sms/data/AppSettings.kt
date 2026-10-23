package org.openui.sms.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

class AppSettings(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("openui_sms_settings", Context.MODE_PRIVATE)

    private val _ghostMode = MutableStateFlow(prefs.getBoolean(KEY_GHOST_MODE, false))
    val ghostMode: StateFlow<Boolean> = _ghostMode.asStateFlow()

    private val _wallpaperUri = MutableStateFlow(prefs.getString(KEY_WALLPAPER_URI, null))
    val wallpaperUri: StateFlow<String?> = _wallpaperUri.asStateFlow()

    private val _wallpaperType = MutableStateFlow(prefs.getString(KEY_WALLPAPER_TYPE, TYPE_NONE) ?: TYPE_NONE)
    val wallpaperType: StateFlow<String> = _wallpaperType.asStateFlow()

    fun isGhostMode(): Boolean = _ghostMode.value

    fun setGhostMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GHOST_MODE, enabled).apply()
        _ghostMode.value = enabled
    }

    fun saveWallpaperFromUri(sourceUri: Uri, isVideo: Boolean): Boolean {
        return try {
            val extension = if (isVideo) "mp4" else "media"
            val targetFile = File(context.filesDir, "custom_wallpaper.$extension")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            val savedUriString = Uri.fromFile(targetFile).toString()
            val type = if (isVideo) TYPE_VIDEO else {
                val mime = context.contentResolver.getType(sourceUri)?.lowercase() ?: ""
                if (mime.contains("gif") || sourceUri.toString().contains(".gif", ignoreCase = true)) {
                    TYPE_GIF
                } else {
                    TYPE_IMAGE
                }
            }

            prefs.edit()
                .putString(KEY_WALLPAPER_URI, savedUriString)
                .putString(KEY_WALLPAPER_TYPE, type)
                .apply()

            _wallpaperUri.value = savedUriString
            _wallpaperType.value = type
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun clearWallpaper() {
        try {
            val file1 = File(context.filesDir, "custom_wallpaper.media")
            if (file1.exists()) file1.delete()
            val file2 = File(context.filesDir, "custom_wallpaper.mp4")
            if (file2.exists()) file2.delete()
        } catch (_: Exception) {}

        prefs.edit()
            .remove(KEY_WALLPAPER_URI)
            .remove(KEY_WALLPAPER_TYPE)
            .apply()

        _wallpaperUri.value = null
        _wallpaperType.value = TYPE_NONE
    }

    companion object {
        private const val KEY_GHOST_MODE = "ghost_mode"
        private const val KEY_WALLPAPER_URI = "wallpaper_uri"
        private const val KEY_WALLPAPER_TYPE = "wallpaper_type"

        const val TYPE_NONE = "none"
        const val TYPE_IMAGE = "image"
        const val TYPE_GIF = "gif"
        const val TYPE_VIDEO = "video"

        @Volatile
        private var instance: AppSettings? = null

        fun getInstance(context: Context): AppSettings {
            return instance ?: synchronized(this) {
                instance ?: AppSettings(context.applicationContext).also { instance = it }
            }
        }
    }
}
