package com.radiopure.app.radiopure.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radiopure.app.radiopure.catalog.RadioStationCatalog
import com.radiopure.app.radiopure.model.RadioStation
import com.radiopure.app.radiopure.player.RadioPlayerState
import com.radiopure.app.radiopure.ui.theme.RadioPureBackground

private val DividerColor = Color.White.copy(alpha = 0.1f)
private val SubtleDividerColor = Color.White.copy(alpha = 0.07f)

@Composable
fun ContentScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val playerState by viewModel.playerState.collectAsState()
    val stations = RadioStationCatalog.all

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RadioPureBackground),
    ) {
        HeaderSection(playerState = playerState)
        HorizontalDivider(color = DividerColor)
        StationListSection(
            stations = stations,
            playerState = playerState,
            onStationClick = { viewModel.play(it) },
            modifier = Modifier.weight(1f),
        )
        HorizontalDivider(color = DividerColor)
        ControlsSection(
            playerState = playerState,
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onVolumeChange = { viewModel.setVolume(it) },
        )
    }
}

@Composable
private fun HeaderSection(playerState: RadioPlayerState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "RadioPure",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.4f),
            letterSpacing = 3.sp,
        )
        val station = playerState.currentStation
        if (station != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    playerState.isError -> StatusDot(Color.Red)
                    playerState.isPlaying -> StatusDot(Color(0xFF4CD964))
                }
                Text(
                    text = station.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (playerState.isError) {
                        Color.White.copy(alpha = 0.6f)
                    } else {
                        Color.White
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Text(
                text = "选择一个电台开始收听",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun StationListSection(
    stations: List<RadioStation>,
    playerState: RadioPlayerState,
    onStationClick: (RadioStation) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        itemsIndexed(stations) { index, station ->
            val isSelected = playerState.currentStation?.id == station.id
            val isPlaying = isSelected && playerState.isPlaying
            val isError = isSelected && playerState.isError
            StationRow(
                station = station,
                isSelected = isSelected,
                isPlaying = isPlaying,
                isError = isError,
                onClick = { onStationClick(station) },
            )
            if (index < stations.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 60.dp),
                    color = SubtleDividerColor,
                )
            }
        }
    }
}

@Composable
private fun StationRow(
    station: RadioStation,
    isSelected: Boolean,
    isPlaying: Boolean,
    isError: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) Color.White.copy(alpha = 0.04f) else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isSelected) Color.White.copy(alpha = 0.15f)
                    else Color.White.copy(alpha = 0.06f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = station.emoji, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = station.name,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
            )
            Text(
                text = when {
                    isError -> "加载失败，请重试"
                    isPlaying -> "正在播放"
                    else -> "点击收听"
                },
                fontSize = 12.sp,
                color = when {
                    isError -> Color.Red.copy(alpha = 0.8f)
                    isPlaying -> Color(0xFF4CD964)
                    else -> Color.White.copy(alpha = 0.3f)
                },
            )
        }
        when {
            isSelected && isError -> Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = Color.Red.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp),
            )
            isSelected && isPlaying -> Icon(
                Icons.Default.GraphicEq,
                contentDescription = null,
                tint = Color(0xFF4CD964),
                modifier = Modifier.size(16.dp),
            )
            isSelected -> Icon(
                Icons.Default.PauseCircle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp),
            )
            else -> Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun ControlsSection(
    playerState: RadioPlayerState,
    onTogglePlayPause: () -> Unit,
    onVolumeChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val hasStation = playerState.currentStation != null
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (hasStation) Color.White else Color.White.copy(alpha = 0.2f),
                )
                .clickable(enabled = hasStation, onClick = onTogglePlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playerState.isPlaying) "暂停" else "播放",
                tint = if (hasStation) RadioPureBackground else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.VolumeDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Slider(
                value = playerState.volume,
                onValueChange = onVolumeChange,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White.copy(alpha = 0.9f),
                    activeTrackColor = Color.White.copy(alpha = 0.7f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                ),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
