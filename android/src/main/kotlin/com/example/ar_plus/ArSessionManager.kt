package com.example.ar_plus

import android.app.Activity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.ArSceneView

class ArSessionManager(
    private val activity: Activity,
    private val sceneView: ArSceneView
) {
    var isInitialized = false
        private set

    fun initializeSession(onPlaneDetected: () -> Unit, onError: (String) -> Unit) {
        try {
            val availability = ArCoreApk.getInstance().checkAvailability(activity)
            if (availability.isSupported) {
                val session = Session(activity).apply {
                    configure(Config(this).apply {
                        updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                        planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    })
                }

                sceneView.setupSession(session)
                isInitialized = true

                sceneView.scene.addOnUpdateListener {
                    val frame = sceneView.arFrame ?: return@addOnUpdateListener
                    val updatedPlanes = frame.getUpdatedTrackables(Plane::class.java)
                    for (plane in updatedPlanes) {
                        if (plane.trackingState == TrackingState.TRACKING && plane.subsumedBy == null) {
                            onPlaneDetected()
                            break
                        }
                    }
                }
            } else onError("ARCore not supported.")
        } catch (e: Exception) {
            onError("Error initializing AR session: ${e.localizedMessage}")
        }
    }
}
