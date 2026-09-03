package com.zik.music.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zik.music.data.AudioDetails
import com.zik.music.ui.theme.AccentMutedBlue
import com.zik.music.ui.theme.PureBlack

@Composable
fun AudioInfoSheet(
    details: AudioDetails,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = PureBlack,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Drag handle pill
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AccentMutedBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Audio Inspector",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Codec & Hi-Res Summary Hero Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AccentMutedBlue.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, AccentMutedBlue.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = details.codec,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (details.isHiRes) {
                                    BadgePill(text = "HI-RES", isAccent = true)
                                }
                                BadgePill(
                                    text = if (details.isLossless) "LOSSLESS" else "LOSSY",
                                    isAccent = details.isLossless
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val sampleRateFormatted = details.sampleRateHz?.let { "${it / 1000.0} kHz" } ?: "44.1 kHz"
                        val bitDepthFormatted = details.bitDepth?.let { "$it-bit" } ?: ""
                        val bitrateFormatted = details.bitrateKbps?.let { "$it kbps" } ?: ""

                        val specsSummary = listOfNotNull(
                            sampleRateFormatted,
                            bitDepthFormatted.ifEmpty { null },
                            bitrateFormatted.ifEmpty { null }
                        ).joinToString(" | ")

                        Text(
                            text = specsSummary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentMutedBlue
                        )
                    }
                }

                // 2. Audio Stream Specifications Section
                InspectorSection(
                    title = "AUDIO STREAM SPECIFICATIONS",
                    icon = Icons.Default.GraphicEq
                ) {
                    InspectorRow(label = "Format / Codec", value = details.codec)
                    InspectorRow(
                        label = "Sample Rate",
                        value = details.sampleRateHz?.let { "$it Hz (${it / 1000.0} kHz)" } ?: "44100 Hz (44.1 kHz)"
                    )
                    InspectorRow(
                        label = "Bitrate",
                        value = details.bitrateKbps?.let { "$it kbps" } ?: "Variable / Stream"
                    )
                    InspectorRow(
                        label = "Channels",
                        value = when (details.channelCount) {
                            1 -> "Mono (1.0)"
                            2 -> "Stereo (2.0)"
                            6 -> "5.1 Surround"
                            else -> "${details.channelCount ?: 2} Channels"
                        }
                    )
                    if (details.bitDepth != null) {
                        InspectorRow(label = "Bit Depth", value = "${details.bitDepth}-bit")
                    }
                    InspectorRow(label = "MIME Type", value = details.mimeType)
                }

                // 3. File & Storage Details Section
                InspectorSection(
                    title = "FILE & STORAGE DETAILS",
                    icon = Icons.Default.Folder
                ) {
                    InspectorRow(label = "File Size", value = details.fileSizeFormatted)
                    InspectorRow(label = "File Name", value = details.fileName)

                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Storage Path",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.50f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("File Path", details.filePath)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Path copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = details.filePath,
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Path",
                                    tint = AccentMutedBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Metadata Tags Section
                InspectorSection(
                    title = "METADATA & TAGS",
                    icon = Icons.Default.LibraryMusic
                ) {
                    InspectorRow(label = "Title", value = details.title)
                    InspectorRow(label = "Artist", value = details.artist)
                    InspectorRow(label = "Album", value = details.album)
                    details.albumArtist?.let { InspectorRow(label = "Album Artist", value = it) }
                    details.genre?.let { InspectorRow(label = "Genre", value = it) }
                    details.year?.let { InspectorRow(label = "Year / Date", value = it) }
                    details.trackNumber?.let { InspectorRow(label = "Track Number", value = it) }
                    details.discNumber?.let { InspectorRow(label = "Disc Number", value = it) }
                    details.composer?.let { InspectorRow(label = "Composer", value = it) }
                }
            }
        }
    }
}

@Composable
private fun InspectorSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentMutedBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentMutedBlue
                )
            }

            content()
        }
    }
}

@Composable
private fun InspectorRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.55f)
        )
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BadgePill(
    text: String,
    isAccent: Boolean
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isAccent) AccentMutedBlue.copy(alpha = 0.16f) else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (isAccent) AccentMutedBlue.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.15f)
        )
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAccent) AccentMutedBlue else Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
