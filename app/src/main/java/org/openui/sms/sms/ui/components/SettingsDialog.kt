package org.openui.sms.sms.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import org.openui.sms.R
import org.openui.sms.data.AppSettings
import org.openui.sms.ui.theme.OpenUICardBackground
import org.openui.sms.ui.theme.OpenUIOrangeBadge
import org.openui.sms.ui.theme.OpenUISurface
import org.openui.sms.ui.theme.OpenUITextSecondary
import java.io.File

@Composable
fun SettingsDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val appSettings = remember(context) { AppSettings.getInstance(context) }
    val isGhostMode by appSettings.ghostMode.collectAsState()
    val wallpaperUri by appSettings.wallpaperUri.collectAsState()
    val wallpaperType by appSettings.wallpaperType.collectAsState()

    var isGhostDescExpanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (isGhostDescExpanded) 180f else 0f,
        animationSpec = tween(250),
        label = "arrowRotation"
    )

    var pendingCropSourceUri by remember { mutableStateOf<Uri?>(null) }
    var tempCropOutputFile by remember { mutableStateOf<File?>(null) }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val file = tempCropOutputFile
            if (file != null && file.exists() && file.length() > 0) {
                appSettings.saveWallpaperFromUri(Uri.fromFile(file), isVideo = false)
                Toast.makeText(context, context.getString(R.string.wallpaper_set_toast), Toast.LENGTH_SHORT).show()
            } else {
                val dataUri = result.data?.data
                if (dataUri != null) {
                    appSettings.saveWallpaperFromUri(dataUri, isVideo = false)
                    Toast.makeText(context, context.getString(R.string.wallpaper_set_toast), Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            pendingCropSourceUri?.let { uri ->
                appSettings.saveWallpaperFromUri(uri, isVideo = false)
                Toast.makeText(context, context.getString(R.string.wallpaper_set_toast), Toast.LENGTH_SHORT).show()
            }
        }
        pendingCropSourceUri = null
        tempCropOutputFile = null
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri)?.lowercase() ?: ""
            val isVideo = mime.startsWith("video")
            val isGif = mime.contains("gif") || uri.toString().contains(".gif", ignoreCase = true)

            if (isVideo) {
                appSettings.saveWallpaperFromUri(uri, isVideo = true)
                Toast.makeText(context, context.getString(R.string.wallpaper_set_toast), Toast.LENGTH_SHORT).show()
            } else if (isGif) {
                appSettings.saveWallpaperFromUri(uri, isVideo = false)
                Toast.makeText(context, context.getString(R.string.wallpaper_set_toast), Toast.LENGTH_SHORT).show()
            } else {
                try {
                    val cropFile = File(context.cacheDir, "crop_temp_${System.currentTimeMillis()}.jpg")
                    tempCropOutputFile = cropFile
                    pendingCropSourceUri = uri
                    val outputUri = Uri.fromFile(cropFile)

                    val cropIntent = Intent("com.android.camera.action.CROP").apply {
                        setDataAndType(uri, "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        putExtra("crop", "true")
                        putExtra("aspectX", 9)
                        putExtra("aspectY", 19)
                        putExtra("scale", true)
                        putExtra("return-data", false)
                        putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
                    }
                    cropLauncher.launch(cropIntent)
                } catch (_: Exception) {
                    appSettings.saveWallpaperFromUri(uri, isVideo = false)
                    Toast.makeText(context, context.getString(R.string.wallpaper_set_toast), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = OpenUISurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.about_close),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(OpenUICardBackground)
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(OpenUIOrangeBadge.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VisibilityOff,
                                    contentDescription = null,
                                    tint = OpenUIOrangeBadge,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.ghost_mode_title),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.ghost_mode_subtitle),
                                    color = OpenUITextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Switch(
                                checked = isGhostMode,
                                onCheckedChange = { appSettings.setGhostMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = OpenUIOrangeBadge,
                                    uncheckedThumbColor = OpenUITextSecondary,
                                    uncheckedTrackColor = OpenUISurface
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { isGhostDescExpanded = !isGhostDescExpanded }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isGhostDescExpanded) stringResource(R.string.about_close) else stringResource(R.string.ghost_mode_learn_more),
                                color = OpenUIOrangeBadge,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = OpenUIOrangeBadge,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(arrowRotation)
                            )
                        }

                        AnimatedVisibility(
                            visible = isGhostDescExpanded,
                            enter = expandVertically(animationSpec = tween(240)) + fadeIn(animationSpec = tween(240)),
                            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(160))
                        ) {
                            Text(
                                text = stringResource(R.string.ghost_mode_desc),
                                color = OpenUITextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(OpenUICardBackground)
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF388E3C).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Wallpaper,
                                    contentDescription = null,
                                    tint = Color(0xFF81C784),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.wallpaper_title),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.wallpaper_subtitle),
                                    color = OpenUITextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (wallpaperUri != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(OpenUISurface)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp, 72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (wallpaperType == AppSettings.TYPE_VIDEO) {
                                        Icon(
                                            imageVector = Icons.Rounded.Wallpaper,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    } else {
                                        AsyncImage(
                                            model = wallpaperUri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.matchParentSize()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.wallpaper_active_label),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (wallpaperType == AppSettings.TYPE_VIDEO) "Видео (MP4)" else if (wallpaperType == AppSettings.TYPE_GIF) "Анимация (GIF)" else "Фотография",
                                        color = OpenUITextSecondary,
                                        fontSize = 12.sp
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        appSettings.clearWallpaper()
                                        Toast.makeText(context, context.getString(R.string.wallpaper_removed_toast), Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = stringResource(R.string.wallpaper_remove_btn),
                                        tint = Color(0xFFEF5350)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Button(
                            onClick = {
                                mediaPickerLauncher.launch("*/*")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OpenUIOrangeBadge),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Crop,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.wallpaper_choose_btn),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = stringResource(R.string.about_close),
                        color = OpenUITextSecondary,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
