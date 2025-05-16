import 'package:ar_plus/types/shapes.dart';

extension ShapeFactoryHelper on Shapes {
  String get toId {
    switch (this) {
      case Shapes.cube:
        return "cube";
      case Shapes.sphere:
        return "sphere";
      case Shapes.cylinder:
        return "cylinder";
      case Shapes.cone:
        return "cone";
      case Shapes.plane:
        return "plane";
    }
  }
}
