package com.example.customcamera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.*
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import java.util.*


class SingleImageScreen : AppCompatActivity() {

    private lateinit var cameraView: SurfaceView
    private lateinit var cameraManager: CameraManager
    private lateinit var captureButton: Button
    private lateinit var timerTextview: TextView
    private lateinit var statusTextView: TextView


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_image_screen)
        getPermissions()

        cameraView = findViewById(R.id.camera_preview)
        cameraManager = CameraManager(this, cameraView)
        captureButton = findViewById(R.id.btn_capture1)

        captureButton.setOnClickListener {
            captureButton.isEnabled = false
            cameraManager.takePicture { data, camera ->
                cameraManager.savePhotoToGallery(data,0.0) { success, uri ->
                    if (success) {
                        // The photo was saved successfully
                        Toast.makeText(this, "Image Saved to the Gallery", Toast.LENGTH_SHORT)
                            .show()
                        camera?.startPreview()
                        counterStart()
                    } else {
                        // There was an error saving the photo
                        Toast.makeText(this, "Image Couldn't be saved", Toast.LENGTH_SHORT).show()
                        camera?.startPreview()
                    }
                }
            }
        }

    }

    private fun counterStart() {
        timerTextview = findViewById(R.id.timer_textview)

        statusTextView = findViewById(R.id.test_status_textview)
        statusTextView.text = (getString(R.string.timer_started_text))

        object : CountDownTimer(300000, 5000) { // 300 seconds, 5 second interval
            override fun onTick(millisUntilFinished: Long) {
                timerTextview.text = (millisUntilFinished / 1000).toString()
                timerTextview.visibility = View.VISIBLE
            }

            override fun onFinish() {
                startActivity(Intent(this@SingleImageScreen, MultipleImageScreen::class.java))
                finish()
                timerTextview.visibility = View.GONE
            }
        }.start()

        val circularProgressBar: CircularProgressBar = findViewById(R.id.timerProgressBar)
        circularProgressBar.apply {
            circularProgressBar.visibility = View.VISIBLE
            setProgressWithAnimation(100f, 300000)

            progressMax = 100f
            progressBarColor = Color.GREEN
            backgroundProgressBarColor = Color.GRAY
            progressBarWidth = 7f
            backgroundProgressBarWidth = 3f
            roundBorder = true
            startAngle = 0f
            progressDirection = CircularProgressBar.ProgressDirection.TO_RIGHT
        }
    }

    private fun getPermissions() {
        val permissionList = mutableListOf<String>()

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            permissionList.add(Manifest.permission.CAMERA)
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permissionList.size > 0) {
            requestPermissions(permissionList.toTypedArray(), 101)
        }else{
            cameraManager.openCamera("100","1")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        grantResults.forEach {
            if (it != PackageManager.PERMISSION_GRANTED) {
                getPermissions()
            }
        }
    }
}