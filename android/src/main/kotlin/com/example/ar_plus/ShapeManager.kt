package com.example.ar_plus

import android.app.Activity
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
import java.util.UUID

class ShapeManager(
    private val sceneView: ArSceneView,
    private val activity: Activity,
    private val transformationSystem: TransformationSystem
) {
    private val shapeMap = mutableMapOf<String, TransformableNode>()

    fun performHitTest(x: Float, y: Float): HitResult? {
        val frame = sceneView.arFrame ?: return null
        val hitResults = frame.hitTest(x, y)

        for (hit in hitResults) {
            val trackable = hit.trackable
            if ((trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) ||
                (trackable is Point && trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
            ) return hit
        }
        return null
    }

    fun placeShape(
        hit: HitResult,
        shape: String,
        radius: Float,
        height: Float,
        width: Float,
        depth: Float,
        color: Int,
        onResult: (Boolean, String?) -> Unit
    ) {
        val anchorNode = AnchorNode(hit.createAnchor()).apply {
            setParent(sceneView.scene)
        }

        MaterialFactory.makeOpaqueWithColor(activity, com.google.ar.sceneform.rendering.Color(color))
            .thenAccept { material ->
                val renderable = when (shape.lowercase()) {
                    "cylinder" -> ShapeFactory.makeCylinder(radius, height, Vector3.zero(), material)
                    "sphere" -> ShapeFactory.makeSphere(radius, Vector3.zero(), material)
                    "cube" -> ShapeFactory.makeCube(Vector3(width, height, depth), Vector3.zero(), material)
                    else -> {
                        onResult(false, "Unknown shape")
                        return@thenAccept
                    }
                }

                val node = TransformableNode(transformationSystem).apply {
                    setParent(anchorNode)
                    this.renderable = renderable
                    select()
                }

                val id = UUID.randomUUID().toString()
                shapeMap[id] = node
                onResult(true, id)
            }.exceptionally {
                onResult(false, it.localizedMessage)
                null
            }
    }

    fun moveShape(id: String, x: Float, y: Float, z: Float): Boolean {
        shapeMap[id]?.let {
            it.localPosition = Vector3(x, y, z)
            return true
        }
        return false
    }

    fun removeShape(id: String): Boolean {
        return shapeMap.remove(id)?.apply { setParent(null) } != null
    }
}
