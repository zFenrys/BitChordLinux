package com.music.bitchord.desktop

import java.awt.KeyboardFocusManager
import java.io.File
import java.net.URL
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import com.music.bitchord.data.model.Song
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant

/** The desktop's notification service, as every Linux shell implements it. */
@DBusInterfaceName("org.freedesktop.Notifications")
interface FreedesktopNotifications : DBusInterface {
    fun Notify(
        appName: String,
        replacesId: UInt32,
        appIcon: String,
        summary: String,
        body: String,
        actions: List<String>,
        hints: Map<String, Variant<*>>,
        expireTimeout: Int,
    ): UInt32
}

/**
 * Sends a "now playing" notification. Each one replaces the previous, so a long playlist leaves
 * a single card, not a stack. Any failure is logged and ignored: playback never depends on this.
 */
internal object DesktopTrackNotifier {
    private val worker: ExecutorService by lazy {
        Executors.newSingleThreadExecutor { r -> Thread(r, "BitChord-Notify").apply { isDaemon = true } }
    }
    private var connection: DBusConnection? = null
    private var service: FreedesktopNotifications? = null
    private var lastId = UInt32(0)
    private var lastSentAt: Long? = null

    private val onLinux: Boolean =
        System.getProperty("os.name").orEmpty().contains("Linux", ignoreCase = true)

    /** True while the window is closed to the tray or none of the app's windows has the focus. */
    fun windowNeedsNotice(): Boolean =
        !DesktopWindowVisibility.visible.value ||
            KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow == null

    /** Called when the selected song changes. Waits for playback to really start, then notifies. */
    suspend fun onTrackChanged(song: Song, isPlaying: () -> Boolean) {
        if (!DesktopNotificationSettings.enabled.value) return
        DesktopTrackLog.log("notify: track changed to '${song.title}', waiting for playback")
        val started = withTimeoutOrNull(10_000L) {
            while (!isPlaying()) delay(250L)
            true
        } ?: false
        if (!started) {
            DesktopTrackLog.log("notify: '${song.title}' did not start playing within 10s, skipped")
            return
        }
        if (!DesktopNotificationSettings.enabled.value) return
        if (DesktopNotificationSettings.onlyWhenUnfocused.value && !windowNeedsNotice()) {
            DesktopTrackLog.log("notify: skipped, the window is in front")
            return
        }
        val body = listOfNotNull(
            song.artist.takeIf { DesktopNotificationSettings.showArtist.value && it.isNotBlank() },
            song.albumName?.takeIf { DesktopNotificationSettings.showAlbum.value && it.isNotBlank() },
        ).joinToString(" \u2014 ")
        val cover = if (DesktopNotificationSettings.showCover.value) {
            withContext(Dispatchers.IO) { coverFile(song) }
        } else {
            null
        }
        show(song.title, body, cover)
    }

    private val coverDir: File by lazy {
        File(DesktopMediaCache.directory.toFile(), "notify-covers").apply { mkdirs() }
    }

    /** The cover as a small PNG the notification service can always read, or null. */
    private fun coverFile(song: Song): String? = runCatching {
        val url = song.thumbnailUrl?.takeIf { it.startsWith("https://") } ?: return@runCatching null
        val target = File(coverDir, song.videoId.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".png")
        if (target.isFile && target.length() > 0) return@runCatching target.absolutePath
        val connection = URL(url).openConnection().apply { connectTimeout = 4000; readTimeout = 4000 }
        val image = connection.getInputStream().use { ImageIO.read(it) } ?: return@runCatching null
        ImageIO.write(image, "png", target)
        coverDir.listFiles()?.sortedBy { it.lastModified() }?.dropLast(40)?.forEach { it.delete() }
        target.absolutePath
    }.onFailure { DesktopTrackLog.log("notify: cover failed: ${it.message}") }.getOrNull()

    fun show(summary: String, body: String, coverPath: String? = null) {
        if (!onLinux || summary.isBlank()) return
        worker.execute {
            runCatching {
                val remote = service ?: run {
                    val bus = DBusConnectionBuilder.forSessionBus().withShared(false).build()
                    connection = bus
                    bus.getRemoteObject(
                        "org.freedesktop.Notifications",
                        "/org/freedesktop/Notifications",
                        FreedesktopNotifications::class.java,
                    ).also { service = it }
                }
                val now = System.nanoTime()
                val replaces = if (lastSentAt?.let { now - it < 6_000_000_000L } == true) lastId else UInt32(0)
                lastId = remote.Notify(
                    "BitChord", replaces, "", summary, body, emptyList(),
                    buildMap<String, Variant<*>> {
                        put("desktop-entry", Variant("bitchord"))
                        if (coverPath != null) put("image-path", Variant("file://" + coverPath))
                    },
                    -1,
                )
                lastSentAt = now
                DesktopTrackLog.log("notify: sent id=$lastId replaced=$replaces")
            }.onFailure { failure ->
                DesktopTrackLog.log("notification failed: ${failure.message}")
                runCatching { connection?.close() }
                connection = null
                service = null
            }
        }
    }
}
