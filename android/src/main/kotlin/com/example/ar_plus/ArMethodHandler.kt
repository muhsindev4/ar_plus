package com.example.ar_plus

import android.graphics.Color
import com.google.ar.sceneform.ArSceneView
import io.flutter.plugin.common.MethodChannel

class ArMethodHandler(
    private val shapeManager: ShapeManager,
    private val sceneView: ArSceneView,
    private val methodChannel: MethodChannel
) {
    fun setHandler() {
        methodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "makeShape" -> {
                    val shape = call.argument<String>("shape") ?: "cylinder"
                    val radius = call.argument<Double>("radius")?.toFloat() ?: 0.01f
                    val height = call.argument<Double>("height")?.toFloat() ?: 0.01f
                    val width = call.argument<Double>("width")?.toFloat() ?: 0.01f
                    val depth = call.argument<Double>("depth")?.toFloat() ?: 0.01f
                    val x = call.argument<Double>("positionX")?.toFloat() ?: 0f
                    val y = call.argument<Double>("positionY")?.toFloat() ?: 0f
                    val color = (call.argument<Number>("color") ?: Color.YELLOW).toInt()

                    val hit = shapeManager.performHitTest(x, y)
                    if (hit != null) {
                        shapeManager.placeShape(hit, shape, radius, height, width, depth, color) { success, message ->
                            if (success) result.success(message)
                            else result.error("SHAPE_ERROR", message, null)
                        }
                    } else {
                        result.error("NO_HIT", "No suitable surface found", null)
                    }
                }

                "moveShape" -> {
                    val id = call.argument<String>("id") ?: return@setMethodCallHandler result.error("INVALID_ID", "Missing ID", null)
                    val x = call.argument<Double>("x")?.toFloat() ?: 0f
                    val y = call.argument<Double>("y")?.toFloat() ?: 0f
                    val z = call.argument<Double>("z")?.toFloat() ?: 0f
                    if (shapeManager.moveShape(id, x, y, z)) result.success(true)
                    else result.error("NOT_FOUND", "Shape not found", null)
                }

                "removeShape" -> {
                    val id = call.argument<String>("id") ?: return@setMethodCallHandler result.error("INVALID_ID", "Missing ID", null)
                    if (shapeManager.removeShape(id)) result.success(true)
                    else result.error("NOT_FOUND", "Shape not found", null)
                }

                "performHitTest" -> {
                    val x = call.argument<Double>("x")?.toFloat() ?: 0f
                    val y = call.argument<Double>("y")?.toFloat() ?: 0f
                    val hit = shapeManager.performHitTest(x, y)
                    if (hit != null) {
                        val pose = hit.hitPose
                        result.success(mapOf("x" to pose.tx(), "y" to pose.ty(), "z" to pose.tz()))
                    } else result.error("NO_HIT", "No surface hit", null)
                }

                else -> result.notImplemented()
            }
        }
    }
}
