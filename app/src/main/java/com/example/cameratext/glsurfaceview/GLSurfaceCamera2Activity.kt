package com.example.cameratext.glsurfaceview

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.Image
import android.media.ImageReader.OnImageAvailableListener
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.MyApp
import com.example.cameratext.Config
import com.example.cameratext.ImageUtils
import com.example.cameratext.R
import com.example.cameratext.bean.CacheBean
import com.example.cameratext.camera.Camera2Proxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.android.synthetic.main.activity_glsurface_camera2.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*

class GLSurfaceCamera2Activity : AppCompatActivity(),
    View.OnClickListener {
    private var mDialog: ProgressDialog? = null
    private var job: Job? = null
    private var camera_view: Camera2GLSurfaceView? = null
    private var mCameraProxy: Camera2Proxy? = null
    private var mCacheBean: CacheBean? = null
    private lateinit var mConfig: Config
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_glsurface_camera2)
        mConfig = MyApp.instance?.config ?: return
        checkInterfaceData()
        checkPermission()
    }

    private fun checkInterfaceData() {
        newLooper()
    }

    private fun newLooper() {
        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                loadReq()
                delay(6000)
            }
        }
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    fun deepCopy(src: CacheBean?): CacheBean? {
        if (src == null) return null
        val byteOut = ByteArrayOutputStream()
        val out = ObjectOutputStream(byteOut)
        out.writeObject(src)
        val byteIn = ByteArrayInputStream(byteOut.toByteArray())
        val fuck = ObjectInputStream(byteIn)
        return fuck.readObject() as CacheBean
    }

    private fun loadFinish(text: String, code: Int) {
        runOnUiThread {
            if (!TextUtils.isEmpty(text)) {
                var json = JSONObject(text)
                var arr = json.optJSONArray("blocks")
                if (arr != null && arr.length() > 0) {
                    var target = arr.getJSONObject(0)
                    target?.run {
                        var data = "https://explorer.simplechain.com/block/${optString("hash")}"
                        create2DCode(
                            data,
                            mConfig.mCodeSize,
                            mConfig.mCodeSize,
                            mConfig.mCodeColor,
                            optString("hash"),
                            optString("number"),
                            optLong("timestamp")
                        )
                    }
                } else {
                    dismissDialog()
                }
            } else {
                if (code != 0 && code != 200) {
                    Toast.makeText(this@GLSurfaceCamera2Activity, "错误码$code", Toast.LENGTH_SHORT)
                        .show()
                }
                dismissDialog()
            }
        }
    }

    fun create2DCode(
        str: String,
        w: Int,
        h: Int,
        coclor: Int,
        hash: String,
        block: String,
        time: Long
    ) {
        var w = w
        try {
            val hints = Hashtable<EncodeHintType, Any>()
            hints[EncodeHintType.CHARACTER_SET] = "utf-8"
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            hints[EncodeHintType.MARGIN] = 1
            var matrix = QRCodeWriter().encode(str, BarcodeFormat.QR_CODE, w, h)
//            matrix=deleteWhite(matrix)
            w = matrix.width
            val height = matrix.height
            val pixels = IntArray(w * height)
            for (y in 0 until height) {
                for (x in 0 until w) {
                    if (matrix.get(x, y)) {
                        pixels[y * w + x] = coclor
                    } else {
                        pixels[y * w + x] = Color.WHITE
                    }
                }
            }
            var bm = Bitmap.createBitmap(w, height, Bitmap.Config.ARGB_8888)
            bm.setPixels(pixels, 0, w, 0, 0, w, height)
            updateBean(CacheBean(hash, block, str, time, bm))
            dismissDialog()
        } catch (e: Exception) {
            e.printStackTrace()
            dismissDialog()
        }
    }

    fun changeDate2(time: Long): String? {
        val date = Date(time)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm")
        return sdf.format(date)
    }

    private fun showProgress() {
        if (mDialog == null) {
            mDialog = ProgressDialog.show(this, "", "正在加载sipc区块", true, false)
        }
    }

    private fun dismissDialog() {
        runOnUiThread {
            mDialog?.dismiss()
        }
    }

    private fun loadReq() {
        job = GlobalScope.launch(Dispatchers.IO) {
            try {
                var url =
                    URL("https://explorer.simplechain.com/api/block/page?pageNumber=1&pageSize=10")
                var client = url.openConnection() as HttpURLConnection
                client?.run {
                    setRequestProperty("Content-Type", "application/json")
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    connect()
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    var lines = reader.readLine()
                    val sb = StringBuffer("")
                    while (lines != null) {
                        lines = URLDecoder.decode(lines, "utf-8")
                        sb.append(lines)
                        lines = reader.readLine()
                    }
                    if (client.responseCode == 200) {
                        loadFinish(sb.toString(), 200)
                    } else {
                        loadFinish("", client.responseCode)
                    }
                    reader.close()
                    disconnect()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadFinish("", 0)
            }
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        permission
                    ) !== PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(this, permissions, 200)
                    return
                }
            }
        }
        initView()
    }

    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String?>, @NonNull grantResults: IntArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && requestCode == 200) {
            for (i in permissions.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "请在设置中打开摄像头和存储权限", Toast.LENGTH_SHORT).show()
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri =
                        Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivityForResult(intent, 200)
                    return
                }
            }
            initView()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 200) {
                checkPermission()
            }
        }
    }

    private fun initView() {
        camera_view = v_stub.inflate() as Camera2GLSurfaceView?
        fl_top.visibility = View.VISIBLE
        showProgress()
        toolbar_switch_iv.setOnClickListener(this)
        take_picture_iv.setOnClickListener(this)
        picture_iv.setOnClickListener(this)
        picture_iv.setImageBitmap(ImageUtils.latestThumbBitmap)
        mCameraProxy = camera_view!!.cameraProxy
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.toolbar_switch_iv -> {
                mCameraProxy?.run {
                    switchCamera(camera_view!!.width, camera_view!!.height)
                    startPreview()
                }
            }
            R.id.take_picture_iv -> {
                mCameraProxy?.run {
                    setImageAvailableListener(mOnImageAvailableListener)
                    captureStillPicture() // 拍照
                }
            }
            R.id.picture_iv -> {
                val mainIntent = Intent(Intent.ACTION_MAIN, null)
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                var mApps = packageManager.queryIntentActivities(mainIntent, 0)
                val list = ArrayList<ResolveInfo>()
                mApps.filter { it.activityInfo.packageName.contains("gallery") }
                    .forEach {
                        list.add(it)
                    }
                if (list.isEmpty()) {
                    var intent = Intent()
                    intent.action = Intent.ACTION_MAIN
                    intent.addCategory(Intent.CATEGORY_APP_GALLERY)
                    startActivity(intent)
                } else {
                    val intent =
                        packageManager.getLaunchIntentForPackage(list[0].activityInfo.packageName)
                    startActivity(intent)
                }
            }
        }
    }

    private val mOnImageAvailableListener =
        OnImageAvailableListener { reader ->
            doImageWork(reader.acquireNextImage())
        }

    private fun doImageWork(vararg images: Image) {
        GlobalScope.launch(Dispatchers.IO) {
            val buffer = images[0].planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer[bytes]
            var orientation = -1
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                orientation = ImageUtils.getExifOrientation(bytes)
            }
            var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (mCameraProxy!!.isFrontCamera) {
                orientation = 0 - orientation
                bitmap = ImageUtils.rotateBitmap(bitmap, 0, true, true)
            }
            bitmap = ImageUtils.addTextWatermark(
                ImageUtils.getRotationBitmap(bitmap, orientation),
                this@GLSurfaceCamera2Activity, mCacheBean, mConfig
            )
            ImageUtils.saveBitmap(bitmap)
            images[0].close()
            runOnUiThread {
                picture_iv.setImageBitmap(bitmap)
            }
        }
    }

    @Synchronized
    fun updateBean(bean: CacheBean) {
        this.mCacheBean = bean
        bean.run {
            iv_code.setImageBitmap(bm)
            tv_block.text = "Block:   $block_fuck"
            tv_time.text = "Time:    ${changeDate2(time * 1000L)}"
            tv_hash.text = "Hash:   $hash_fuck"
        }
    }

    companion object {
        private const val TAG = "GLSurfaceCamera2Act"
    }
}