package org.openui.sms.sms.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import org.openui.sms.BuildConfig
import org.openui.sms.R
import org.openui.sms.data.AppSettings
import org.openui.sms.ui.theme.AppTheme

private enum class SettingsPage { ROOT, APPEARANCE, STYLE, WALLPAPER }

@Composable
fun SettingsDialog(
    onDismissRequest: () -> Unit,
    onAboutClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val appSettings = remember(context) { AppSettings.getInstance(context) }

    val themeMode by appSettings.themeMode.collectAsState()
    val designStyle by appSettings.designStyle.collectAsState()
    val monetEnabled by appSettings.monetEnabled.collectAsState()
    val ghostMode by appSettings.ghostMode.collectAsState()
    val wallpaperUri by appSettings.wallpaperUri.collectAsState()
    val wallpaperType by appSettings.wallpaperType.collectAsState()

    var page by remember { mutableStateOf(SettingsPage.ROOT) }
    var cropDialogUri by remember { mutableStateOf<Uri?>(null) }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri)?.lowercase() ?: ""
            if (mime.startsWith("video")) {
                appSettings.saveWallpaperFromUri(uri, isVideo = true)
                Toast.makeText(context, context.getString(R.string.wallpaper_set_toast), Toast.LENGTH_SHORT).show()
            } else {
                cropDialogUri = uri
            }
        }
    }

    BackHandler(enabled = page != SettingsPage.ROOT) { page = SettingsPage.ROOT }

    val themeLabels = mapOf(
        AppSettings.THEME_SYSTEM to stringResource(R.string.settings_theme_system),
        AppSettings.THEME_DARK to stringResource(R.string.settings_theme_dark),
        AppSettings.THEME_LIGHT to stringResource(R.string.settings_theme_light)
    )
    val styleLabels = mapOf(
        AppSettings.STYLE_OPENUI to stringResource(R.string.settings_style_openui),
        AppSettings.STYLE_MATERIAL3_EXPRESSIVE to stringResource(R.string.settings_style_expressive)
    )
    val themeLabel = themeLabels[themeMode] ?: themeLabels.getValue(AppSettings.THEME_SYSTEM)
    val styleLabel = styleLabels[designStyle] ?: styleLabels.getValue(AppSettings.STYLE_OPENUI)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                SettingsTopBar(
                    title = when (page) {
                        SettingsPage.ROOT -> stringResource(R.string.settings_title)
                        SettingsPage.APPEARANCE -> stringResource(R.string.settings_theme_title)
                        SettingsPage.STYLE -> stringResource(R.string.settings_style_title)
                        SettingsPage.WALLPAPER -> stringResource(R.string.wallpaper_title)
                    },
                    isRoot = page == SettingsPage.ROOT,
                    onBack = { page = SettingsPage.ROOT },
                    onClose = onDismissRequest
                )

                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        if (forward) {
                            (slideInHorizontally(tween(240)) { it / 3 } + fadeIn(tween(200)))
                                .togetherWith(slideOutHorizontally(tween(240)) { -it / 4 } + fadeOut(tween(160)))
                        } else {
                            (slideInHorizontally(tween(240)) { -it / 4 } + fadeIn(tween(200)))
                                .togetherWith(slideOutHorizontally(tween(240)) { it / 3 } + fadeOut(tween(160)))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "settings_page"
                ) { currentPage ->
                    when (currentPage) {
                        SettingsPage.ROOT -> SettingsRootPage(
                            themeLabel = themeLabel,
                            styleLabel = styleLabel,
                            ghostMode = ghostMode,
                            onGhostModeChange = appSettings::setGhostMode,
                            onAppearanceClick = { page = SettingsPage.APPEARANCE },
                            onAboutClick = {
                                onDismissRequest()
                                onAboutClick?.invoke()
                            }
                        )

                        SettingsPage.APPEARANCE -> SettingsAppearancePage(
                            themeMode = themeMode,
                            onThemeModeChange = appSettings::setThemeMode,
                            styleLabel = styleLabel,
                            showMonet = designStyle == AppSettings.STYLE_MATERIAL3_EXPRESSIVE,
                            monetEnabled = monetEnabled,
                            onMonetChange = appSettings::setMonetEnabled,
                            wallpaperSubtitle = if (wallpaperType == AppSettings.TYPE_NONE) {
                                stringResource(R.string.settings_wallpaper_not_set)
                            } else {
                                wallpaperSubtitle(wallpaperType)
                            },
                            onStyleClick = { page = SettingsPage.STYLE },
                            onWallpaperClick = { page = SettingsPage.WALLPAPER }
                        )

                        SettingsPage.STYLE -> SettingsStylePage(
                            designStyle = designStyle,
                            onDesignStyleChange = appSettings::setDesignStyle
                        )

                        SettingsPage.WALLPAPER -> SettingsWallpaperPage(
                            wallpaperUri = wallpaperUri,
                            wallpaperType = wallpaperType,
                            onChooseMedia = { mediaPickerLauncher.launch("*/*") },
                            onRemoveWallpaper = {
                                appSettings.clearWallpaper()
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.wallpaper_removed_toast),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }
        }
    }

    if (cropDialogUri != null) {
        CropFrameDialog(
            sourceUri = cropDialogUri!!,
            onDismissRequest = { cropDialogUri = null },
            onCropConfirmed = { croppedBitmap ->
                appSettings.saveWallpaperFromBitmap(croppedBitmap)
                cropDialogUri = null
                Toast.makeText(context, context.getString(R.string.wallpaper_set_toast), Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun wallpaperSubtitle(type: String): String = when (type) {
    AppSettings.TYPE_VIDEO -> stringResource(R.string.wallpaper_type_video)
    AppSettings.TYPE_GIF -> stringResource(R.string.wallpaper_type_gif)
    else -> stringResource(R.string.wallpaper_type_photo)
}
@Composable
private fun SettingsTopBar(
    title: String,
    isRoot: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = if (isRoot) onClose else onBack) {
            Icon(
                imageVector = if (isRoot) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.about_close),
                tint = colors.textPrimary
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun SettingsRootPage(
    themeLabel: String,
    styleLabel: String,
    ghostMode: Boolean,
    onGhostModeChange: (Boolean) -> Unit,
    onAppearanceClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    val colors = AppTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp)
    ) {
        item { SettingsSectionHeader(stringResource(R.string.settings_section_appearance)) }
        item {
            SettingsGroup {
                SettingsNavRow(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.settings_theme_title),
                    subtitle = "$themeLabel · $styleLabel",
                    onClick = onAppearanceClick
                )
            }
        }

        item { SettingsSectionHeader(stringResource(R.string.settings_section_privacy)) }
        item {
            SettingsGroup {
                SettingsSwitchRow(
                    icon = Icons.Rounded.VisibilityOff,
                    title = stringResource(R.string.ghost_mode_title),
                    subtitle = stringResource(R.string.ghost_mode_subtitle),
                    checked = ghostMode,
                    onCheckedChange = onGhostModeChange
                )
            }
        }

        item { SettingsSectionHeader(stringResource(R.string.settings_section_about)) }
        item {
            SettingsGroup {
                SettingsNavRow(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.about_app),
                    subtitle = stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME),
                    onClick = onAboutClick
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.about_title) + " • v" + BuildConfig.VERSION_NAME,
                modifier = Modifier.padding(start = 12.dp, top = 28.dp),
                color = colors.textMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    val colors = AppTheme.colors
    Text(
        text = text,
        modifier = Modifier.padding(start = 12.dp, top = 22.dp, bottom = 8.dp),
        color = colors.primaryBrand,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.card)
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider() {
    val colors = AppTheme.colors
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp),
        thickness = 0.5.dp,
        color = colors.divider
    )
}



@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = AppTheme.colors.textMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = AppTheme.colors
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onAccent,
                    checkedTrackColor = colors.primaryBrand,
                    uncheckedThumbColor = colors.textSecondary,
                    uncheckedTrackColor = colors.cardElevated,
                    uncheckedBorderColor = colors.divider
                )
            )
        }
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.cardElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primaryBrand,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 2
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        trailing()
    }
}

@Composable
private fun SettingsThemeRadioRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = colors.primaryBrand,
                unselectedColor = colors.textMuted
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = subtitle,
                color = colors.textSecondary,
                fontSize = 12.sp
            )
        }
    }
}
@Composable
private fun SettingsAppearancePage(
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    styleLabel: String,
    showMonet: Boolean,
    monetEnabled: Boolean,
    onMonetChange: (Boolean) -> Unit,
    wallpaperSubtitle: String,
    onStyleClick: () -> Unit,
    onWallpaperClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp)
    ) {
        item { SettingsSectionHeader(stringResource(R.string.settings_theme_title)) }
        item {
            SettingsGroup {
                SettingsThemeRadioRow(
                    title = stringResource(R.string.settings_theme_system),
                    subtitle = stringResource(R.string.settings_theme_system_desc),
                    selected = themeMode == AppSettings.THEME_SYSTEM,
                    onClick = { onThemeModeChange(AppSettings.THEME_SYSTEM) }
                )
                SettingsDivider()
                SettingsThemeRadioRow(
                    title = stringResource(R.string.settings_theme_dark),
                    subtitle = stringResource(R.string.settings_theme_dark_desc),
                    selected = themeMode == AppSettings.THEME_DARK,
                    onClick = { onThemeModeChange(AppSettings.THEME_DARK) }
                )
                SettingsDivider()
                SettingsThemeRadioRow(
                    title = stringResource(R.string.settings_theme_light),
                    subtitle = stringResource(R.string.settings_theme_light_desc),
                    selected = themeMode == AppSettings.THEME_LIGHT,
                    onClick = { onThemeModeChange(AppSettings.THEME_LIGHT) }
                )
            }
        }

        item { SettingsSectionHeader(stringResource(R.string.settings_style_title)) }
        item {
            SettingsGroup {
                SettingsNavRow(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.settings_style_title),
                    subtitle = styleLabel,
                    onClick = onStyleClick
                )
            }
        }

        if (showMonet) {
            item { SettingsSectionHeader(stringResource(R.string.settings_material_you_title)) }
            item {
                SettingsGroup {
                    SettingsSwitchRow(
                        icon = Icons.Rounded.DarkMode,
                        title = stringResource(R.string.settings_monet_title),
                        subtitle = stringResource(R.string.settings_monet_subtitle),
                        checked = monetEnabled,
                        onCheckedChange = onMonetChange
                    )
                }
            }
            item { MonetPalettePreview() }
        }

        item { SettingsSectionHeader(stringResource(R.string.wallpaper_title)) }
        item {
            SettingsGroup {
                SettingsNavRow(
                    icon = Icons.Rounded.Wallpaper,
                    title = stringResource(R.string.wallpaper_title),
                    subtitle = wallpaperSubtitle,
                    onClick = onWallpaperClick
                )
            }
        }
    }
}

@Composable
private fun MonetPalettePreview() {
    val scheme = MaterialTheme.colorScheme
    val colors = AppTheme.colors
    val palette = listOf(
        scheme.primary,
        scheme.secondary,
        scheme.tertiary,
        scheme.primaryContainer,
        scheme.secondaryContainer
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        palette.forEach { color ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun SettingsStylePage(
    designStyle: String,
    onDesignStyleChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp)
    ) {
        item { SettingsSectionHeader(stringResource(R.string.settings_style_title)) }
        item {
            SettingsGroup {
                StyleOption(
                    title = stringResource(R.string.settings_style_openui),
                    subtitle = stringResource(R.string.settings_style_openui_desc),
                    selected = designStyle == AppSettings.STYLE_OPENUI,
                    onClick = { onDesignStyleChange(AppSettings.STYLE_OPENUI) }
                )
                SettingsDivider()
                StyleOption(
                    title = stringResource(R.string.settings_style_expressive),
                    subtitle = stringResource(R.string.settings_style_expressive_desc),
                    selected = designStyle == AppSettings.STYLE_MATERIAL3_EXPRESSIVE,
                    onClick = { onDesignStyleChange(AppSettings.STYLE_MATERIAL3_EXPRESSIVE) }
                )
            }
        }
    }
}

@Composable
private fun StyleOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = colors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 17.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (selected) colors.primaryBrand else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(17.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colors.divider)
                )
            }
        }
    }
}

@Composable
private fun SettingsWallpaperPage(
    wallpaperUri: String?,
    wallpaperType: String,
    onChooseMedia: () -> Unit,
    onRemoveWallpaper: () -> Unit
) {
    val colors = AppTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp)
    ) {
        item { SettingsSectionHeader(stringResource(R.string.wallpaper_title)) }
        if (wallpaperUri != null) {
            item {
                SettingsGroup {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.cardElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            if (wallpaperType == AppSettings.TYPE_VIDEO) {
                                Icon(
                                    imageVector = Icons.Rounded.Wallpaper,
                                    contentDescription = null,
                                    tint = colors.primaryBrand,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                AsyncImage(
                                    model = wallpaperUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.wallpaper_active_label),
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = wallpaperSubtitle(wallpaperType),
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = onRemoveWallpaper) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.wallpaper_remove_btn),
                                tint = colors.actionRed
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Text(
                    text = stringResource(R.string.wallpaper_subtitle),
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp, end = 12.dp),
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        item {
            Button(
                onClick = onChooseMedia,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryBrand,
                    contentColor = colors.onAccent
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Crop,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.wallpaper_choose_btn),
                    color = colors.onAccent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

