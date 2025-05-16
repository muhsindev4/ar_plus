package com.example.ar_plus

import android.view.MotionEvent
import com.google.ar.sceneform.ArSceneView

class TapListener(
    private val sceneView: ArSceneView,
    private val onTap: (Float, Float) -> Unit
) {
    fun setup() {
        sceneView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                onTap(event.x, event.y)
            }
            true
        }
    }
}
