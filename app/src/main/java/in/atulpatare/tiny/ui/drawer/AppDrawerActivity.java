package in.atulpatare.tiny.ui.drawer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.TextView;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import in.atulpatare.tiny.Config;
import in.atulpatare.tiny.R;
import in.atulpatare.tiny.ui.models.AppInfo;

public class AppDrawerActivity extends Activity {

    private AppAdapter adapter;
    private TextView tvCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_drawer);

        applyWindowInsets();

        RecyclerView rv = findViewById(R.id.rv_apps);
        EditText etSearch = findViewById(R.id.et_search);
        tvCount = findViewById(R.id.tv_app_count);

        adapter = new AppAdapter(new AppAdapter.Listener() {
            @Override
            public void onTap(AppInfo app) {
                launch(app.packageName);
            }

            @Override
            public void onHold(AppInfo app) {
                showMenu(app);
            }
        });

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                adapter.filter(s.toString());
                updateCount();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Focus search and show keyboard
        etSearch.requestFocus();

        loadAppsAsync();
    }

    private void applyWindowInsets() {
        View spacer = findViewById(R.id.status_spacer);
        findViewById(R.id.drawer_root).setOnApplyWindowInsetsListener((v, insets) -> {
            int topInset;
            int bottomInset;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                topInset = insets.getInsets(WindowInsets.Type.statusBars()).top;
                bottomInset = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
            } else {
                topInset = insets.getSystemWindowInsetTop();
                bottomInset = insets.getSystemWindowInsetBottom();
            }

            spacer.getLayoutParams().height = topInset;
            spacer.requestLayout();
            return insets;
        });
    }

    private void loadAppsAsync() {
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);

            List<AppInfo> apps = new ArrayList<>();
            for (ResolveInfo ri : resolveInfos) {
                String name = ri.loadLabel(pm).toString();
                String pkg = ri.activityInfo.packageName;
                android.graphics.drawable.Drawable icon = ri.loadIcon(pm);
                apps.add(new AppInfo(name, pkg, icon));
            }
            apps.sort((a, b) -> a.name.compareToIgnoreCase(b.name));

            runOnUiThread(() -> {
                adapter.setApps(apps);
                updateCount();
            });
        }).start();
    }

    @SuppressLint("SetTextI18n")
    private void updateCount() {
        int count = adapter.shownCount();
        tvCount.setText(count + (count == 1 ? " app" : " apps"));
    }

    private void launch(String pkg) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(pkg);
        if (intent != null) startActivity(intent);
    }

    private void showMenu(AppInfo app) {
        new AlertDialog.Builder(this)
                .setTitle(app.name)
                .setItems(new String[]{"add to home", "app info"}, (d, w) -> {
                    if (w == 0) addToHome(app);
                    else openAppInfo(app.packageName);
                }).show();
    }

    private void addToHome(AppInfo app) {
        android.content.SharedPreferences prefs =
                getSharedPreferences(Config.PREFS, MODE_PRIVATE);
        String csv = prefs.getString(Config.KEY_PINNED, "");
        List<String> pkgs = new ArrayList<>();
        if (!csv.isEmpty()) {
            for (String p : csv.split(",")) pkgs.add(p.trim());
        }
        if (pkgs.size() >= 5) {
            android.widget.Toast.makeText(this, "home screen is full (5/5)", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pkgs.contains(app.packageName)) {
            pkgs.add(app.packageName);
            StringBuilder sb = new StringBuilder();
            for (String p : pkgs) {
                if (sb.length() > 0) sb.append(",");
                sb.append(p);
            }
            prefs.edit().putString(Config.KEY_PINNED, sb.toString()).apply();
            android.widget.Toast.makeText(this, app.name + " added to home", android.widget.Toast.LENGTH_SHORT).show();
        } else {
            android.widget.Toast.makeText(this, app.name + " is already on home", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void openAppInfo(String pkg) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(android.net.Uri.parse("package:" + pkg));
        startActivity(intent);
    }

    @NonNull
    @Override
    public OnBackInvokedDispatcher getOnBackInvokedDispatcher() {
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        return super.getOnBackInvokedDispatcher();
    }
}
