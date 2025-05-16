import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../ar_plus.dart';
import '../models/vector3.dart';

class ArPlusController {
  Future<Vector3> performHitTest(double x, double y) async {
    final result = await channel.invokeMethod<Map>("performHitTest", {
      "x": x,
      "y": y,
    });

    if (result == null) {
      throw Exception("No hit result");
    }

    return Vector3(
      result['x'] as double,
      result['y'] as double,
      result['z'] as double,
    );
  }


  Future shoot() async {
    final result = await channel.invokeMethod<Map>("shoot",);

  }


  Future clearAllPoints() async {
    final result = await channel.invokeMethod<Map>("clearAllPoints",);
  }

  Future restoreVisualElements() async {
    final result = await channel.invokeMethod<Map>("restoreVisualElements",);
  }

  Future hideVisualElements() async {
    final result = await channel.invokeMethod<Map>("hideVisualElements",);
  }

  Future<String?> takeScreenshot() async {
    try{
      final path = await channel.invokeMethod<String>('takeScreenshot');
      return path;
    }catch(e){
     print("EROROOROR:${e.toString()}");
      return null;
    }

  }


}
