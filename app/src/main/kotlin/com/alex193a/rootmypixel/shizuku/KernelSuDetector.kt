package com.alex193a.rootmypixel.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Detects modern ReSukiSU through its su interface instead of legacy nodes. */
object KernelSuDetector {

    /** Installs in flight app-wide; probing is suppressed while any is running. */
    private val installsInFlight = AtomicInteger(0)

    fun beginInstall() {
        installsInFlight.incrementAndGet()
    }

    fun endInstall() {
        installsInFlight.updateAndGet { if (it > 0) it - 1 else 0 }
    }

    fun isInstallInFlight(): Boolean = installsInFlight.get() > 0

    /**
     * Whether KernelSU is live *right now*.
     * Suppressed while an install is running to avoid the probe destroying
     * the exploit's Shizuku service mid-run.
     * Tries direct app su first, then a Shizuku shell probe as fallback.
     */
    fun isActive(context: Context): Boolean {
        if (isInstallInFlight()) return false
        if (isDirectAppSuActive()) return true
        return isShizukuShellSuActive(context)
    }

    private fun isDirectAppSuActive(): Boolean = runCatching {
        val process = ProcessBuilder(
            "/system/bin/su",
            "-c",
            "echo $PROBE_MARKER; id -u",
        ).redirectErrorStream(true).start()
        if (!process.waitFor(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            process.waitFor(1, TimeUnit.SECONDS)
            return@runCatching false
        }
        isRootShellProbeOutput(process.inputStream.bufferedReader().use { it.readText() })
    }.getOrDefault(false)

    private fun isShizukuShellSuActive(context: Context): Boolean {
        val available = runCatching {
            Shizuku.pingBinder() &&
                !Shizuku.isPreV11() &&
                Shizuku.getUid() == 2000 &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        if (!available) return false

        // IMPORTANT: Use ProbeService (not ExploitService) so unbinding the
        // probe cannot destroy the exploit's Shizuku service process.
        val args = Shizuku.UserServiceArgs(
            ComponentName(context.packageName, ProbeService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("probe_service")
            .version(1)

        val connected = CountDownLatch(1)
        var service: IExploitService? = null
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                service = IExploitService.Stub.asInterface(binder)
                connected.countDown()
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
            }
        }

        var bound = false
        return try {
            Shizuku.bindUserService(args, connection)
            bound = true
            if (!connected.await(SERVICE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) return false
            // Try ksud kernel-side version first (authoritative on all KMIs)
            val versionOutput = service?.exec(
                "$KSUD_PATH debug version 2>/dev/null || true"
            ).orEmpty()
            val kernelVersion = parseKernelVersion(versionOutput)
            if (kernelVersion != null) return kernelVersion > 0
            // Fallback: direct su probe
            val suOutput = service?.exec(ROOT_PROBE_COMMAND).orEmpty()
            isRootShellProbeOutput(suOutput)
        } catch (_: Exception) {
            false
        } finally {
            if (bound) {
                runCatching { Shizuku.unbindUserService(args, connection, true) }
            }
        }
    }

    internal fun isRootShellProbeOutput(output: String): Boolean {
        val lines = output.lineSequence().map(String::trim).filter(String::isNotEmpty).toSet()
        return PROBE_MARKER in lines && "0" in lines
    }

    /**
     * Kernel-side version from `ksud debug version`, or null if ksud did not
     * answer. Callers rely on null ("no answer") differing from 0 ("not live").
     */
    fun parseKernelVersion(output: String): Int? =
        KERNEL_VERSION.find(output)?.groupValues?.get(1)?.toIntOrNull()

    const val KSUD_PATH = "/data/local/tmp/ksud-pixel"

    private const val SERVICE_TIMEOUT_SECONDS = 5L
    private const val PROBE_TIMEOUT_SECONDS = 5L
    private const val PROBE_MARKER = "ROOT_MY_PIXEL_KSU_ACTIVE"
    private const val ROOT_PROBE_COMMAND =
        "/system/bin/su -c 'echo $PROBE_MARKER; id -u' </dev/null 2>/dev/null"
    private val KERNEL_VERSION = Regex("Kernel Version:\\s*(-?[0-9]+)")
}