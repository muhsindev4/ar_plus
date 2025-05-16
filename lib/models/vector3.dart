import 'dart:math';

class Vector3 {
  double x;
  double y;
  double z;

  Vector3([this.x = 0.0, this.y = 0.0, this.z = 0.0]);

  Vector3.copy(Vector3 v)
      : x = v.x,
        y = v.y,
        z = v.z;

  void set(Vector3 v) {
    x = v.x;
    y = v.y;
    z = v.z;
  }

  void setValues(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  void setZero() => setValues(0.0, 0.0, 0.0);
  void setOne() => setValues(1.0, 1.0, 1.0);
  void setForward() => setValues(0.0, 0.0, -1.0);
  void setBack() => setValues(0.0, 0.0, 1.0);
  void setUp() => setValues(0.0, 1.0, 0.0);
  void setDown() => setValues(0.0, -1.0, 0.0);
  void setRight() => setValues(1.0, 0.0, 0.0);
  void setLeft() => setValues(-1.0, 0.0, 0.0);

  double lengthSquared() => x * x + y * y + z * z;

  double length() => sqrt(lengthSquared());

  Vector3 normalized() {
    final lenSq = lengthSquared();
    if (_almostEqual(lenSq, 0.0)) {
      return Vector3.zero();
    } else if (!_almostEqual(lenSq, 1.0)) {
      final invLen = 1.0 / sqrt(lenSq);
      return scaled(invLen);
    }
    return Vector3.copy(this);
  }

  Vector3 scaled(double scale) => Vector3(x * scale, y * scale, z * scale);

  Vector3 negated() => Vector3(-x, -y, -z);

  static Vector3 add(Vector3 a, Vector3 b) =>
      Vector3(a.x + b.x, a.y + b.y, a.z + b.z);

  static Vector3 subtract(Vector3 a, Vector3 b) =>
      Vector3(a.x - b.x, a.y - b.y, a.z - b.z);

  static double dot(Vector3 a, Vector3 b) =>
      a.x * b.x + a.y * b.y + a.z * b.z;

  static Vector3 cross(Vector3 a, Vector3 b) => Vector3(
    a.y * b.z - a.z * b.y,
    a.z * b.x - a.x * b.z,
    a.x * b.y - a.y * b.x,
  );

  static double componentMax(Vector3 a) => max(max(a.x, a.y), a.z);

  static double componentMin(Vector3 a) => min(min(a.x, a.y), a.z);

  static Vector3 lerp(Vector3 a, Vector3 b, double t) => Vector3(
    _lerp(a.x, b.x, t),
    _lerp(a.y, b.y, t),
    _lerp(a.z, b.z, t),
  );

  static double angleBetweenVectors(Vector3 a, Vector3 b) {
    final lenProduct = a.length() * b.length();
    if (_almostEqual(lenProduct, 0.0)) return 0.0;
    final cosTheta = dot(a, b) / lenProduct;
    return _toDegrees(acos(_clamp(cosTheta, -1.0, 1.0)));
  }

  static bool equals(Vector3 a, Vector3 b) =>
      _almostEqual(a.x, b.x) &&
          _almostEqual(a.y, b.y) &&
          _almostEqual(a.z, b.z);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
          (other is Vector3 && equals(this, other));

  @override
  int get hashCode =>
      x.hashCode ^ y.hashCode ^ z.hashCode;

  @override
  String toString() => '[x=$x, y=$y, z=$z]';

  // Factory constructors
  static Vector3 zero() => Vector3();
  static Vector3 one() => Vector3(1.0, 1.0, 1.0);
  static Vector3 forward() => Vector3(0.0, 0.0, -1.0);
  static Vector3 back() => Vector3(0.0, 0.0, 1.0);
  static Vector3 up() => Vector3(0.0, 1.0, 0.0);
  static Vector3 down() => Vector3(0.0, -1.0, 0.0);
  static Vector3 right() => Vector3(1.0, 0.0, 0.0);
  static Vector3 left() => Vector3(-1.0, 0.0, 0.0);
}

// Helper functions

bool _almostEqual(double a, double b, [double tolerance = 1e-6]) =>
    (a - b).abs() < tolerance;

double _lerp(double a, double b, double t) => a + (b - a) * t;

double _clamp(double val, double minVal, double maxVal) =>
    val < minVal ? minVal : (val > maxVal ? maxVal : val);

double _toDegrees(double radians) => radians * (180.0 / pi);
