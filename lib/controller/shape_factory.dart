import 'package:ar_plus/extensions/shape_factory_helper.dart';
import 'package:flutter/material.dart';
import 'package:ar_plus/models/vector3.dart';
import '../ar_plus.dart';
import '../types/shapes.dart';

class ShapeFactory {
  /// Creates a sphere shape in AR space
  Future<String?> makeSphere({
    required double positionX,
    required double positionY,
    double radius = 0.03,
    Color color = Colors.red,
  }) async {
    try{
      final result= await channel.invokeMethod('makeShape', {
        'shape': Shapes.sphere.toId,
        'radius': radius,
        'positionX': positionX,
        'positionY': positionY,
        'color': color.value,
      });
      return result;

    }catch(e){
      debugPrint('Error creating shape: $e');
      return null;
    }

  }

  /// Creates a cylinder shape in AR space
  Future<String?>  makeCylinder({
    required double positionX,
    required double positionY,
    double radius = 0.015,
    double height = 0.05,
    Color color = Colors.green,
  }) async {
    try{
      return await channel.invokeMethod('makeShape', {
        'shape': Shapes.cylinder.toId,
        'radius': radius,
        'height': height,
        'positionX': positionX,
        'positionY': positionY,
        'color': color.value,
      });
    }catch(e){
      debugPrint('Error creating shape: $e');
      return null;
    }

  }

  /// Creates a cube shape in AR space
  Future<String?>  makeCube({
    required double positionX,
    required double positionY,
    double width = 0.05,
    double height = 0.05,
    double depth = 0.05,
    Color color = Colors.blue,
  }) async {
    try{
      return  await channel.invokeMethod('makeShape', {
        'shape': Shapes.cube.toId,
        'width': width,
        'height': height,
        'depth': depth,
        'positionX': positionX,
        'positionY': positionY,
        'color': color.value,
      });
    }catch(e){
      debugPrint('Error creating shape: $e');
      return null;
    }

  }

  Future<void> moveShape({
    required String shapeId,
    required double x,
    required double y,
    required double z,
  }) async {
    await channel.invokeMethod('moveShape', {
      'id': shapeId,
      'x': x,
      'y': y,
      'z': z,
    });
  }

  Future<bool> removeShape(String id) async {
    try {
      return await channel.invokeMethod('removeShape', {'id': id});
    } catch (e) {
      debugPrint('Error removing shape: $e');
      return false;
    }
  }
}
