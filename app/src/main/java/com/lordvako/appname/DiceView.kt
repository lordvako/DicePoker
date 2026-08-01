package lordvako.appname

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class DiceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var diceValue: Int = 1
        set(v) {
            field = v.coerceIn(1, 6)
            invalidate()
        }

    var isDiceSelected: Boolean = false
        set(v) {
            field = v
            invalidate()
        }

    var isDiceLocked: Boolean = false
        set(v) {
            field = v
            invalidate()
        }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        dotPaint.color = Color.parseColor("#2D2D2D")
        shadowPaint.color = Color.parseColor("#40000000")
        borderPaint.style = Paint.Style.STROKE
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val corner = w * 0.18f
        val shadowOff = w * 0.06f

        // Shadow
        canvas.drawRoundRect(shadowOff, shadowOff, w + shadowOff * 0.5f, h + shadowOff * 0.5f, corner, corner, shadowPaint)

        // Background with 3D gradient effect
        val bgColor = when {
            isDiceSelected -> Color.parseColor("#E8F5E9")
            isDiceLocked -> Color.parseColor("#D0D0D0")
            else -> Color.parseColor("#FAFAFA")
        }
        bgPaint.color = bgColor
        canvas.drawRoundRect(0f, 0f, w, h, corner, corner, bgPaint)

        // Inner highlight (3D effect)
        bgPaint.color = Color.parseColor("#30FFFFFF")
        canvas.drawRoundRect(w * 0.1f, h * 0.05f, w * 0.9f, h * 0.45f, corner * 0.6f, corner * 0.6f, bgPaint)

        // Border
        if (isDiceSelected) {
            borderPaint.color = Color.parseColor("#FFD700")
            borderPaint.strokeWidth = w * 0.07f
            canvas.drawRoundRect(0f, 0f, w, h, corner, corner, borderPaint)
        } else {
            borderPaint.color = Color.parseColor("#999999")
            borderPaint.strokeWidth = w * 0.025f
            canvas.drawRoundRect(0f, 0f, w, h, corner, corner, borderPaint)
        }

        // Dots
        drawDots(canvas, diceValue, w, h)
    }

    private fun drawDots(canvas: Canvas, value: Int, w: Float, h: Float) {
        val dotR = w * 0.09f
        val positions = getDotPositions(value, w, h)
        for ((x, y) in positions) {
            // Dot shadow
            dotPaint.color = Color.parseColor("#20000000")
            canvas.drawCircle(x + 1.5f, y + 1.5f, dotR, dotPaint)
            // Dot
            dotPaint.color = Color.parseColor("#2D2D2D")
            canvas.drawCircle(x, y, dotR, dotPaint)
        }
    }

    private fun getDotPositions(value: Int, w: Float, h: Float): List<Pair<Float, Float>> {
        val cx = w / 2f
        val cy = h / 2f
        val off = w * 0.22f
        val off2 = w * 0.32f
        val list = mutableListOf<Pair<Float, Float>>()

        when (value) {
            1 -> list.add(cx to cy)
            2 -> {
                list.add(cx - off to cy - off)
                list.add(cx + off to cy + off)
            }
            3 -> {
                list.add(cx - off to cy - off)
                list.add(cx to cy)
                list.add(cx + off to cy + off)
            }
            4 -> {
                list.add(cx - off to cy - off)
                list.add(cx + off to cy - off)
                list.add(cx - off to cy + off)
                list.add(cx + off to cy + off)
            }
            5 -> {
                list.add(cx - off to cy - off)
                list.add(cx + off to cy - off)
                list.add(cx to cy)
                list.add(cx - off to cy + off)
                list.add(cx + off to cy + off)
            }
            6 -> {
                list.add(cx - off2 to cy - off)
                list.add(cx - off2 to cy)
                list.add(cx - off2 to cy + off)
                list.add(cx + off2 to cy - off)
                list.add(cx + off2 to cy)
                list.add(cx + off2 to cy + off)
            }
        }
        return list
    }

    fun animateRoll() {
        ObjectAnimator.ofFloat(this, "rotationX", 0f, 360f * 2).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(this, "rotationY", 0f, 360f).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(this, "translationY", 0f, -60f, 0f).apply {
            duration = 400
            start()
        }
    }

    fun animateSelect() {
        ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.15f, 1f).apply {
            duration = 200
            start()
        }
        ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.15f, 1f).apply {
            duration = 200
            start()
        }
    }
}
