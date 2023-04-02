package com.example.customcamera

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.Manifest
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File


class CameraManager(private val context: Context, cameraView: SurfaceView) :
    SurfaceHolder.Callback {

    private var camera: Camera? = null
    private var CAMERA_PERMISSION_REQUEST_CODE = 100
    var isCapturingMultipleImages = false

    init {
        // Adding the surface holder callback to the surface view
        cameraView.holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        openCamera(holder, "100", Camera.Parameters.FOCUS_MODE_FIXED)
        camera?.setDisplayOrientation(90)
        camera?.setPreviewDisplay(holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Configure the camera parameters
        val parameters = camera?.parameters
        parameters?.setPreviewSize(width, height)
        camera?.parameters = parameters
        camera?.startPreview()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        camera?.stopPreview()
        camera?.release()
        camera = null
    }

    fun takePicture(callback: Camera.PictureCallback) {
        val parameters = camera?.parameters
        camera?.parameters = parameters
        camera?.takePicture(null, null, callback)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun savePhotoToGallery(data: ByteArray, callback: (success: Boolean, uri: Uri?) -> Unit) {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Captured Images/${System.currentTimeMillis()}.jpg"
        )
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_DCIM}/Captured Images"
            )
        }
        try {
            val imageUri = resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            imageUri?.let {
                val outputStream = resolver.openOutputStream(it)
                outputStream?.write(data)
                outputStream?.close()

                // starting a countdown timer after the image is saved
                callback(true, imageUri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            callback(false, null)
        }
    }


    private fun openCamera(
        surfaceHolder: SurfaceHolder,
        iso: String?,
        focusMode: String?,
    ) =
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                Activity(), arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            try {
                camera = Camera.open()
                val parameters = camera!!.parameters

                // Setting the ISO parameter
                parameters?.let {
                    val isoValues = parameters.get("iso-values")
                    if (isoValues != null) {
                        parameters.set("iso", "100")
                    }
                    parameters.set("focus-mode", "1")
                    camera?.parameters = parameters
                }

                camera!!.setPreviewDisplay(surfaceHolder)
                camera!!.startPreview()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    fun setExposureCompensation(value: Int) {
        val parameters = camera?.parameters
        parameters?.exposureCompensation = value
        camera?.parameters = parameters
    }
}
