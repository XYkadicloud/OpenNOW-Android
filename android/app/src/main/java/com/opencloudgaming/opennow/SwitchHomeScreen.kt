package com.opencloudgaming.opennow

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A third, optional home layout modelled on the Nintendo Switch HOME menu: a top user bar,
 * a single centred row of large square game icons (the focused one grows and shows its title),
 * and a bottom row of system shortcuts. Purely additive — [HomeScreen] and every other page
 * are untouched, and the choice can be flipped from Settings > Stream.
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

private val SwitchBackground = Color(0xFF1E1E1E)
private val SwitchTileFallback = Color(0xFF333333)
private val SwitchIconCircle = Color(0xFF3A3A3A)
private val SwitchMuted = Color(0xFFBFBFBF)

@Composable
internal fun SwitchStyleHomeScreen(
    state: OpenNowUiState,
    viewModel: OpenNowViewModel,
    tvProfile: Boolean,
) {
    val games = state.games.ifEmpty { state.catalogResult.games }
    val haptics = LocalOpenNowHaptics.current
    val tileSize: Dp = if (tvProfile) 150.dp else 132.dp
    val cellSize: Dp = tileSize + 34.dp
    var focusedTitle by remember(games) {
        mutableStateOf(games.firstOrNull()?.title.orEmpty())
    }
    val launch: (GameInfo) -> Unit = { game ->
        viewModel.selectGame(game)
        haptics?.play(HapticCue.Activate)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SwitchBackground)
            .padding(horizontal = 32.dp, vertical = 20.dp),
    ) {
        SwitchTopBar()
        Spacer(Modifier.height(14.dp))

        if (games.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("正在加载游戏库…", color = SwitchMuted, fontSize = 18.sp)
            }
        } else {
            Text(
                text = focusedTitle,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(games, key = { it.id }) { game ->
                        Box(Modifier.size(cellSize), contentAlignment = Alignment.Center) {
                            SwitchGameTile(
                                game = game,
                                size = tileSize,
                                onClick = { launch(game) },
                                onFocused = { focusedTitle = game.title },
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        SwitchBottomBar()
    }
}

@Composable
private fun SwitchTopBar() {
    val time = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF7F77DD)),
            contentAlignment = Alignment.Center,
        ) {
            Text("玩", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(10.dp))
        Text("玩家", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Text(time, color = SwitchMuted, fontSize = 13.sp)
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(width = 24.dp, height = 12.dp)
                .border(1.5.dp, SwitchMuted, RoundedCornerShape(3.dp))
                .padding(2.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SwitchMuted, RoundedCornerShape(1.dp)),
            )
        }
    }
}

@Composable
private fun SwitchBottomBar() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SwitchSystemIcon(Icons.Filled.Home, "游戏")
        SwitchSystemIcon(Icons.Filled.ShoppingCart, "商店")
        SwitchSystemIcon(Icons.Filled.Search, "搜索")
        SwitchSystemIcon(Icons.Filled.SportsEsports, "手柄")
        SwitchSystemIcon(Icons.Filled.Settings, "设置")
        SwitchSystemIcon(Icons.Filled.PowerSettingsNew, "退出")
    }
}

@Composable
private fun SwitchSystemIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(SwitchIconCircle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = SwitchMuted, fontSize = 11.sp)
    }
}

@Composable
private fun SwitchGameTile(
    game: GameInfo,
    size: Dp,
    onClick: () -> Unit,
    onFocused: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.14f else 1f,
        label = "switch-tile-scale",
    )
    val shape = RoundedCornerShape(20.dp)
    val art = game.imageUrl ?: game.tvCardImageUrl ?: game.screenshotUrl
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(SwitchTileFallback)
            .border(3.dp, if (focused) Color.White else Color.Transparent, shape)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (art != null) {
            AsyncImage(
                model = art,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = game.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}
