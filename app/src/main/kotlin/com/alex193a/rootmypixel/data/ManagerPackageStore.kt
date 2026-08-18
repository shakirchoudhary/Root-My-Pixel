package com.alex193a.rootmypixel.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists the user-selected ReSukiSU manager package name across app restarts.
 * Defaults to the canonical package; user can override to any renamed/spoofed variant.
 */
object ManagerPackageStore {

    const val DEFAULT_PACKAGE = "com.resukisu.resukisu"
    private const val PREFS_NAME = "manager_prefs"
    private const val KEY_PACKAGE = "manager_package"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var selectedPackage: String
        get() = prefs.getString(KEY_PACKAGE, DEFAULT_PACKAGE) ?: DEFAULT_PACKAGE
        set(value) = prefs.edit().putString(KEY_PACKAGE, value.trim()).apply()

    fun reset() {
        selectedPackage = DEFAULT_PACKAGE
    }
}
