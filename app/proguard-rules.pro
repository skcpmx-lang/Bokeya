-keepattributes *Annotation*, InnerClasses, Signature
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.shohankhan.bokeya.**$$serializer { *; }
-keepclassmembers class com.shohankhan.bokeya.** {
    *** Companion;
}
-keepclasseswithmembers class com.shohankhan.bokeya.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class androidx.room.** { *; }
-dontwarn org.slf4j.**
