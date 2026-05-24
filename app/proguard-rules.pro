# ─── Cipher ProGuard / R8 rules ────────────────────────────────────────────

# Preserve source / line numbers for usable crash traces in release.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ─── kotlinx.serialization ────────────────────────────────────────────────
# Without these, R8 strips the synthetic $serializer companions and any
# @Serializable-driven encode/decode crashes at runtime (or fails R8 itself
# on the sealed Message hierarchy).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep the runtime markers and polymorphism plumbing
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep every Cipher model class and its generated $serializer.
-keep,includedescriptorclasses class uz.angrykitten.spygame.model.** { *; }
-keepclassmembers class uz.angrykitten.spygame.model.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class uz.angrykitten.spygame.model.**$$serializer { *; }

# Generic rule for any other @Serializable class we add later.
-keep,includedescriptorclasses @kotlinx.serialization.Serializable class ** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class **$$serializer { *; }

# ─── Hilt / Dagger ────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.AndroidEntryPoint <init>(...);
}
-keep @dagger.hilt.* class * { *; }
-keep @javax.inject.Singleton class * { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
    @javax.inject.Inject <methods>;
}

# ─── Jetpack Compose ──────────────────────────────────────────────────────
# Compose ships its own R8 rules, but keep our @Composable signatures so
# tooling-only previews don't get pruned in surprising ways.
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ─── AndroidX DataStore ───────────────────────────────────────────────────
-keep class androidx.datastore.*.** { *; }

# ─── ZXing (QR generation) ────────────────────────────────────────────────
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# ─── ML Kit Barcode (QR scanning) ─────────────────────────────────────────
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.vision.**

# ─── CameraX ──────────────────────────────────────────────────────────────
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ─── Kotlin reflection / coroutines internals ─────────────────────────────
-keepclassmembers class kotlin.Metadata { *; }
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { }
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory { }

# ─── App entry points the framework looks up by name ─────────────────────
-keep class uz.angrykitten.spygame.SpyApp { *; }
-keep class uz.angrykitten.spygame.MainActivity { *; }

# Suppress harmless warnings emitted by libraries with optional deps.
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
