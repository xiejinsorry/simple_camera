package com.example.cameratext

import android.graphics.Color
import com.example.MyApp

class Config {
    private constructor()

    var mCodeColor = 0
    var mCodeSize = 0

    var txtColor = 0
    var bm_bg = 0

    var textSize = 0


    class Builder {
        var code_coclor = Color.parseColor("#333333")
        var code_size = ImageUtils.dip2px(MyApp.instance!!, 80f)

        var txtColor = Color.parseColor("#333333")
        var bm_bg = Color.parseColor("#FFFFFF")

        var texstSize = 14

        constructor()

        fun setCodeColor(color: Int): Builder {
            this.code_coclor = color
            return this
        }


        fun setBitCodeSize(size: Int): Builder {
            this.code_size = size
            return this
        }


        fun setTextColor(color: Int): Builder {
            this.txtColor = color
            return this
        }

        fun setBgColor(color: Int): Builder {
            this.bm_bg = color
            return this
        }

        fun build(): Config {
            var config = Config()
            config.mCodeColor = code_coclor
            config.mCodeSize = code_size
            config.bm_bg = bm_bg
            config.txtColor = txtColor
            config.textSize = texstSize
            return config
        }
    }

}