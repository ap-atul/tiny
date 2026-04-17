package in.atulpatare.tiny;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

public class AppInfo {
    public final String name;
    public final String packageName;
    public final Drawable icon;

    public AppInfo(String name, String packageName, Drawable icon) {
        this.name = name;
        this.packageName = packageName;
        this.icon = icon;
    }

    /**
     * Returns null if the package is not installed.
     */
    public static AppInfo from(PackageManager pm, String packageName) {
        try {
            String label = pm.getApplicationLabel(
                    pm.getApplicationInfo(packageName, 0)).toString();
            Drawable icon = pm.getApplicationIcon(packageName);
            return new AppInfo(label, packageName, icon);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
