package com.rmb.app_deteccion_rostro_ia

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class FaceOverlay(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val paint = Paint()

    var faceRect: Rect? = null

    init {
        paint.color = Color.GREEN
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        faceRect?.let {
            canvas.drawRect(it, paint)
        }
    }
}