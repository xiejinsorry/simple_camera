package com.example

import android.graphics.Color
import com.example.cameratext.Config
import com.example.cameratext.ImageUtils.dip2px
import com.example.cameratext.R
import com.uuzuche.lib_zxing.ZApplication

class MyApp : ZApplication() {
    val config: Config by lazy { Config.Builder()
        .setBgColor(Color.parseColor("#80FFFFFF"))
        .setTextColor(resources.getColor(R.color.color_dark))
        .setBitCodeSize(dip2px(this, 80f))
        .setCodeColor(resources.getColor(R.color.color_dark)).build() }
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        var instance: MyApp? = null
            private set
    }
}