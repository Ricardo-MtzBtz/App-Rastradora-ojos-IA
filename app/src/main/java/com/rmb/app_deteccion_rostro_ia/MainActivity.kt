package com.rmb.app_deteccion_rostro_ia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout

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

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        previewView = PreviewView(this)
        overlay = FaceOverlay(this)

        val layout = FrameLayout(this)
        layout.addView(previewView)
        layout.addView(overlay)

        setContentView(layout)

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

                        // Seleccionar la cara más grande (la más cercana)
                        val face = faces.maxByOrNull {
                            it.boundingBox.width() * it.boundingBox.height()
                        }

                        face?.let {

                            val box = it.boundingBox

                            val centerX = box.centerX()
                            val centerY = box.centerY()

                            Log.d("FACE", "X:$centerX Y:$centerY")

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