package com.miragekit.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.miragekit.client.MirageClient
import com.miragekit.protocol.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MirageSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private var client: MirageClient? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        holder.addCallback(this)
    }

    fun attachClient(client: MirageClient) {
        this.client = client
        if (holder.surface.isValid) {
            client.setSurface(holder.surface)
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        client?.setSurface(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Handle resize if needed
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Cleanup
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val client = client ?: return super.onTouchEvent(event)

        val w = width.toDouble()
        val h = height.toDouble()
        if (w == 0.0 || h == 0.0) return false

        val x = event.x.toDouble() / w
        val y = event.y.toDouble() / h
        val loc = listOf(x, y)
        val ts = System.currentTimeMillis() / 1000.0

        val mirageEvent = when (event.action) {
            MotionEvent.ACTION_DOWN -> MirageInputEvent.MouseDown(
                MirageMouseEvent(location = loc, timestamp = ts)
            )
            MotionEvent.ACTION_MOVE -> MirageInputEvent.MouseDragged(
                MirageMouseEvent(location = loc, timestamp = ts)
            )
            MotionEvent.ACTION_UP -> MirageInputEvent.MouseUp(
                MirageMouseEvent(location = loc, timestamp = ts)
            )
            else -> null
        }

        if (mirageEvent != null) {
            scope.launch {
                try {
                    client.sendInput(mirageEvent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return true
        }

        return super.onTouchEvent(event)
    }
}
