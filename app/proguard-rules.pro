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

# WorkManager instantiates workers reflectively by class name; R8 cannot see these edges.
-keep class * extends androidx.work.ListenableWorker { public <init>(...); }
-keep class com.shohankhan.bokeya.notifications.** { *; }

# Glance widget receivers are resolved from the manifest / AppWidgetProvider metadata.
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class com.shohankhan.bokeya.widget.** { *; }

# Room entities/DAOs are accessed via generated code and reflection on column names.
-keep class com.shohankhan.bokeya.data.db.** { *; }

# Backup/restore DTOs are serialized by name — renaming them breaks older backup files.
-keep class com.shohankhan.bokeya.backup.** { *; }

# Broadcast receivers named in the manifest.
-keep class * extends android.content.BroadcastReceiver { <init>(); }
