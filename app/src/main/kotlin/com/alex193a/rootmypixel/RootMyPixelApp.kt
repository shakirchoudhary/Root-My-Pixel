package com.alex193a.rootmypixel

import android.app.Application
import com.alex193a.rootmypixel.data.ManagerPackageStore
import com.alex193a.rootmypixel.di.dataModule
import com.alex193a.rootmypixel.di.domainModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class RootMyPixelApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ManagerPackageStore.init(this)
        startKoin {
            androidContext(this@RootMyPixelApp)
            modules(domainModule, dataModule)
        }
    }
}

