import UIKit
import ARKit
import Flutter

class Measurement: NSObject, FlutterPlatformView {
    private var arView: ARSCNView
    private var lastNode: SCNNode?
    private var lastPosition: SCNVector3?
    private var methodChannel: FlutterMethodChannel

    init(frame: CGRect,
         viewIdentifier viewId: Int64,
         arguments args: Any?,
         binaryMessenger messenger: FlutterBinaryMessenger) {

        arView = ARSCNView(frame: frame)
        methodChannel = FlutterMethodChannel(name: "ar_plus_view_channel", binaryMessenger: messenger)

        super.init()

        setupARSession()
        setupGesture()
        setupMethodHandler()
    }

    func view() -> UIView {
        return arView
    }

    private func setupARSession() {
        let config = ARWorldTrackingConfiguration()
        config.planeDetection = [.horizontal, .vertical]
        arView.session.run(config)
    }

    private func setupGesture() {
        let tap = UITapGestureRecognizer(target: self, action: #selector(handleTap(_:)))
        arView.addGestureRecognizer(tap)
    }

    @objc private func handleTap(_ gesture: UITapGestureRecognizer) {
        let location = gesture.location(in: arView)
        let results = arView.hitTest(location, types: [.featurePoint, .estimatedHorizontalPlane, .estimatedVerticalPlane])
        guard let result = results.first else { return }

        let position = SCNVector3(result.worldTransform.columns.3.x,
                                  result.worldTransform.columns.3.y,
                                  result.worldTransform.columns.3.z)

        placeDot(at: position)
    }

    private func placeDot(at position: SCNVector3) {
        let sphere = SCNSphere(radius: 0.01)
        sphere.firstMaterial?.diffuse.contents = UIColor.red

        let node = SCNNode(geometry: sphere)
        node.position = position
        arView.scene.rootNode.addChildNode(node)

        if let last = lastPosition {
            drawLine(from: last, to: position)
            let distance = distanceBetween(point1: last, point2: position)
            methodChannel.invokeMethod("onDistanceMeasured", arguments: ["distance": distance])
        }

        lastPosition = position
        lastNode = node
    }

    private func drawLine(from: SCNVector3, to: SCNVector3) {
        let lineGeometry = SCNCylinder(radius: 0.002, height: CGFloat(distanceBetween(point1: from, point2: to)))
        lineGeometry.firstMaterial?.diffuse.contents = UIColor.green

        let lineNode = SCNNode(geometry: lineGeometry)
        lineNode.position = midpoint(from, to)
        lineNode.eulerAngles = SCNVector3.lineEulerAngles(from: from, to: to)
        arView.scene.rootNode.addChildNode(lineNode)
    }

    private func distanceBetween(point1: SCNVector3, point2: SCNVector3) -> Float {
        let dx = point1.x - point2.x
        let dy = point1.y - point2.y
        let dz = point1.z - point2.z
        return sqrt(dx*dx + dy*dy + dz*dz)
    }

    private func midpoint(_ a: SCNVector3, _ b: SCNVector3) -> SCNVector3 {
        return SCNVector3((a.x + b.x)/2, (a.y + b.y)/2, (a.z + b.z)/2)
    }

    private func setupMethodHandler() {
        methodChannel.setMethodCallHandler { [weak self] call, result in
            switch call.method {
            case "clearAllPoints":
                self?.clearScene()
                result(nil)
            default:
                result(FlutterMethodNotImplemented)
            }
        }
    }

    private func clearScene() {
        arView.scene.rootNode.childNodes.forEach { $0.removeFromParentNode() }
        lastPosition = nil
    }
}

extension SCNVector3 {
    static func lineEulerAngles(from: SCNVector3, to: SCNVector3) -> SCNVector3 {
        let delta = SCNVector3(to.x - from.x, to.y - from.y, to.z - from.z)
        let length = sqrt(delta.x * delta.x + delta.y * delta.y + delta.z * delta.z)
        let yaw = atan2(delta.z, delta.x)
        let pitch = atan2(delta.y, length)
        return SCNVector3(pitch, -yaw, 0)
    }
}
