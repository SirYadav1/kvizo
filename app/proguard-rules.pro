# Keep Ed25519 verification classes
-keep class java.security.** { *; }
-keep class com.kvizo.app.util.CommunityFetch { *; }
-keep class com.kvizo.app.util.CommunityFetch$CacheProvider { *; }
-keep class com.kvizo.app.data.CommunityQuizManager { *; }
-keep class com.kvizo.app.util.AnnouncementWorker { *; }
-keep class com.kvizo.app.util.AnnouncementNotifier { *; }
-keep class com.kvizo.app.util.BackupManager { *; }
-keep class com.kvizo.app.util.ShareCodec { *; }
-keep class com.kvizo.app.util.StringProvider { *; }
-keep class com.kvizo.app.data.Entities$* { *; }
-keep class org.json.** { *; }

-keepclassmembers class **.R$* {
    public static <fields>;
}

-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
