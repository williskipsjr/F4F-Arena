package com.f4f.arena.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f4f.arena.ui.theme.F4FColors
import java.io.ByteArrayOutputStream

@Composable
fun PostMatchUpdateDialog(
    matchTitle: String,
    playerCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (turfFee: Int?, photoDataUrl: String?) -> Unit
) {
    val context = LocalContext.current
    var turfFeeText by remember { mutableStateOf("") }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var photoDataUrl by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            photoBitmap = bmp
            photoDataUrl = encodeBitmapToDataUrl(bmp)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val decoded = BitmapFactory.decodeStream(stream)
                    if (decoded != null) {
                        val scaled = scaleBitmapMax(decoded, 960)
                        photoBitmap = scaled
                        photoDataUrl = encodeBitmapToDataUrl(scaled)
                    }
                }
            }
        }
    }

    val feeInt = turfFeeText.toIntOrNull()
    val effectivePlayers = playerCount.coerceAtLeast(1)
    val perPlayer = feeInt?.let { (it + effectivePlayers - 1) / effectivePlayers }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("End Match & Post-Match Update") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Wrap up \"$matchTitle\" with a squad photo and turf fee split.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = F4FColors.TextSecondary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { cameraLauncher.launch(null) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Camera", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gallery", fontSize = 12.sp)
                    }
                }

                if (photoBitmap != null) {
                    Image(
                        bitmap = photoBitmap!!.asImageBitmap(),
                        contentDescription = "Post-match photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }

                OutlinedTextField(
                    value = turfFeeText,
                    onValueChange = { if (it.all(Char::isDigit)) turfFeeText = it },
                    label = { Text("Total Turf / Ground Fee (₹, optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (perPlayer != null && feeInt > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Split across $effectivePlayers players: ₹$perPlayer / player",
                            color = F4FColors.ForestDeep,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(feeInt, photoDataUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = F4FColors.ForestDeep)
            ) {
                Text("End & Publish", color = F4FColors.PitchLime, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun decodeDataUrlToBitmap(dataUrl: String?): Bitmap? {
    if (dataUrl.isNullOrBlank()) return null
    val base64Part = dataUrl.substringAfter("base64,", "")
    if (base64Part.isBlank()) return null
    return runCatching {
        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

private fun scaleBitmapMax(src: Bitmap, maxDim: Int): Bitmap {
    val w = src.width
    val h = src.height
    if (w <= maxDim && h <= maxDim) return src
    val ratio = maxDim.toFloat() / maxOf(w, h)
    return Bitmap.createScaledBitmap(src, (w * ratio).toInt(), (h * ratio).toInt(), true)
}

private fun encodeBitmapToDataUrl(bmp: Bitmap): String {
    val scaled = scaleBitmapMax(bmp, 800)
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
    val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    return "data:image/jpeg;base64,$b64"
}
