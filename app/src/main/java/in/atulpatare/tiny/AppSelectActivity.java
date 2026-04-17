package in.atulpatare.tiny;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class AppSelectActivity extends AppCompatActivity {

    private AppSelectAdapter adapter;
    private TextView tvCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_select);

        applyWindowInsets();

        RecyclerView rv = findViewById(R.id.rv_apps);
        EditText etSearch = findViewById(R.id.et_search);
        tvCount = findViewById(R.id.tv_selected_count);
        TextView btnSave = findViewById(R.id.btn_save);
        TextView btnBack = findViewById(R.id.btn_back);

        // Load currently pinned packages into an ordered set
        LinkedHashSet<String> current = loadCurrentPinned();

        adapter = new AppSelectAdapter(current, count -> updateCount(count));

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveAndFinish());

        updateCount(current.size());
        loadAppsAsync();
    }

    private void applyWindowInsets() {
        View spacer = findViewById(R.id.status_spacer);
        spacer.setOnApplyWindowInsetsListener((v, insets) -> {
            v.getLayoutParams().height = insets.getInsets(WindowInsets.Type.statusBars()).top;
            v.requestLayout();
            return insets;
        });
    }

    private LinkedHashSet<String> loadCurrentPinned() {
        SharedPreferences p = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        String csv = p.getString(MainActivity.KEY_PINNED, "");
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (!csv.isEmpty()) {
            for (String pkg : csv.split(",")) set.add(pkg.trim());
        }
        return set;
    }

    private void loadAppsAsync() {
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);

            List<AppInfo> apps = new ArrayList<>();
            for (ResolveInfo ri : resolveInfos) {
                String name = ri.loadLabel(pm).toString();
                String pkg = ri.activityInfo.packageName;
                android.graphics.drawable.Drawable icon = ri.loadIcon(pm);
                apps.add(new AppInfo(name, pkg, icon));
            }
            apps.sort((a, b) -> a.name.compareToIgnoreCase(b.name));

            runOnUiThread(() -> adapter.setApps(apps));
        }).start();
    }

    private void updateCount(int count) {
        tvCount.setText(count + "/5");
    }

    private void saveAndFinish() {
        LinkedHashSet<String> selected = adapter.getSelected();
        List<String> pkgs = new ArrayList<>(selected);

        StringBuilder sb = new StringBuilder();
        for (String p : pkgs) {
            if (sb.length() > 0) sb.append(",");
            sb.append(p);
        }
        getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
                .edit().putString(MainActivity.KEY_PINNED, sb.toString()).apply();

        setResult(RESULT_OK);
        finish();
    }
}
