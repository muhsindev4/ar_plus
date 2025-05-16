package com.example.ar_plus

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import java.util.UUID

class ARView(
    context: Context,
    private val activity: Activity,
    private val lifecycle: Lifecycle,
    messenger: BinaryMessenger
) : PlatformView, DefaultLifecycleObserver {

    private val TAG = "ARView"
    private val sceneView = ArSceneView(context)
    private val rootLayout = FrameLayout(context)
    private val instructionTextView: TextView

    private var sessionInitialized = false
    private lateinit var methodChannel: MethodChannel
    private lateinit var transformationSystem: TransformationSystem
    private val shapeMap = mutableMapOf<String, TransformableNode>()

    companion object {
        private const val CAMERA_PERMISSION_CODE = 1001
    }

    init {
        Log.d(TAG, "Initializing ARView")

        methodChannel = MethodChannel(messenger, "ar_plus_view_channel")
        lifecycle.addObserver(this)

        instructionTextView = TextView(context).apply {
            text = "Move your phone slowly to detect surfaces"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(20, 20, 20, 20)
            gravity = Gravity.CENTER
            setBackgroundColor(0x80000000.toInt()) // semi-transparent black
        }

        rootLayout.addView(sceneView)

        if (hasCameraPermission()) {
            rootLayout.addView(instructionTextView)
            initializeARSession()
        } else {
            requestCameraPermission()
        }

        transformationSystem = TransformationSystem(
            context.resources.displayMetrics,
            FootprintSelectionVisualizer()
        )

        methodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "makeShape" -> {
                    val shape = call.argument<String>("shape") ?: "cylinder"
                    val radius = call.argument<Double>("radius")?.toFloat() ?: 0.01f
                    val height = call.argument<Double>("height")?.toFloat() ?: 0.01f
                    val width = call.argument<Double>("width")?.toFloat() ?: 0.01f
                    val depth = call.argument<Double>("depth")?.toFloat() ?: 0.01f
                    val positionX = call.argument<Double>("positionX")?.toFloat() ?: 0.0f
                    val positionY = call.argument<Double>("positionY")?.toFloat() ?: 0.0f
                    val colorValue = (call.argument<Number>("color") ?: Color.YELLOW).toInt()

                    makeShape(shape, positionX, positionY, radius, height, width, depth, colorValue, result)
                }

                "moveShape" -> {
                    val shapeId = call.argument<String>("id")
                    val newX = call.argument<Double>("x")?.toFloat()
                    val newY = call.argument<Double>("y")?.toFloat()
                    val newZ = call.argument<Double>("z")?.toFloat()

                    if (shapeId != null && newX != null && newY != null && newZ != null) {
                        val node = shapeMap[shapeId]
                        if (node != null) {
                            node.localPosition = Vector3(newX, newY, newZ)
                            result.success(true)
                        } else {
                            result.error("NOT_FOUND", "Shape not found for ID: $shapeId", null)
                        }
                    } else {
                        result.error("INVALID_ARGS", "Missing ID or position", null)
                    }
                }

                "removeShape" -> {
                    val shapeId = call.argument<String>("id")
                    val node = shapeId?.let { shapeMap.remove(it) }
                    if (node != null) {
                        node.setParent(null)
                        result.success(true)
                    } else {
                        result.error("NOT_FOUND", "No shape with ID $shapeId", null)
                    }
                }

                "performHitTest" -> {
                    val x = (call.argument<Double>("x") ?: 0.0).toFloat()
                    val y = (call.argument<Double>("y") ?: 0.0).toFloat()
                    performHitTest(x, y, result)
                }

                else -> result.notImplemented()
            }
        }


    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(activity, android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    private fun initializeARSession() {
        try {
            val availability = ArCoreApk.getInstance().checkAvailability(activity)
            if (availability.isSupported) {
                val session = Session(activity)
                val config = Config(session).apply {
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                }
                session.configure(config)
                sceneView.setupSession(session)

                sessionInitialized = true

                sceneView.scene.addOnUpdateListener {
                    val frame = sceneView.arFrame ?: return@addOnUpdateListener
                    val updatedPlanes = frame.getUpdatedTrackables(Plane::class.java)

                    for (plane in updatedPlanes) {
                        if (plane.trackingState == TrackingState.TRACKING && plane.subsumedBy == null) {
                            instructionTextView.visibility = View.GONE
                            methodChannel.invokeMethod("onDetectedPlanes", null)
                            break
                        }
                    }
                }

                setupTapListener()
                methodChannel.invokeMethod("onCreated", null)
            } else {
                Log.e(TAG, "ARCore not supported.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AR session", e)
        }
    }

    private fun setupTapListener() {
        sceneView.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                val x = motionEvent.x
                val y = motionEvent.y
                methodChannel.invokeMethod("onTap", mapOf("x" to x, "y" to y))
            }
            true
        }
    }


    fun performHitTest(x: Float, y: Float, result: MethodChannel.Result) {
        val frame = sceneView.arFrame ?: return result.error("FRAME_NULL", "AR frame is null", null)
        if (frame.camera.trackingState != TrackingState.TRACKING)
            return result.error("TRACKING_INVALID", "Camera not tracking", null)

        val hitResults = frame.hitTest(x, y)

        for (hit in hitResults) {
            val trackable = hit.trackable
            if ((trackable is Plane && trackable.isPoseInPolygon(hit.hitPose))
                || (trackable is Point && trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
            ) {
                val pose = hit.hitPose
                val x = pose.tx()
                val y = pose.ty()
                val z = pose.tz()

                val resultMap = mapOf(
                    "x" to x,
                    "y" to y,
                    "z" to z
                )
                result.success(resultMap)
                return
            }
        }

        result.error("NO_HIT", "No suitable surface found", null)
    }


    private fun makeShape(
        shape: String,
        x: Float,
        y: Float,
        radius: Float,
        height: Float,
        width: Float,
        depth: Float,
        colorInt: Int,
        result: MethodChannel.Result
    ) {
        val frame = sceneView.arFrame ?: return result.error("FRAME_NULL", "AR frame is null", null)
        if (frame.camera.trackingState != TrackingState.TRACKING)
            return result.error("TRACKING_INVALID", "Camera not tracking", null)

        val hitResults = frame.hitTest(x, y)
        for (hit in hitResults) {
            val trackable = hit.trackable
            if ((trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) ||
                (trackable is Point && trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
            ) {
                placeShape(hit, shape, radius, height, width, depth, colorInt, result)
                return
            }
        }

        result.error("NO_HIT", "No suitable surface found", null)
    }


    private fun placeShape(
        hit: HitResult,
        shape: String,
        radius: Float,
        height: Float,
        width: Float,
        depth: Float,
        colorInt: Int,
        result: MethodChannel.Result
    ) {
        val anchor = hit.createAnchor()
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(sceneView.scene)

        MaterialFactory.makeOpaqueWithColor(activity, com.google.ar.sceneform.rendering.Color(colorInt))
            .thenAccept { material ->
                val renderable = when (shape.lowercase()) {
                    "cylinder" -> ShapeFactory.makeCylinder(radius, height, Vector3.zero(), material)
                    "sphere" -> ShapeFactory.makeSphere(radius, Vector3.zero(), material)
                    "cube" -> ShapeFactory.makeCube(Vector3(width, height, depth), Vector3.zero(), material)
                    else -> {
                        Log.e(TAG, "Unknown shape: $shape")
                        result.error("UNKNOWN_SHAPE", "Unknown shape: $shape", null)
                        return@thenAccept
                    }
                }

                val shapeId = UUID.randomUUID().toString()
                TransformableNode(transformationSystem).apply {
                    setParent(anchorNode)
                    this.renderable = renderable
                    select()
                    shapeMap[shapeId] = this
                    result.success(shapeId)  // Return shape ID to Flutter
                }
            }.exceptionally { throwable ->
                Log.e(TAG, "Failed to create shape renderable", throwable)
                result.error("RENDERABLE_ERROR", throwable.localizedMessage, null)
                null
            }
    }



    override fun getView(): View = rootLayout

    override fun dispose() {
        lifecycle.removeObserver(this)
        sceneView.destroy()
    }

    override fun onResume(owner: LifecycleOwner) {
        if (hasCameraPermission()) {
            if (!sessionInitialized) initializeARSession()
            sceneView.resume()
        } else {
            requestCameraPermission()
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        sceneView.pause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        sceneView.destroy()
    }
}
