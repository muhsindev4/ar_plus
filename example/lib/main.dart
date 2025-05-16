import 'dart:io';
import 'package:ar_plus/models/vector3.dart';
import 'package:ar_plus/widgets/ar_plus_view.dart';
import 'package:flutter/material.dart';
import 'package:ar_plus/ar_plus.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      debugShowCheckedModeBanner: false,
      home: ARHomePage(),
    );
  }
}

class ARHomePage extends StatefulWidget {
  const ARHomePage({super.key});

  @override
  State<ARHomePage> createState() => _ARHomePageState();
}

class _ARHomePageState extends State<ARHomePage> {
  final ArPlusController _arPlusController = ArPlusController();
  String? screenshotPath;

  void _takeScreenshot() async {
    try {
      // await _arPlusController.hideVisualElements();hideVisualElements
      final path = await _arPlusController.takeScreenshot();
      // await _arPlusController.restoreVisualElements();
      if (path != null) {
        setState(() {
          screenshotPath = path;
        });

        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Screenshot captured!')),
          );
        }
      }
    } catch (e) {
      debugPrint('Error taking screenshot: $e');
    }


  }

  void _resetMeasurement() {
    _arPlusController.clearAllPoints();
    setState(() {
      screenshotPath = null;
    });
  }

  void _viewScreenshot() {
    if (screenshotPath == null) return;
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => FullImageView(imagePath: screenshotPath!),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          /// AR View
          ArViewWidget(
            onDetectedPlanes: () {},
            onTap: (x, y) {},
          ),

          /// Top Gradient
          Positioned.fill(
            child: Container(
              decoration: const BoxDecoration(
                gradient: LinearGradient(
                  colors: [Colors.black54, Colors.transparent],
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                ),
              ),
            ),
          ),

          /// Instruction Text
          Positioned(
            top: 40,
            left: 20,
            right: 20,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              decoration: BoxDecoration(
                color: Colors.black87.withOpacity(0.6),
                borderRadius: BorderRadius.circular(12),
              ),
              child: const Text(
                'Tap on two points to measure distance',
                style: TextStyle(color: Colors.white, fontSize: 16),
                textAlign: TextAlign.center,
              ),
            ),
          ),

          /// Screenshot Preview Thumbnail
          if (screenshotPath != null)
            Positioned(
              bottom: 120,
              right: 20,
              child: GestureDetector(
                onTap: _viewScreenshot,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(12),
                  child: Image.file(
                    File(screenshotPath!),
                    width: 100,
                    height: 100,
                    fit: BoxFit.cover,
                  ),
                ),
              ),
            ),

          /// Buttons: Screenshot & Clear
          Positioned(
            bottom: 40,
            left: 20,
            right: 20,
            child: Row(
              children: [
                /// Screenshot Button
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: () async {
                      await _arPlusController.shoot();
                    },
                    icon: const Icon(Icons.camera_alt_rounded),
                    label: const Padding(
                      padding: EdgeInsets.symmetric(vertical: 14),
                      child: Text('Add point', style: TextStyle(fontSize: 16)),
                    ),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.deepPurple,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(14),
                      ),
                      elevation: 6,
                    ),
                  ),
                ),
                const SizedBox(width: 16),

                /// Clear Points Button
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: _takeScreenshot,
                    icon: const Icon(Icons.clear_all),
                    label: const Padding(
                      padding: EdgeInsets.symmetric(vertical: 14),
                      child: Text('Take Screenshot', style: TextStyle(fontSize: 16)),
                    ),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.redAccent,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(14),
                      ),
                      elevation: 6,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class FullImageView extends StatelessWidget {
  final String imagePath;

  const FullImageView({super.key, required this.imagePath});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Screenshot Preview')),
      backgroundColor: Colors.black,
      body: Center(
        child: Image.file(File(imagePath)),
      ),
    );
  }
}
