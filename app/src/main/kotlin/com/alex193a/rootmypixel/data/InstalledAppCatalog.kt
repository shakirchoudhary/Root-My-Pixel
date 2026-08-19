package com.alex193a.rootmypixel.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class ManagerCandidate(
    val packageName: String,
    val label: String,
) {
    val searchText: String = "$label $packageName".lowercase()
}

/** Reads launchable apps and picks likely KernelSU managers. */
class InstalledAppCatalog(context: Context) {
    private val appContext = context.applicationContext
    private val pm: PackageManager = appContext.packageManager

    fun isInstalled(packageName: String): Boolean {
        return runCatching {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            true
        }.getOrDefault(false)
    }

    fun labelFor(packageName: String): String {
        return runCatching {
            val info = pm.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0),
            )
            pm.getApplicationLabel(info).toString()
        }.getOrDefault(packageName)
    }

    fun loadLaunchableApps(): List<ManagerCandidate> {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(
            launcher,
            PackageManager.ResolveInfoFlags.of(0),
        )
            .map { ri ->
                ManagerCandidate(
                    packageName = ri.activityInfo.packageName,
                    label = ri.loadLabel(pm).toString(),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun suggested(
        allApps: List<ManagerCandidate>,
        selectedPackage: String,
    ): List<ManagerCandidate> {
        val matched = allApps.filter { app ->
            app.packageName == selectedPackage || isLikelyManager(app)
        }.toMutableList()

        if (matched.none { it.packageName == selectedPackage } && isInstalled(selectedPackage)) {
            matched += ManagerCandidate(selectedPackage, labelFor(selectedPackage))
        }

        return matched.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }

    companion object {
        private val MANAGER_KEYWORDS = listOf("sukisu", "kernelsu", "ksu", "superuser", "magisk")

        private fun isLikelyManager(app: ManagerCandidate): Boolean {
            return MANAGER_KEYWORDS.any { app.searchText.contains(it) }
        }
    }
}
