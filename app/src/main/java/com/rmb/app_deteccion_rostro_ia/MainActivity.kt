package com.rmb.app_deteccion_rostro_ia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.TextView

import androidx.activity.ComponentActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*

import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlay: FaceOverlay
    private lateinit var statusText: TextView

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private val bluetoothManager = BluetoothManager()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        requestPermissions()
        previewView = PreviewView(this)
        overlay = FaceOverlay(this)

        statusText = TextView(this)
        statusText.text = getString(R.string.StatedCon_1) ?: "@string/StatedCon_1"
        statusText.textSize = 20f

        val layout = FrameLayout(this)
        layout.addView(previewView)
        layout.addView(overlay)
        layout.addView(statusText)

        setContentView(layout)

        connectBluetooth()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            startCamera()

        } else {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                1
            )

        }

    }

    private fun connectBluetooth() {

        Thread {

            val connected = bluetoothManager.connect()

            runOnUiThread {

                if (connected) {

                    statusText.text = getString(R.string.StatedCon_3)

                } else {

                    statusText.text = getString(R.string.StatedCon_2)

                }

            }

        }.start()

    }

    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {

                it.setSurfaceProvider(previewView.surfaceProvider)

            }

            val imageAnalyzer = ImageAnalysis.Builder()

                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)

                .build()

            imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->

                processImage(imageProxy)

            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(

                this,
                cameraSelector,
                preview,
                imageAnalyzer

            )

        }, ContextCompat.getMainExecutor(this))
    }
    private fun requestPermissions() {

        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        ActivityCompat.requestPermissions(
            this,
            permissions,
            1
        )
    }
    private fun processImage(imageProxy: ImageProxy) {

        val mediaImage = imageProxy.image

        if (mediaImage != null) {

            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()

            val detector = FaceDetection.getClient(options)

            detector.process(image)

                .addOnSuccessListener { faces ->

                    if (faces.isNotEmpty()) {

                        val face = faces.maxByOrNull {

                            it.boundingBox.width() * it.boundingBox.height()

                        }

                        face?.let {

                            val box = it.boundingBox

                            val centerX = box.centerX()
                            val centerY = box.centerY()

                            Log.d("FACE", "X:$centerX Y:$centerY")

                            bluetoothManager.sendCoordinates(
                                centerX,
                                centerY
                            )

                            runOnUiThread {

                                overlay.faceRect = box
                                overlay.invalidate()

                            }

                        }

                    } else {

                        runOnUiThread {

                            overlay.faceRect = null
                            overlay.invalidate()

                        }

                    }

                }

                .addOnCompleteListener {

                    imageProxy.close()

                }

        }

    }
}