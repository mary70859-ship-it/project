# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Gson model classes
-keepattributes Signature
-keepattributes *Annotation*

# Keep ARCore
-keep class com.google.ar.** { *; }

# Keep SceneView (Sceneform alternative)
-keep class io.github.sceneview.** { *; }
-keep class io.github.sceneview.ux.** { *; }

# Keep Room entities
-keep class com.ar.education.progress.** { *; }
-keep class com.ar.education.data.** { *; }
