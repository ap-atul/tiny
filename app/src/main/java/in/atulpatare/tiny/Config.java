package in.atulpatare.tiny;

public class Config {

    public static final String PREFS = "launcher_prefs";
    public static final String KEY_PINNED = "pinned_csv";

    // Default pinned apps — covers AOSP, Pixel, and Samsung naming variants
    public static final String[] DEFAULT_PKGS = {
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.contacts",
            "com.android.mms",
            "com.google.android.apps.messaging",
            "com.android.camera2",
            "com.android.settings"
    };
}
