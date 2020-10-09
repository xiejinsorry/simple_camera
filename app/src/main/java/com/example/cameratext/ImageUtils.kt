package com.example.cameratext

import android.content.Context
import android.graphics.*
import android.media.ExifInterface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.MyApp
import com.example.cameratext.bean.CacheBean
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object ImageUtils {
    private const val TAG = "ImageUtils"
    private val GALLERY_PATH =
        Environment.getExternalStorageDirectory().absolutePath + File.separator + "Camera"
    private val STORE_IMAGES = arrayOf(
        MediaStore.Images.Thumbnails._ID
    )
    private val DATE_FORMAT =
        SimpleDateFormat("yyyyMMdd_HHmmss")

    fun rotateBitmap(
        source: Bitmap,
        degree: Int,
        flipHorizontal: Boolean,
        recycle: Boolean
    ): Bitmap {
        if (degree == 0 && !flipHorizontal) {
            return source
        }
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        if (flipHorizontal) {
            matrix.postScale(-1f, 1f)
        }
        Log.d(TAG, "source width: " + source.width + ", height: " + source.height)
        Log.d(TAG, "rotateBitmap: degree: $degree")
        val rotateBitmap =
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, false)
        Log.d(
            TAG,
            "rotate width: " + rotateBitmap.width + ", height: " + rotateBitmap.height
        )
        if (recycle) {
            source.recycle()
        }
        return rotateBitmap
    }

    fun saveBitmap(bitmap: Bitmap): String {
        val fileName =
            DATE_FORMAT.format(Date(System.currentTimeMillis())) + ".jpg"
        var fileParent = File(GALLERY_PATH)
        if (!fileParent.exists()) {
            fileParent.mkdirs()
        }
        val outFile = File(GALLERY_PATH, fileName)
        Log.d(TAG, "saveImage. filepath: " + outFile.absolutePath)
        var os: FileOutputStream? = null
        return try {
            os = FileOutputStream(outFile)
            val success = bitmap.compress(Bitmap.CompressFormat.JPEG,85, os)
            os.flush()
            Log.d(TAG, "saveBitmap: $success")
            if (success) {
                val paths = arrayListOf<String>()
                paths.add(outFile.absolutePath)
                ScanUtils2.scanPaths(paths)
                return outFile.absolutePath
                //                insertToDB(outFile.getAbsolutePath());
            }
            ""
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        } finally {
            if (os != null) {
                try {
                    os.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun getExifOrientation(bytes: ByteArray?): Int { // YOUR MEDIA PATH AS STRING
        var degree = 0
        var exif: ExifInterface? = null
        try {
            exif = ExifInterface(ByteArrayInputStream(bytes))
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        if (exif != null) {
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
            if (orientation != -1) {
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> degree = 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> degree = 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> degree = 270
                }
            }
        }
        return degree
    }

    // 按照时间顺序降序查询
    val latestThumbBitmap: Bitmap?
        get() {
            var bitmap: Bitmap? = null
            // 按照时间顺序降序查询
            val cursor = MediaStore.Images.Media.query(
                MyApp.instance?.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                STORE_IMAGES,
                null,
                null,
                MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"
            )
            val first = cursor.moveToFirst()
            if (first) {
                val id = cursor.getLong(0)
                bitmap = MediaStore.Images.Thumbnails.getThumbnail(
                    MyApp.instance?.contentResolver,
                    id,
                    MediaStore.Images.Thumbnails.MICRO_KIND,
                    null
                )
                Log.d(TAG, "bitmap width: " + bitmap.width)
                Log.d(TAG, "bitmap height: " + bitmap.height)
            }
            cursor.close()
            return bitmap
        }

    fun Bitmap2Bytes(bm: Bitmap): ByteArray {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val arr = baos.toByteArray()
        if (baos != null) {
            try {
                baos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return arr
    }

    fun changeDate2(time: Long): String {
        val date = Date(time)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm")
        return sdf.format(date)
    }

    private fun calcculate_baseline(
        y: Float,
        mTextPaint: Paint
    ): Float {
        val metric = mTextPaint.fontMetrics
        val textHeight = Math.abs(metric.bottom - metric.top)
        return y + (textHeight / 2 - metric.bottom)
    }

    fun addTextWatermark(
        mBitmap: Bitmap,
        context: Context,
        cacheBean: CacheBean?,
        config: Config?
    ): Bitmap {
        if (config == null || cacheBean == null) return mBitmap
        val code_map = cacheBean.bm
        //获取原始图片与水印图片的宽与高
        val mBitmapWidth = mBitmap.width
        val mBitmapHeight = mBitmap.height
        val codeWidth = code_map.width
        val codeHeight = code_map.height
        //定义底片 大小 将mBitmap填充
        val mNewBitmap =
            Bitmap.createBitmap(mBitmapWidth, mBitmapHeight, Bitmap.Config.ARGB_8888)
        val mCanvas = Canvas(mNewBitmap)
        mCanvas.save()
        //向位图中开始画入MBitmap原始图片
        mCanvas.drawBitmap(mBitmap, 0f, 0f, null)
        val padding = dip2px(context, 15f)
        val pad_size = dip2px(context, 5f)
        val rectHeight = codeHeight + padding * 2
        val mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textSize = dip2px(context, config.textSize.toFloat())
        mTextPaint.textSize = textSize.toFloat()
        val metrics = mTextPaint.fontMetrics
        val start_txt = codeWidth + padding * 3
        val list_line =
            loadLineWidth(
                mTextPaint,
                "Hash:   " + cacheBean.hash_fuck,
                mBitmapWidth - start_txt - padding
            )
        val textHeight =
            textSize * 2 + pad_size + (textSize * list_line.size + pad_size * list_line.size) //文本的高度
        val size_text = textHeight + padding * 2 //文本的高度加上一个padding
        val rect_height = Math.max(rectHeight, size_text) //底部框框的高度
        mCanvas.translate(0f, mBitmapHeight - rect_height.toFloat()) //移动到最底部
        val paintBg =
            Paint(Paint.ANTI_ALIAS_FLAG)
        paintBg.style = Paint.Style.FILL
        paintBg.color = config.bm_bg
        val rectF = RectF(0.0f, 0.0f, mBitmapWidth.toFloat(), rect_height.toFloat())
        mCanvas.drawRect(rectF, paintBg)
        val top_txt = (rect_height - textHeight) / 2
        mCanvas.drawText(
            "Block:  " + cacheBean.block_fuck,
            start_txt.toFloat(),
            top_txt + textSize.toFloat(),
            mTextPaint
        )
        mCanvas.drawText(
            "Time:   " + changeDate2(cacheBean.time * 1000L),
            start_txt.toFloat(),
            top_txt + textSize * 2 + pad_size.toFloat(),
            mTextPaint
        )
        for ((index, str) in list_line.withIndex()) {
            mCanvas.drawText(
                str,
                start_txt.toFloat(),
                top_txt + textSize * (3 + index) + (pad_size * (2 + index)).toFloat(),
                mTextPaint
            )
        }
        mCanvas.drawBitmap(
            code_map,
            padding.toFloat(),
            (rect_height - codeHeight) / 2.toFloat(),
            null
        )
        mCanvas.restore()
        return mNewBitmap
    }

    fun addTextWatermark(
        mBitmap: Bitmap,
        context: Context,
        code_map: Bitmap?,
        vertype: Int,
        oritype: Int
    ): Bitmap {
        if (code_map == null) return mBitmap
        //获取原始图片与水印图片的宽与高
        val mBitmapWidth = mBitmap.width
        val mBitmapHeight = mBitmap.height
        val codeWidth = code_map.width
        val codeHeight = code_map.height
        //定义底片 大小 将mBitmap填充
        val mNewBitmap =
            Bitmap.createBitmap(mBitmapWidth, mBitmapHeight, Bitmap.Config.ARGB_8888)
        val mCanvas = Canvas(mNewBitmap)
        //向位图中开始画入MBitmap原始图片
        mCanvas.drawBitmap(mBitmap, 0f, 0f, null)
        val padding = dip2px(context, 15f)
        var left = padding
        var top = padding
        if (oritype == 1) { //中间
            left = mBitmapWidth / 2 - codeWidth / 2
        } else if (oritype == 2) { //右边
            left = mBitmapWidth - padding - codeWidth
        }
        if (vertype == 1) { //中
            top = mBitmapHeight / 2 - codeHeight / 2
        } else if (vertype == 2) { //下
            top = mBitmapHeight - padding - codeHeight
        }
        mCanvas.drawBitmap(code_map, left.toFloat(), top.toFloat(), null)
        mCanvas.save()
        mCanvas.restore()
        return mNewBitmap
    }

    /**
     * 添加文字
     *
     * @param mBitmap
     * @param context
     * @param text
     * @param vertype
     * @param oritype
     * @param config
     * @return
     */
    fun addTextWatermark(
        mBitmap: Bitmap,
        context: Context,
        text: String,
        vertype: Int,
        oritype: Int,
        config: Config
    ): Bitmap {
        if (TextUtils.isEmpty(text)) return mBitmap
        //获取原始图片与水印图片的宽与高
        val mBitmapWidth = mBitmap.width
        val mBitmapHeight = mBitmap.height
        //定义底片 大小 将mBitmap填充
        val mNewBitmap =
            Bitmap.createBitmap(mBitmapWidth, mBitmapHeight, Bitmap.Config.ARGB_8888)
        val mCanvas = Canvas(mNewBitmap)
        //向位图中开始画入MBitmap原始图片
        mCanvas.drawBitmap(mBitmap, 0f, 0f, null)
        //添加文字
        val mPaint = Paint()
        mPaint.color = config.txtColor
        val size = dip2px(context, config.textSize.toFloat())
        mPaint.textSize = size.toFloat()
        val padding = dip2px(context, 15f)
        var x = padding
        var y = 0
        val textWidth = mPaint.measureText(text).toInt()
        val letterspace = dip2px(context, 5f)
        val list_line =
            loadLineWidth(mPaint, text, mBitmapWidth - padding * 2)
        val lines = list_line.size
        if (oritype == 1) { //中间
            x = if (lines <= 1) {
                Math.max(0, mBitmapWidth / 2 - textWidth / 2)
            } else {
                padding
            }
        } else if (oritype == 2) { //右边
            x = if (lines <= 1) {
                Math.max(0, mBitmapWidth - padding - textWidth)
            } else {
                padding
            }
        }
        y = if (vertype == 0) {
            padding + size
        } else if (vertype == 1) {
            if (lines <= 1) {
                mBitmapHeight / 2 + size / 2
            } else {
                val textHeight = lines * size + (size - 1) * letterspace
                mBitmapHeight / 2 - textHeight / 2 + size / 2
            }
        } else {
            if (lines <= 1) {
                mBitmapHeight - padding
            } else {
                val textHeight = lines * size + (size - 1) * letterspace
                mBitmapHeight - padding - textHeight + size
            }
        }
        for (i in 0 until lines) {
            val line = list_line[i]
            mCanvas.drawText(
                line,
                x.toFloat(),
                y + size * i + (letterspace * i).toFloat(),
                mPaint
            )
        }
        mCanvas.save()
        mCanvas.restore()
        return mNewBitmap
    }

    private fun loadLineWidth(
        textPaint: Paint,
        str: String,
        lineWidth: Int
    ): List<String> {
        var subIndex = textPaint.breakText(str, 0, str.length, true, lineWidth.toFloat(), null)
        var ss = str.substring(subIndex)
        val list: MutableList<String> =
            ArrayList()
        list.add(str.substring(0, subIndex))
        while (ss.length > 0) {
            subIndex = textPaint.breakText(ss, 0, ss.length, true, lineWidth.toFloat(), null)
            list.add(ss.substring(0, subIndex))
            ss = ss.substring(subIndex)
        }
        return list
    }

    fun getRotationBitmap(bitmap: Bitmap, rotation: Int): Bitmap {
        if (rotation != 0) {
            val m = Matrix()
            m.setRotate(rotation.toFloat())
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
        }
        return bitmap
    }

    fun dip2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }
}