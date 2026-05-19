# ============================================================================
# Kotlinx Serialization
# ============================================================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Manter classes @Serializable do app
-keep,includedescriptorclasses class com.lifeforge.**$$serializer { *; }
-keepclassmembers class com.lifeforge.** {
    *** Companion;
}
-keepclasseswithmembers class com.lifeforge.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================================================
# Retrofit
# ============================================================================
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# ============================================================================
# OkHttp
# ============================================================================
-dontwarn okhttp3.**
-dontwarn okio.**

# ============================================================================
# Hilt
# ============================================================================
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# ============================================================================
# Engine de Monte Carlo / Otimização — preserva nomes para logs legíveis
# ============================================================================
-keep class com.lifeforge.domain.model.** { *; }
