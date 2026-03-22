package com.rmb.app_deteccion_rostro_ia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
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
    private lateinit var switchCameraButton: Button

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val bluetoothManager = BluetoothManager()

    private var cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

    private var lastBlinkTime = 0L
    private val blinkCooldown = 800

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        statusText = findViewById(R.id.txtEstado)
        switchCameraButton = findViewById(R.id.btnSwitchCamera)

        overlay = FaceOverlay(this)
        previewView.overlay.add(overlay)

        requestPermissions()

        switchCameraButton.setOnClickListener {

            cameraSelector =
                if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA)
                    CameraSelector.DEFAULT_BACK_CAMERA
                else
                    CameraSelector.DEFAULT_FRONT_CAMERA

            startCamera()
        }

        connectBluetooth()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        }
    }

    private fun connectBluetooth() {
        Thread {
            val connected = bluetoothManager.connect()

            runOnUiThread {
                statusText.text =
                    if (connected) "Conectado ESP32"
                    else "Error Bluetooth"
            }
        }.start()
    }

    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()

            preview.setSurfaceProvider(previewView.surfaceProvider)

            val analyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analyzer.setAnalyzer(cameraExecutor) {
                processImage(it)
            }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                analyzer
            )

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(imageProxy: ImageProxy) {

        val mediaImage = imageProxy.image ?: return

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )

        detector.process(image)
            .addOnSuccessListener { faces ->

                if (faces.isNotEmpty()) {

                    val face = faces.maxByOrNull {
                        it.boundingBox.width() * it.boundingBox.height()
                    }

                    face?.let {

                        val box = it.boundingBox

                        val cx = box.centerX()
                        val cy = box.centerY()

                        val w = imageProxy.width
                        val h = imageProxy.height

                        val normX = cx.toFloat() / w
                        val normY = cy.toFloat() / h

                        // ======================
                        // TUS RANGOS
                        // ======================

                        val X = ((1 - normX) * (135 - 45) + 45).toInt()
                        val Y = ((1 - normY) * (125 - 50) + 50).toInt()

                        bluetoothManager.sendData("$X,$Y\n")

                        // PARPADEO
                        val leftEye = it.leftEyeOpenProbability ?: 1f
                        val rightEye = it.rightEyeOpenProbability ?: 1f

                        val now = System.currentTimeMillis()

                        if (leftEye < 0.4f && rightEye < 0.4f) {
                            if (now - lastBlinkTime > blinkCooldown) {

                                bluetoothManager.sendData("BLINK\n")
                                lastBlinkTime = now
                            }
                        }

                        runOnUiThread {
                            overlay.faceRect = box
                            overlay.invalidate()
                        }
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            1
        )
    }
}