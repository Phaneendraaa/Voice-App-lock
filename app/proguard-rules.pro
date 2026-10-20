# Keep JNI-bound classes used by the voice engines
-keep class com.k2fsa.sherpa.onnx.** { *; }
-keep class org.vosk.** { *; }
-keep class com.sun.jna.** { *; }
-dontwarn java.awt.*
