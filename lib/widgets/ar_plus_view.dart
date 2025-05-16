
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';


class ArViewWidget extends StatefulWidget {
  const ArViewWidget({super.key,this.onTap,this.onCreated,this.onDetectedPlanes});
  final Function? onCreated;
  final Function? onDetectedPlanes;
  final Function(double x, double y)? onTap;

  @override
  State<ArViewWidget> createState() => _ArViewWidgetState();
}

class _ArViewWidgetState extends State<ArViewWidget> {
  static const String viewType = 'ar_plus_view';
  static const MethodChannel _channel = MethodChannel('ar_plus_view_channel');

  @override
  void initState() {
    super.initState();

    _channel.setMethodCallHandler((call) async {
      switch (call.method) {
        case 'onCreated':
          debugPrint('AR view created');
          widget.onCreated?.call();
          break;
        case 'onDetectedPlanes':
          debugPrint('Planes detected');
          widget.onDetectedPlanes?.call();
          break;
        case 'onTap':
          final x = call.arguments['x'];
          final y = call.arguments['y'];
          debugPrint('Tapped at: x=$x, y=$y');
          widget.onTap?.call(x,y);
          break;
      }
    });
  }



  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform == TargetPlatform.iOS) {
      return UiKitView(
        viewType: viewType,
        creationParams: {},
        creationParamsCodec: const StandardMessageCodec(),
      );
    }else if(defaultTargetPlatform == TargetPlatform.android) {
      return AndroidView(
        viewType: viewType,
        onPlatformViewCreated: (_) {},
      );
    }
      else {
      return const Text("Unsupported Platform");
    }

  }
}
