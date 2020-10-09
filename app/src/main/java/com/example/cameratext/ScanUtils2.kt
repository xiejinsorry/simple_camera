package com.example.cameratext

import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.MediaScannerConnectionClient
import android.net.Uri
import com.example.MyApp

object ScanUtils2 {
    private var mMediaScannerConnection: MediaScannerConnection? = null
    private var mLstPaths = arrayListOf<String>()
    fun scanPaths(lstPath: List<String>?) {
        lstPath?.run {
            mLstPaths.addAll(this)
        }
        scan()
    }

    private fun scan() {
        mMediaScannerConnection = MediaScannerConnection(MyApp.instance,
            object : MediaScannerConnectionClient {
                override fun onMediaScannerConnected() {
                    try {
                        if (mLstPaths.size > 0) {
                            mMediaScannerConnection!!.scanFile(
                                mLstPaths[0],
                                "image/*"
                            )
                        } else {
                            mMediaScannerConnection!!.disconnect()
                            mMediaScannerConnection = null
                        }
                    } catch (ex: IllegalStateException) {
                        ex.printStackTrace()
                    }
                }

                override fun onScanCompleted(
                    path: String,
                    uri: Uri
                ) {
                    mLstPaths.remove(path)
                    if (mLstPaths.size > 0) {
                        mMediaScannerConnection!!.scanFile(
                            mLstPaths[0],
                            "image/*"
                        )
                    } else {
                        mMediaScannerConnection?.disconnect()
                        mMediaScannerConnection = null
                    }
                }
            })
        mMediaScannerConnection?.connect()
    }
}