package com.example.touchpad

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

class TouchpadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onTouchCallback: ((TouchpadMessage) -> Unit)? = null

    private var lastX = 0f
    private var lastY = 0f
    private var lastTouchTime = 0L
    private val DOUBLE_CLICK_TIME = 300L
    private var isFirstClick = true
    private var firstClickX = 0f
    private var firstClickY = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = x
                lastY = y
                firstClickX = x
                firstClickY = y
                isFirstClick = true
                true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = x - lastX
                val deltaY = y - lastY

                if (hypot(deltaX, deltaY) > 3) {
                    onTouchCallback?.invoke(TouchpadMessage.MouseMove(deltaX, deltaY))
                    lastX = x
                    lastY = y
                    isFirstClick = false
                }
                true
            }

            MotionEvent.ACTION_UP -> {
                val currentTime = System.currentTimeMillis()
                val timeDiff = currentTime - lastTouchTime
                val distance = hypot(x - firstClickX, y - firstClickY)

                if (isFirstClick && distance < 50) {
                    if (timeDiff < DOUBLE_CLICK_TIME) {
                        onTouchCallback?.invoke(TouchpadMessage.MouseClick(2))
                        lastTouchTime = 0
                    } else {
                        onTouchCallback?.invoke(TouchpadMessage.MouseClick(1))
                        lastTouchTime = currentTime
                    }
                }
                true
            }

            else -> false
        }
    }
}