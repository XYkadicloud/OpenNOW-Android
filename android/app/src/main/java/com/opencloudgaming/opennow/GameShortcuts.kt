package com.opencloudgaming.opennow

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Pins a single game to the device home screen. The shortcut targets [MainActivity] with the same
 * `opennow://launch/<id>` deep link the rest of the app already understands, so tapping it opens the
 * game directly instead of dropping the player back on the catalog.
 */
internal object GameShortcuts {

    enum class Result { Pinned, Unsupported, Failed }

    /** Launcher pinning is an API 26 feature and some TV launchers simply never implement it. */
    fun isSupported(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            runCatching { ShortcutManagerCompat.isRequestPinShortcutSupported(context) }.getOrDefault(false)

    suspend fun pin(context: Context, game: GameInfo): Result {
        if (!isSupported(context)) return Result.Unsupported
        val icon = loadIcon(catalogCardImageUrl(game, tvProfile = false))
        val shortcut = runCatching {
            ShortcutInfoCompat.Builder(context, shortcutId(game))
                .setShortLabel(game.title.take(40))
                .setLongLabel(game.title)
                .setIcon(icon ?: IconCompat.createWithResource(context, R.drawable.opennow_icon))
                .setIntent(launchIntent(context, game))
                .setRank(0)
                .build()
        }.getOrNull() ?: return Result.Failed
        val requested = runCatching {
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        }.getOrDefault(false)
        return if (requested) Result.Pinned else Result.Failed
    }

    private fun shortcutId(game: GameInfo): String =
        "game_" + game.id.replace(Regex("[^A-Za-z0-9_.-]"), "_")

    private fun launchIntent(context: Context, game: GameInfo): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("opennow://launch/${Uri.encode(game.id)}")
            // The id extra is what [OpenNowViewModel.extractExternalLaunchId] reads first, so a game
            // whose id contains characters that do not survive a URI round trip still resolves.
            putExtra("id", game.id)
            putExtra("title", game.title)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

    /**
     * A shortcut without real box art is indistinguishable from every other pinned game, so fetch a
     * small copy. Failure is not fatal: the caller falls back to the app icon.
     */
    private suspend fun loadIcon(url: String?): IconCompat? = withContext(Dispatchers.IO) {
        if (url.isNullOrBlank()) return@withContext null
        val bitmap: Bitmap? = runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6_000
                readTimeout = 6_000
                instanceFollowRedirects = true
            }
            try {
                val bytes = connection.inputStream.use { it.readBytes() }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } finally {
                runCatching { connection.disconnect() }
            }
        }.getOrNull() ?: return@withContext null
        runCatching { IconCompat.createWithBitmap(bitmap) }.getOrNull()
    }
}
