# SmartEQ Pro release ProGuard/R8 rules.
# Adds tamper-resistance on top of R8's default obfuscation/shrinking:
# class and member names in release builds are renamed, making static
# reverse-engineering and simple binary patching meaningfully harder.

# --- Kotlinx Serialization: keep generated serializers for our DTOs ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class com.smarteq.pro.data.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.smarteq.pro.**$$serializer { *; }
-keepclassmembers class com.smarteq.pro.** {
    *** Companion;
}
-keepclasseswithmembers class com.smarteq.pro.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# --- Retrofit / OkHttp ---
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keepattributes Signature, Exceptions

# --- Hilt ---
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# --- Security-critical: never let R8 accidentally inline/constant-fold
#     the encrypted-prefs / keystore alias strings in a way that would
#     make them easier to grep out of the compiled DEX ---
-keepclassmembers class com.smarteq.pro.data.security.** {
    private static final java.lang.String *;
}

# General Android/Kotlin metadata needed for reflection-based libraries
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }
