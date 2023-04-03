package com.example.customcamera

import TestImageResponse
import TestImageService
import android.os.*
import android.util.Log
import android.view.SurfaceView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody

class MultipleImageScreen : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var cameraView: SurfaceView
    private lateinit var statusTextView: TextView
    private lateinit var captureButton: Button


    private var currentExposure = 0
    private val exposureValues = (-12..12).toList().toIntArray()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multiple_image_screen)

        statusTextView = findViewById(R.id.test_status_textview_s3)
        statusTextView.text = getString(R.string.capturing_multiple_images)

        cameraView = findViewById(R.id.camera_preview_s3)
        cameraManager = CameraManager(this, cameraView)
        captureButton = findViewById(R.id.btn_capture_s3)

        // Set the initial exposure compensation value
        cameraManager.setExposureCompensation(exposureValues[currentExposure])

        captureButton.setOnClickListener {
            startClick()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun startClick() {
        if (!cameraManager.isCapturingMultipleImages) {
            // Start capturing multiple images
            cameraManager.isCapturingMultipleImages = true

            // Disable the capture button during the capture process
            captureButton.isEnabled = false

            var index = 0
            val handler = Handler()
            val delayMillis = 2000L

            fun captureImage() {
                if (index < exposureValues.size) {
                    cameraManager.setExposureCompensation(exposureValues[index])
                    statusTextView.text = "Capturing Image for Exposure: ${exposureValues[index]}"

                    cameraManager.takePicture { data, camera ->
                        cameraManager.savePhotoToGallery(data, exposureValues[index].toDouble()) { success, uri ->
                            if (success) {
                                // The photo was saved successfully
                                Toast.makeText(
                                    this,
                                    "Image with ${exposureValues[index]} EV Saved to the Gallery",
                                    Toast.LENGTH_SHORT
                                ).show()
                                camera?.startPreview()
                                index++

                                // Delay the capture of the next image
                                handler.postDelayed({ captureImage() }, delayMillis)
                            } else {
                                Toast.makeText(this, "Image Couldn't be saved", Toast.LENGTH_SHORT)
                                    .show()
                                camera?.startPreview()
                            }
                            if (index >= exposureValues.size) {
                                // Finished capturing all images
                                cameraManager.isCapturingMultipleImages = false
                                captureButton.isEnabled = true
                                statusTextView.text = "Sending Image"
                                sendCapturedImagesToBackend()
                            }
                        }
                    }
                }
            }
            captureImage()
        }
    }


    private fun sendCapturedImagesToBackend() {
        val capturedImagesFolder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "Captured Images"
        )

        // Get all files from the Captured Images folder
        val files = capturedImagesFolder.listFiles()
        if (files != null && files.isNotEmpty()) {
            // Get the access token, uid, and client from the signin API
            val retrofit = Retrofit.Builder()
                .baseUrl("http://apistaging.inito.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val authService = retrofit.create(AuthService::class.java)
            val email = "amit_4@test.com"
            val password = "12345678"
            val signInCall = authService.signIn(SignInRequest(email, password))
            signInCall.enqueue(object : Callback<SignInResponse> {
                override fun onResponse(
                    call: Call<SignInResponse>,
                    response: Response<SignInResponse>
                ) {
                    if (response.isSuccessful) {
                        val accessToken = response.headers()["access-token"].toString()
                        val uid = response.headers()["uid"].toString()
                        val client = response.headers()["client"].toString()

                        val apiService = retrofit.create(TestImageService::class.java)

                        // Calculate the mean exposure value of all the images
                        val exposureFiles =
                            files.map { Pair(getIntValue(it.nameWithoutExtension), it) }
                        val sortedFiles = exposureFiles.sortedBy { it.first }
                        val meanExposureValue = sortedFiles.map { it.first }.average()

                        // Finding the file with the exposure value closest to the mean
                        var closestFile: File? = null
                        var closestDifference = Double.MAX_VALUE
                        for (file in sortedFiles) {
                            val difference = abs(file.first - meanExposureValue)
                            if (difference < closestDifference) {
                                closestFile = file.second
                                closestDifference = difference
                            }
                        }

                        if (closestFile != null) {
                            val requestBody = MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart(
                                    "test[images_attributes][][pic]",
                                    closestFile.name,
                                    closestFile.asRequestBody("image/*".toMediaTypeOrNull())
                                )
                                .addFormDataPart("test[batch_qr_code]", "AAO")
                                .addFormDataPart("test[reason]", "NA")
                                .addFormDataPart("test[failure]", "false")
                                .addFormDataPart(
                                    "test[done_date]",
                                    SimpleDateFormat(
                                        "yyyy-MM-dd",
                                        Locale.getDefault()
                                    ).format(Date())
                                )
                                .build()

                            // Sending the image to the backend using the API service
                            apiService.sendTestImagesToBackend(
                                "Bearer $accessToken",
                                requestBody
                            ).enqueue(object : Callback<TestImageResponse> {
                                override fun onResponse(
                                    call: Call<TestImageResponse>,
                                    response: Response<TestImageResponse>
                                ) {
                                    if (response.isSuccessful) {
                                        // The image was sent successfully
                                        statusTextView.text = "TestSuccessful"
                                        Log.d(
                                            "TAG",
                                            "Image with EV ${closestFile.nameWithoutExtension} sent successfully"
                                        )
                                    } else {
                                        // The image sending failed
                                        statusTextView.text = "Test Failed"
                                        Log.d(
                                            "TAG",
                                            "Image with EV ${closestFile.nameWithoutExtension} sending failed"
                                        )
                                    }
                                }

                                override fun onFailure(
                                    call: Call<TestImageResponse>,
                                    t: Throwable
                                ) {
                                    statusTextView.text = "failed to upload"
                                    Log.e(
                                        "TAG",
                                        "Error sending image with EV ${closestFile.nameWithoutExtension}",
                                        t
                                    )
                                }
                            })
                        }
                    } else {
                        // The signin API failed
                        statusTextView.text = "Signin Failed"
                        Log.d("TAG", "Signing API failed with status code ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<SignInResponse>, t: Throwable) {
                    statusTextView.text = "Signin Failed"
                    Log.e("TAG", "Error signing in", t)
                }
            })
        }

    }

    private fun getIntValue(value: String): Int {
        var temp = "";
        for (chr in value) {
            if (chr.isDigit()) {
                temp += chr
            }
        }
        return temp.toIntOrNull() ?: 0
    }
}
