package com.opencloudgaming.opennow

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

/**
 * A third, optional home layout in the spirit of the Nintendo Switch home screen:
 * one large, rounded, artwork-filled tile row that is easy to drive with a TV remote.
 *
 * It is additive — [HomeScreen] (the classic catalog) and every other page stay exactly
 * as they are. The choice is persisted and can be flipped from Settings > Stream.
 */
object HomeUiState {
    /** true = Switch-style home, false = classic catalog. Defaults to the Switch layout. */
    val useSwitchStyle: MutableState<Boolean> = mutableStateOf(true)

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        useSwitchStyle.value = prefs.getBoolean(KEY_SWITCH_HOME, true)
    }

    fun set(context: Context, enabled: Boolean) {
        useSwitchStyle.value = enabled
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SWITCH_HOME, enabled).apply()
    }

    private const val PREFS = "opennow_ui"
    private const val KEY_SWITCH_HOME = "switch_home"
}

private val SwitchBackground = Color(0xFF1B1E24)
private val SwitchPanel = Color(0xFF262B34)
private val SwitchAccent = Color(0xFF3FA9F5)

@Composable
internal fun SwitchStyleHomeScreen(
    state: OpenNowUiState,
    viewModel: OpenNowViewModel,
    tvProfile: Boolean,
) {
    val games = state.games.ifEmpty { state.catalogResult.games }
    val haptics = LocalOpenNowHaptics.current
    val launch: (GameInfo) -> Unit = { game ->
        viewModel.selectGame(game)
        haptics?.play(HapticCue.Activate)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SwitchBackground)
            .padding(horizontal = 40.dp, vertical = 28.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "游戏",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = if (games.isEmpty()) "" else "${games.size} 款",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (tvProfile) "按 OK 开始" else "点击开始",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 15.sp,
            )
        }

        Spacer(Modifier.height(22.dp))

        if (games.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("正在加载游戏库…", color = Color.White.copy(alpha = 0.6f), fontSize = 18.sp)
            }
        } else {
            // Big hero row — the Switch-style focus band.
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(games.take(12), key = { it.id }) { game ->
                    SwitchGameTile(game, size = 176.dp, onClick = { launch(game) })
                }
            }

            Spacer(Modifier.height(28.dp))

            if (games.size > 12) {
                Text(
                    text = "全部游戏",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(14.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(games.drop(12), key = { it.id }) { game ->
                        SwitchGameTile(game, size = 124.dp, onClick = { launch(game) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchGameTile(
    game: GameInfo,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    var focused by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.09f else 1f,
        label = "switch-tile-scale",
    )
    val art = game.imageUrl ?: game.tvCardImageUrl ?: game.screenshotUrl
    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .background(SwitchPanel)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
    ) {
        if (art != null) {
            AsyncImage(
                model = art,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.85f),
                    ),
                ),
        )
        Text(
            text = game.title,
            color = Color.White,
            fontSize = if (size.value >= 160f) 16.sp else 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
        )
        if (focused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .clip(RoundedCornerShape(22.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SwitchAccent.copy(alpha = 0.18f)),
                )
            }
        }
    }
}
