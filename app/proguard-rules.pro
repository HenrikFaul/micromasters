# MicroMasters — R8 keep rules for the release (minified/obfuscated) build.

# --- WebView JavaScript bridge ---------------------------------------------
# Methods annotated @JavascriptInterface are invoked by name from game.js inside
# the WebView. R8 must not rename or remove them, or the in-game back button breaks.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.micromasters.game.Game3DActivity$Bridge { *; }

# --- Custom Views inflated from XML ----------------------------------------
# GameView is inflated from layout XML, which needs the (Context, AttributeSet) ctor.
-keepclasseswithmembers class * extends android.view.View {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# --- Model enums ------------------------------------------------------------
# Persistence keys on enum .ordinal (stable under R8), but keep the constants
# intact defensively in case any value is ever round-tripped by name.
-keepclassmembers enum com.micromasters.game.** { *; }

# --- Keep attributes needed for sane stack traces & annotations -------------
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, JavascriptInterface
-dontwarn org.jetbrains.annotations.**
