package com.example.noteapp_lttbdd

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var path = Path()
    private val paint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private var mBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null
    private val bitmapPaint = Paint(Paint.DITHER_FLAG)
    
    private var initialBitmap: Bitmap? = null

    fun loadBitmap(bitmap: Bitmap) {
        initialBitmap = bitmap
        path.reset()
        if (width > 0 && height > 0) {
            drawInitialBitmap()
        }
        invalidate()
    }
    
    fun setBlankCanvas() {
        initialBitmap = null
        path.reset()
        mCanvas?.drawColor(Color.WHITE)
        invalidate()
    }

    private fun drawInitialBitmap() {
        mCanvas?.drawColor(Color.WHITE)
        initialBitmap?.let {
            val matrix = Matrix()
            val scale = Math.min(width.toFloat() / it.width, height.toFloat() / it.height)
            val dx = (width - it.width * scale) / 2f
            val dy = (height - it.height * scale) / 2f
            matrix.postScale(scale, scale)
            matrix.postTranslate(dx, dy)
            mCanvas?.drawBitmap(it, matrix, bitmapPaint)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            mCanvas = Canvas(mBitmap!!)
            if (initialBitmap != null) {
                drawInitialBitmap()
            } else {
                mCanvas?.drawColor(Color.WHITE)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mBitmap?.let { canvas.drawBitmap(it, 0f, 0f, bitmapPaint) }
        canvas.drawPath(path, paint)
    }

    private var mX = 0f
    private var mY = 0f
    private val TOUCH_TOLERANCE = 4f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.reset()
                path.moveTo(x, y)
                mX = x
                mY = y
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = Math.abs(x - mX)
                val dy = Math.abs(y - mY)
                if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                    path.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
                    mX = x
                    mY = y
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                path.lineTo(mX, mY)
                mCanvas?.drawPath(path, paint)
                path.reset()
                invalidate()
            }
        }
        return true
    }

    fun clear() {
        path.reset()
        if (initialBitmap != null) {
            drawInitialBitmap()
        } else {
            mCanvas?.drawColor(Color.WHITE)
        }
        invalidate()
    }

    fun getBitmap(): Bitmap? {
        return mBitmap
    }
}
