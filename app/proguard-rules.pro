# ─────────────────────────────────────────────────────────────────────────────
# UNDOC — Production ProGuard / R8 rules
# ─────────────────────────────────────────────────────────────────────────────

# Keep line numbers in stack traces for crash analysis
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── App model & repository (serialised to/from JSON) ─────────────────────────
-keepclassmembers class com.docexpiry.tracker.Document {
    <fields>;
    <init>(...);
    public *;
}
-keepclassmembers class com.docexpiry.tracker.DocumentRepository {
    public *;
}

# ── Subscription interfaces (anonymous classes passed as lambdas) ─────────────
-keep interface com.docexpiry.tracker.SubscriptionManager$StatusCallback { *; }
-keep interface com.docexpiry.tracker.SubscriptionManager$ErrorCallback  { *; }

# ── Razorpay ──────────────────────────────────────────────────────────────────
-keepclassmembers class com.razorpay.** { *; }
-keep class com.razorpay.** { *; }
-dontwarn com.razorpay.**
-keep class proguard.annotation.Keep { *; }
-keep class proguard.annotation.KeepClassMembers { *; }

# ── AndroidX / Material ──────────────────────────────────────────────────────
-keep class androidx.** { *; }
-dontwarn androidx.**
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ── Security-crypto (EncryptedSharedPreferences) ──────────────────────────────
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# ── Standard Android safety rules ────────────────────────────────────────────
-keepclassmembers class * extends android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ── Remove debug logging in release ──────────────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
