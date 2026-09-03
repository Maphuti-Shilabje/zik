package com.zik.music.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zik.music.ui.theme.AccentMutedBlue
import com.zik.music.ui.theme.PureBlack

@Composable
fun SettingsScreen(
    gaplessEnabled: Boolean = true,
    onToggleGapless: (Boolean) -> Unit = {},
    pauseOnUnplug: Boolean = true,
    onTogglePauseOnUnplug: (Boolean) -> Unit = {},
    filterShortAudio: Boolean = true,
    onToggleFilterShortAudio: (Boolean) -> Unit = {},
    smartFilenameCleaner: Boolean = true,
    onToggleSmartFilenameCleaner: (Boolean) -> Unit = {},
    folderHierarchyFallback: Boolean = true,
    onToggleFolderHierarchyFallback: (Boolean) -> Unit = {},
    onRescanLibrary: () -> Unit,
    onOpenEqualizer: () -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .statusBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures { }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "Settings",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 14.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                item {
                    SettingsSectionHeader("AUDIO & PLAYBACK")

                    // Equalizer Navigation Row
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onOpenEqualizer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Equalizer & Sound Effects",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "5-band curve, Bass Boost, 3D audio, & presets",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.60f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = null,
                                tint = AccentMutedBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Sleep Timer Navigation Row
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onOpenSleepTimer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sleep Timer",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Auto-pause with gradual volume fade-out",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.60f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = AccentMutedBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    SettingsToggleRow(
                        title = "Gapless Playback",
                        subtitle = "Seamless transitions between consecutive tracks",
                        checked = gaplessEnabled,
                        onCheckedChange = onToggleGapless
                    )
                    SettingsToggleRow(
                        title = "Pause on Disconnect",
                        subtitle = "Auto-pause playback when headphones are unplugged",
                        checked = pauseOnUnplug,
                        onCheckedChange = onTogglePauseOnUnplug
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsSectionHeader("LIBRARY & SCANNER")
                    SettingsToggleRow(
                        title = "Filter Short Audio (< 15s)",
                        subtitle = "Automatically exclude notification tones and voice notes",
                        checked = filterShortAudio,
                        onCheckedChange = onToggleFilterShortAudio
                    )

                    // Rescan Action Card
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Rescan Library",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                                Text(
                                    text = "Refresh local audio, folders, and cover art",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                            }
                            Button(
                                onClick = onRescanLibrary,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = AccentMutedBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(text = "Scan", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsSectionHeader("METADATA & SANITIZATION")
                    SettingsToggleRow(
                        title = "Smart Filename Cleaner",
                        subtitle = "Strips [y2mate], bitrates, and video tags from song names",
                        checked = smartFilenameCleaner,
                        onCheckedChange = onToggleSmartFilenameCleaner
                    )
                    SettingsToggleRow(
                        title = "Folder Hierarchy Fallback",
                        subtitle = "Uses folder names when album/artist tags are missing",
                        checked = folderHierarchyFallback,
                        onCheckedChange = onToggleFolderHierarchyFallback
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsSectionHeader("PRIVACY & APP INFO")
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Zero Telemetry • 100% Offline",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentMutedBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Zik has zero internet permissions. Your listening history and music files never leave your device.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.70f),
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Version 1.0.0 (Pure Kotlin • Compose M3 • Media3)",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.40f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = AccentMutedBlue,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 6.dp)
    )
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PureBlack,
                    checkedTrackColor = AccentMutedBlue,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.60f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
                    uncheckedBorderColor = Color.White.copy(alpha = 0.25f)
                )
            )
        }
    }
}
