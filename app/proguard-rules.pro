# Proguard rules for Pocket Bansuri.
# Add project specific Keep rules here.
# For more details, see:
# http://developer.android.com/tools/help/proguard.html

# Keep native methods and JNI bindings
-keepclasseswithmembernames class * {
    native <methods>;
}
