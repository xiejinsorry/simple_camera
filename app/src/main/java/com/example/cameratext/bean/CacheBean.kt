package com.example.cameratext.bean

import android.graphics.Bitmap
import java.io.Serializable

data class CacheBean(
    var hash_fuck: String,
    var block_fuck: String,
    var codeUrl: String,
    var time: Long,
    var bm: Bitmap
    )