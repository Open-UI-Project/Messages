package org.openui.sms.sms.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.openui.sms.R
import org.openui.sms.ui.theme.AppTheme
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun CropFrameDialog(
    sourceUri: Uri,
    onDismissRequest: () -> Unit,
    onCropConfirmed: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val colors = AppTheme.colors

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(sourceUri) {
        withContext(Dispatchers.IO) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, bounds)
                }
                var sampleSize = 1
                while (bounds.outWidth / sampleSize > 2048 || bounds.outHeight / sampleSize > 2048) {
                    sampleSize *= 2
                }

                val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    sourceBitmap = BitmapFactory.decodeStream(stream, null, options)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.cardElevated)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Crop,
                                contentDescription = null,
                                tint = colors.dialerGreen
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.crop_dialog_title),
                                color = colors.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.crop_dialog_subtitle),
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.crop_dialog_cancel),
                            tint = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = sourceBitmap
                    if (isLoading || bmp == null) {
                        CircularProgressIndicator(color = colors.primaryBrand)
                    } else {
                        InteractiveCropArea(
                            bitmap = bmp,
                            onCropReady = onCropConfirmed,
                            colors = colors,
                            onDismiss = onDismissRequest
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveCropArea(
    bitmap: Bitmap,
    colors: org.openui.sms.ui.theme.AppCustomColors,
    onDismiss: () -> Unit,
    onCropReady: (Bitmap) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val aspect = 9f / 19f

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(aspect)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = max(0.5f, scale * zoom)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    containerSize = IntSize(size.width.roundToInt(), size.height.roundToInt())

                    val bmpWidth = bitmap.width.toFloat()
                    val bmpHeight = bitmap.height.toFloat()

                    val baseScale = max(size.width / bmpWidth, size.height / bmpHeight)
                    val effectiveScale = baseScale * scale

                    val drawWidth = bmpWidth * effectiveScale
                    val drawHeight = bmpHeight * effectiveScale

                    val left = (size.width - drawWidth) / 2f + offsetX
                    val top = (size.height - drawHeight) / 2f + offsetY

                    drawIntoCanvas { canvas ->
                        val matrix = Matrix().apply {
                            postScale(effectiveScale, effectiveScale)
                            postTranslate(left, top)
                        }
                        canvas.nativeCanvas.drawBitmap(bitmap, matrix, null)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.crop_dialog_cancel), color = colors.textSecondary)
                }

                Button(
                    onClick = {
                        val cropped = performCrop(bitmap, containerSize, scale, offsetX, offsetY)
                        onCropReady(cropped)
                    },
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBrand),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.crop_dialog_apply), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun performCrop(
    bitmap: Bitmap,
    containerSize: IntSize,
    userScale: Float,
    offsetX: Float,
    offsetY: Float
): Bitmap {
    val targetWidth = 1080
    val targetHeight = 2280

    val out = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(out)

    val cW = if (containerSize.width > 0) containerSize.width.toFloat() else targetWidth.toFloat()
    val cH = if (containerSize.height > 0) containerSize.height.toFloat() else targetHeight.toFloat()

    val screenToTargetScale = targetWidth.toFloat() / cW

    val bmpWidth = bitmap.width.toFloat()
    val bmpHeight = bitmap.height.toFloat()

    val baseScale = max(cW / bmpWidth, cH / bmpHeight)
    val totalScale = baseScale * userScale * screenToTargetScale

    val left = ((cW - bmpWidth * baseScale * userScale) / 2f + offsetX) * screenToTargetScale
    val top = ((cH - bmpHeight * baseScale * userScale) / 2f + offsetY) * screenToTargetScale

    val matrix = Matrix().apply {
        postScale(totalScale, totalScale)
        postTranslate(left, top)
    }

    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(bitmap, matrix, paint)

    return out
}
