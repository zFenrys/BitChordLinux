package com.music.bitchord.desktop

import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE
import java.nio.file.attribute.PosixFilePermissions

/**
 * Keeps BitChord to one running copy per user. The first copy holds a file lock and listens on a
 * local socket; a later launch (AppImage, .desktop file, package) finds the lock taken, asks the
 * running copy to show its window, and quits. Any failure here starts the app normally instead.
 * Set BITCHORD_ALLOW_MULTIPLE=1 to skip the check (useful when testing a build next to an
 * installed copy).
 */
internal object DesktopSingleInstance {
    private val directory: Path by lazy {
        val runtime = System.getenv("XDG_RUNTIME_DIR")?.takeIf { it.isNotBlank() }
        val user = System.getProperty("user.name").orEmpty().ifBlank { "user" }
        if (runtime != null) Path.of(runtime).resolve("bitchord")
        else Path.of(System.getProperty("java.io.tmpdir")).resolve("bitchord-$user")
    }

    // Held for the life of the process: letting these be collected would release the lock.
    private var channel: FileChannel? = null
    private var lock: FileLock? = null
    private var server: ServerSocketChannel? = null

    /** True: this is the first copy, carry on starting. False: the running copy was asked to show itself. */
    fun acquireOrSignal(): Boolean {
        if (System.getenv("BITCHORD_ALLOW_MULTIPLE") == "1") return true
        return runCatching {
            Files.createDirectories(directory)
            runCatching { Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------")) }
            val ch = FileChannel.open(directory.resolve("instance.lock"), CREATE, WRITE)
            val held = try { ch.tryLock() } catch (_: OverlappingFileLockException) { null }
            if (held != null) {
                channel = ch
                lock = held
                listen()
                true
            } else {
                ch.close()
                !signalRunningCopy()
            }
        }.getOrDefault(true)
    }

    private fun listen() {
        val socket = directory.resolve("instance.sock")
        Files.deleteIfExists(socket) // left behind by a copy that crashed
        val s = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
        s.bind(UnixDomainSocketAddress.of(socket))
        server = s
        Thread({
            while (s.isOpen) {
                try {
                    s.accept().use { DesktopWindowVisibility.raise() }
                } catch (_: Exception) {
                    if (!s.isOpen) break
                }
            }
        }, "BitChord-SingleInstance").apply { isDaemon = true; start() }
    }

    /** The first copy may still be starting up, so retry for a few seconds. */
    private fun signalRunningCopy(): Boolean {
        val socket = directory.resolve("instance.sock")
        repeat(20) {
            try {
                SocketChannel.open(StandardProtocolFamily.UNIX).use { c ->
                    c.connect(UnixDomainSocketAddress.of(socket))
                    c.write(ByteBuffer.wrap(byteArrayOf(1)))
                }
                return true
            } catch (_: Exception) {
                Thread.sleep(150)
            }
        }
        return false
    }
}
