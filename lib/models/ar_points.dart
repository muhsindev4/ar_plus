

class ARPoint {
  final double x;
  final double y;
  final double z;

  ARPoint(this.x, this.y, this.z);

  double distanceTo(ARPoint other) {
    final dx = x - other.x;
    final dy = y - other.y;
    final dz = z - other.z;
    return (dx * dx + dy * dy + dz * dz);
  }

  @override
  String toString() => '($x, $y, $z)';
}
