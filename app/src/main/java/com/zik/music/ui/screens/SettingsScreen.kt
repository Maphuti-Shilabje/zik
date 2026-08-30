package com.zik.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.zik.music.ui.theme.AccentMutedBlue
import com.zik.music.ui.theme.PureBlack
import com.zik.music.ui.theme.SurfaceLevel1
import com.zik.music.ui.theme.SurfaceLevel2
import com.zik.music.ui.theme.TextDisabled
import com.zik.music.ui.theme.TextPrimary
import com.zik.music.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    onRescanLibrary: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var gaplessEnabled by remember { mutableStateOf(true) }
    var pauseOnUnplug by remember { mutableStateOf(true) }
    var filterShortAudio by remember { mutableStateOf(true) }
    var smartFilenameCleaner by remember { mutableStateOf(true) }
    var folderHierarchyFallback by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                SettingsSectionHeader("AUDIO & PLAYBACK")
                SettingsToggleRow(
                    title = "Gapless Playback",
                    subtitle = "Seamless transitions between consecutive audio tracks",
                    checked = gaplessEnabled,
                    onCheckedChange = { gaplessEnabled = it }
                )
                SettingsToggleRow(
                    title = "Pause on Disconnect",
                    subtitle = "Auto-pause playback when headphones are unplugged",
                    checked = pauseOnUnplug,
                    onCheckedChange = { pauseOnUnplug = it }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader("LIBRARY & SCANNER")
                SettingsToggleRow(
                    title = "Filter Short Audio (< 15s)",
                    subtitle = "Automatically exclude notification tones and voice notes",
                    checked = filterShortAudio,
                    onCheckedChange = { filterShortAudio = it }
                )
                
                // Rescan Action Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceLevel1)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rescan Library",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Refresh files, folders, and embedded art",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Button(
                        onClick = onRescanLibrary,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceLevel2)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = AccentMutedBlue,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(text = "Scan", color = TextPrimary)
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
                    onCheckedChange = { smartFilenameCleaner = it }
                )
                SettingsToggleRow(
                    title = "Folder Hierarchy Fallback",
                    subtitle = "Uses folder names when album/artist tags are missing",
                    checked = folderHierarchyFallback,
                    onCheckedChange = { folderHierarchyFallback = it }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader("PRIVACY & APP INFO")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceLevel1)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Zero Telemetry • 100% Offline",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AccentMutedBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Zik has zero internet permissions. Your listening history and music files never leave your device.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Version 1.0.0 (Pure Kotlin • Compose M3 • Media3)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDisabled
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = AccentMutedBlue,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceLevel1)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PureBlack,
                checkedTrackColor = AccentMutedBlue,
                uncheckedThumbColor = TextDisabled,
                uncheckedTrackColor = SurfaceLevel2
            )
        )
    }
}
