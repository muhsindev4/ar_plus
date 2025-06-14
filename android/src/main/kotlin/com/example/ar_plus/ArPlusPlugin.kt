package com.example.ar_plus

import android.app.Activity
import android.util.Log
import androidx.annotation.NonNull
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding


class ArPlusPlugin : FlutterPlugin, ActivityAware {
  private var activity: Activity? = null
  private var lifecycle: Lifecycle? = null
  private var flutterPluginBinding: FlutterPlugin.FlutterPluginBinding? = null
  private  var viewType="ar_plus_view"

  override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    flutterPluginBinding = binding
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    flutterPluginBinding = null
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
    lifecycle = (activity as LifecycleOwner).lifecycle

    // Enregistrer la factory une fois que nous avons l'activité et le lifecycle
    flutterPluginBinding?.let { flutterBinding ->
      flutterBinding.platformViewRegistry.registerViewFactory(
        viewType,
        ArViewFactory(
          messenger = flutterBinding.binaryMessenger,
          activity = activity!!,
          lifecycle = lifecycle!!
        )
      )
    }
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activity = null
    lifecycle = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activity = binding.activity
    lifecycle = (activity as LifecycleOwner).lifecycle

//    // Réenregistrer la factory après les changements de configuration
//    flutterPluginBinding?.let { flutterBinding ->
//      flutterBinding.platformViewRegistry.registerViewFactory(
//        "ar_flutter_plugin_2",
//        ARView(
//          messenger = flutterBinding.binaryMessenger,
//          activity = activity!!,
//          lifecycle = lifecycle!!
//        )
//      )
//    }
  }

  override fun onDetachedFromActivity() {
    activity = null
    lifecycle = null
  }
}