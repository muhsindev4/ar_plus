package com.example.ar_plus

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.ViewTreeObserver
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
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer
import com.google.ar.sceneform.ux.TransformableNode
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.platform.PlatformView
import com.google.ar.sceneform.ux.TransformationSystem
import java.io.File
import java.io.FileOutputStream

class Measurement(
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

    private var focusNode: AnchorNode? = null
    private var focusRenderable: ModelRenderable? = null

    private val placedAnchors = mutableListOf<AnchorNode>()



    companion object {
        private const val CAMERA_PERMISSION_CODE = 1001
    }

    init {
        Log.d(TAG, "Initializing ARView")

        methodChannel = MethodChannel(messenger, "ar_plus_view_channel")

        lifecycle.addObserver(this)

        instructionTextView = TextView(context).apply {
            text = "Move your phone slowly to detect surfaces"
            setTextColor(android.graphics.Color.WHITE)
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
                "shoot" -> shootPoint()
                "clearAllPoints" -> clearAllPoints()
                "hideVisualElements" -> hideVisualElements()
                "restoreVisualElements" -> restoreVisualElements()
                "takeScreenshot" -> {
                    takeScreenshot(result)
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
                methodChannel.invokeMethod("onCreated", null)



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
                    updateFocusVisual()
                }

                setupTapListener()
                createFocusRenderable()

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

                val frame = sceneView.arFrame
                if (frame != null && frame.camera.trackingState == TrackingState.TRACKING) {
                    val hitResults = frame.hitTest(x, y)
                    for (hit in hitResults) {
                        val trackable = hit.trackable
                        if ((trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) ||
                            (trackable is Point && trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
                        ) {
                            methodChannel.invokeMethod("onTap", mapOf("x" to x, "y" to y))
                            break
                        }
                    }
                }


            }
            true
        }
    }

    private fun updateFocusVisual() {
        val frame = sceneView.arFrame ?: return

        if (frame.camera.trackingState != TrackingState.TRACKING) return

        val hitResults = frame.hitTest(sceneView.width / 2f, sceneView.height / 2f)
        for (hit in hitResults) {
            val trackable = hit.trackable
            if ((trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) ||
                (trackable is Point && trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
            ) {

                if (focusNode == null || focusNode?.parent == null) {
                    val anchor = hit.createAnchor()
                    val anchorNode = AnchorNode(anchor)
                    anchorNode.setParent(sceneView.scene)
                    focusNode?.setParent(anchorNode)
                } else {
                    focusNode?.parent?.let {
                        if (it is AnchorNode) {
                            it.anchor?.detach()
                            it.anchor = hit.createAnchor()
                        }
                    }
                }

                break
            }
        }
    }
    private fun createFocusRenderable() {
        MaterialFactory.makeOpaqueWithColor(activity, Color(android.graphics.Color.WHITE))
            .thenAccept { material ->
                // 1. Center circle (flat cylinder)
                val centerRenderable = ShapeFactory.makeCylinder(
                    0.02f,
                    0.0003f,
                    Vector3.zero(),
                    material
                )

                // 2. Create the visual parent node
                val visualNode = Node()

                val centerNode = Node().apply {
                    renderable = centerRenderable
                    localPosition = Vector3.zero()
                }
                visualNode.addChild(centerNode)

                // 3. Crosshair arms
                val armLength = 0.04f
                val armThickness = 0.003f
                val armHeight = 0.001f

                val directions = listOf(
                    Vector3(armLength / 2 + 0.02f, 0f, 0f),  // Right
                    Vector3(-armLength / 2 - 0.02f, 0f, 0f), // Left
                    Vector3(0f, 0f, armLength / 2 + 0.02f),  // Front
                    Vector3(0f, 0f, -armLength / 2 - 0.02f)  // Back
                )

                val sizes = listOf(
                    Vector3(armLength, armHeight, armThickness), // Horizontal arms
                    Vector3(armLength, armHeight, armThickness),
                    Vector3(armThickness, armHeight, armLength), // Vertical arms
                    Vector3(armThickness, armHeight, armLength)
                )

                for (i in 0 until 4) {
                    val armRenderable = ShapeFactory.makeCube(sizes[i], Vector3.zero(), material)
                    val armNode = Node().apply {
                        renderable = armRenderable
                        localPosition = directions[i]
                    }
                    visualNode.addChild(armNode)
                }

                // 🟡 Now we attach this entire visual to an AnchorNode later
                focusRenderable = null // no longer used
                focusNode = AnchorNode().apply {
                    setParent(null) // not attached yet
                    addChild(visualNode)
                }
            }
    }



    private fun shootPoint() {
        val frame = sceneView.arFrame ?: return
        if (frame.camera.trackingState != TrackingState.TRACKING) return

        val hits = frame.hitTest(sceneView.width / 2f, sceneView.height / 2f)
        val hit = hits.firstOrNull {
            (it.trackable is Plane && (it.trackable as Plane).isPoseInPolygon(it.hitPose))
        } ?: return

        val anchor = hit.createAnchor()
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(sceneView.scene)

        // Red dot
        MaterialFactory.makeOpaqueWithColor(activity, Color(android.graphics.Color.RED))
            .thenAccept { material ->
                val dot = ShapeFactory.makeCylinder(
                    0.01f,
                    0.0003f,
                    Vector3.zero(), material)
                val node = TransformableNode(transformationSystem).apply {
                    setParent(anchorNode)
                    renderable = dot
                }

                placedAnchors.add(anchorNode)

                // If 2 or more points, draw line and show distance
                if (placedAnchors.size >= 2) {
                    val a = placedAnchors[placedAnchors.size - 2]
                    val b = placedAnchors[placedAnchors.size - 1]
                    drawLineBetween(a, b)
                }
            }
    }

    private fun clearAllPoints() {
        placedAnchors.forEach { it.setParent(null) }
        placedAnchors.clear()

        sceneView.scene.children
            .filterIsInstance<TransformableNode>()
            .forEach {
                it.setParent(null) // Remove all distance labels and lines
            }
    }

    private  fun hideVisualElements(){

        // Hide plane visualizations
        sceneView.planeRenderer.isVisible = false

        // Hide focus node if you have one
        focusNode?.isEnabled = false
        focusNode?.renderable = null
    }


    private  fun restoreVisualElements(){

        // Show plane visualizations
        sceneView.planeRenderer.isVisible = true

        // Restore focus node
        focusNode?.isEnabled = true
        focusNode?.renderable = focusRenderable // make sure this is stored earlier
    }

    private fun takeScreenshot(result: MethodChannel.Result) {
        try {
            val bitmap = Bitmap.createBitmap(sceneView.width, sceneView.height, Bitmap.Config.ARGB_8888)
            val handler = Handler(Looper.getMainLooper())

            PixelCopy.request(sceneView, bitmap, { copyResult ->

                if (copyResult == PixelCopy.SUCCESS) {
                    try {
                        val fileName = "screenshot_${System.currentTimeMillis()}.png"
                        val file = File(activity.cacheDir, fileName)
                        val outputStream = FileOutputStream(file)
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        outputStream.flush()
                        outputStream.close()
                        result.success(file.absolutePath)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving screenshot", e)
                        result.error("SAVE_FAILED", "Failed to save screenshot", null)
                    }
                } else {
                    result.error("COPY_FAILED", "Failed to copy pixels", null)
                }
            }, handler)
        } catch (e: Exception) {
            Log.e(TAG, "Error taking screenshot", e)
            result.error("SCREENSHOT_FAILED", "Unexpected error: ${e.localizedMessage}", null)
        }
    }




    private fun drawLineBetween(a: AnchorNode, b: AnchorNode) {
        val start = a.worldPosition
        val end = b.worldPosition
        val difference = Vector3.subtract(end, start)
        val direction = difference.normalized()
        val length = difference.length()

        MaterialFactory.makeOpaqueWithColor(activity, Color(android.graphics.Color.GREEN))
            .thenAccept { material ->
                val line = ShapeFactory.makeCube(
                    Vector3(0.002f, 0.002f, length),
                    Vector3.zero(),
                    material
                )

                val node = TransformableNode(transformationSystem).apply {
                    renderable = line
                    val mid = Vector3.add(start, end).scaled(0.5f)
                    worldPosition = mid
                    worldRotation = Quaternion.lookRotation(direction, Vector3.up())
                    setParent(sceneView.scene)
                }

                // Distance text
                addDistanceLabel(length, node.worldPosition)
            }
    }


    private fun addDistanceLabel(distance: Float, position: Vector3) {
        val textView = TextView(activity).apply {
            text = String.format("%.2f m", distance)
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(0x80000000.toInt())
            textSize = 12f
        }

        ViewRenderable.builder()
            .setView(activity, textView)
            .build()
            .thenAccept { viewRenderable ->
                val node = TransformableNode(transformationSystem).apply {
                    renderable = viewRenderable
                    worldPosition = position
                    setParent(sceneView.scene)
                }

                // Optional: Disable scale and rotation to keep label upright and same size
                node.select()
                node.scaleController.isEnabled = false
                node.rotationController.isEnabled = false
            }
            .exceptionally {
                Log.e(TAG, "Failed to create ViewRenderable", it)
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
        } else requestCameraPermission()
    }

    override fun onPause(owner: LifecycleOwner) {
        sceneView.pause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        sceneView.destroy()
    }
}