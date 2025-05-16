import Flutter
import UIKit

public class ArPlusPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let factory = ARViewFactory(messenger: registrar.messenger())
        registrar.register(factory, withId: "ar_plus_view")
    }
}
