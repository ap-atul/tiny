package in.atulpatare.tiny;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import in.atulpatare.tiny.ui.drawer.AppDrawerActivity;
import in.atulpatare.tiny.ui.models.AppInfo;
import in.atulpatare.tiny.ui.selector.AppSelectActivity;

public class MainActivity extends Activity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView tvBattery, tvNet, tvTime, tvDate, tvAlarm;
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            refreshBattery(intent);
        }
    };
    private RecyclerView rvPinned;
    private PinnedAppsAdapter pinnedAdapter;
    private GestureDetector gestureDetector;
    private Runnable clockRunnable;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvBattery = findViewById(R.id.tv_battery);
        tvNet = findViewById(R.id.tv_net);
        tvTime = findViewById(R.id.tv_time);
        tvDate = findViewById(R.id.tv_date);
        tvAlarm = findViewById(R.id.tv_alarm);
        rvPinned = findViewById(R.id.rv_pinned);

        applyWindowInsets();
        setupGestureDetector();
        setupPinnedApps();
        startClock();

        findViewById(R.id.btn_settings).setOnClickListener(v -> openSettingsMenu());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideStatusBar();
    }

    private void hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController ctrl = getWindow().getInsetsController();
            if (ctrl != null) {
                ctrl.hide(WindowInsets.Type.statusBars());
                ctrl.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        refreshBattery(null);
        refreshNet();
        refreshAlarm();
        pinnedAdapter.updateApps(loadPinned());
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(batteryReceiver);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clockRunnable != null) handler.removeCallbacks(clockRunnable);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) pinnedAdapter.updateApps(loadPinned());
    }

    // ── Window insets ─────────────────────────────────────────────────────────

    private void applyWindowInsets() {
        View statusSpacer = findViewById(R.id.status_spacer);
        View rootLayout = findViewById(R.id.root_layout);
        rootLayout.setOnApplyWindowInsetsListener((v, insets) -> {
            int topInset;
            int bottomInset;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                topInset = insets.getInsets(WindowInsets.Type.statusBars()).top;
                bottomInset = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
            } else {
                topInset = insets.getSystemWindowInsetTop();
                bottomInset = insets.getSystemWindowInsetBottom();
            }

            statusSpacer.getLayoutParams().height = topInset;
            statusSpacer.requestLayout();

            rvPinned.setPadding(
                    rvPinned.getPaddingLeft(), rvPinned.getPaddingTop(),
                    rvPinned.getPaddingRight(), bottomInset + dp(32));
            return insets;
        });
    }

    // ── Gesture detector — horizontal fling opens the app drawer ─────────────

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2,
                                   float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();
                if (Math.abs(dx) > Math.abs(dy)
                        && Math.abs(dx) > dp(60)
                        && Math.abs(velocityX) > 200) {
                    openDrawer();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    // ── Pinned apps ───────────────────────────────────────────────────────────

    private void setupPinnedApps() {
        pinnedAdapter = new PinnedAppsAdapter(loadPinned(), new PinnedAppsAdapter.Listener() {
            @Override
            public void onTap(AppInfo app) {
                launch(app.packageName);
            }

            @Override
            public void onHold(AppInfo app, int pos) {
                showAppMenu(app, pos);
            }
        });
        rvPinned.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        });
        rvPinned.setAdapter(pinnedAdapter);
    }

    List<AppInfo> loadPinned() {
        SharedPreferences p = getSharedPreferences(Config.PREFS, MODE_PRIVATE);
        String csv = p.getString(Config.KEY_PINNED, "");
        PackageManager pm = getPackageManager();
        List<AppInfo> list = new ArrayList<>();

        if (csv.isEmpty()) {
            for (String pkg : Config.DEFAULT_PKGS) {
                AppInfo info = AppInfo.from(pm, pkg);
                if (info != null && !listContainsPkg(list, pkg)) {
                    list.add(info);
                }
                if (list.size() == 5) break;
            }
        } else {
            for (String pkg : csv.split(",")) {
                AppInfo info = AppInfo.from(pm, pkg.trim());
                if (info != null) list.add(info);
            }
        }
        return list;
    }

    void savePinned(List<String> pkgs) {
        StringBuilder sb = new StringBuilder();
        for (String p : pkgs) {
            if (sb.length() > 0) sb.append(",");
            sb.append(p);
        }
        getSharedPreferences(Config.PREFS, MODE_PRIVATE)
                .edit().putString(Config.KEY_PINNED, sb.toString()).apply();
    }

    private boolean listContainsPkg(List<AppInfo> list, String pkg) {
        for (AppInfo a : list) if (a.packageName.equals(pkg)) return true;
        return false;
    }

    // ── Clock ─────────────────────────────────────────────────────────────────

    private void startClock() {
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                tickClock();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(clockRunnable);
    }

    private void tickClock() {
        boolean h24 = android.text.format.DateFormat.is24HourFormat(this);
        String timeFmt = h24 ? "HH:mm" : "h:mm";
        tvTime.setText(new SimpleDateFormat(timeFmt, Locale.getDefault()).format(new Date()).toLowerCase());
        tvDate.setText(new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(new Date()).toLowerCase());
    }

    // ── Battery ───────────────────────────────────────────────────────────────

    @SuppressLint("SetTextI18n")
    private void refreshBattery(Intent intent) {
        if (intent == null) {
            intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }
        if (intent == null) return;
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        if (level < 0 || scale <= 0) return;
        int pct = (level * 100) / scale;
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
        tvBattery.setText(pct + "%" + (charging ? " ⚡" : ""));
    }

    // ── Network ───────────────────────────────────────────────────────────────

    private void refreshNet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        android.net.Network net = cm.getActiveNetwork();
        if (net == null) {
            tvNet.setText("");
            return;
        }
        NetworkCapabilities caps = cm.getNetworkCapabilities(net);
        if (caps == null) {
            tvNet.setText("");
            return;
        }
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            tvNet.setText(R.string.wifi);
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            tvNet.setText(R.string.data);
        } else {
            tvNet.setText("");
        }
    }

    // ── Alarm ─────────────────────────────────────────────────────────────────

    @SuppressLint("SetTextI18n")
    private void refreshAlarm() {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        AlarmManager.AlarmClockInfo next = am.getNextAlarmClock();
        if (next != null) {
            String fmt = new SimpleDateFormat("EEE, h:mm a", Locale.getDefault())
                    .format(new Date(next.getTriggerTime()));
            tvAlarm.setText("⏰  " + fmt.toLowerCase());
            tvAlarm.setVisibility(View.VISIBLE);
        } else {
            tvAlarm.setVisibility(View.GONE);
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void openDrawer() {
        startActivity(new Intent(this, AppDrawerActivity.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void openSettingsMenu() {
        new AlertDialog.Builder(this)
                .setItems(new String[]{"edit home apps", "set as default launcher"}, (d, w) -> {
                    if (w == 0)
                        startActivityForResult(new Intent(this, AppSelectActivity.class), 1);
                    else startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
                }).show();
    }

    private void launch(String packageName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) startActivity(intent);
    }

    private void showAppMenu(AppInfo app, int pos) {
        new AlertDialog.Builder(this)
                .setTitle(app.name)
                .setItems(new String[]{"remove from home", "app info"}, (d, w) -> {
                    if (w == 0) removeFromHome(pos);
                    else openAppInfo(app.packageName);
                }).show();
    }

    private void removeFromHome(int pos) {
        List<AppInfo> list = new ArrayList<>(pinnedAdapter.getApps());
        list.remove(pos);
        List<String> pkgs = new ArrayList<>();
        for (AppInfo a : list) pkgs.add(a.packageName);
        savePinned(pkgs);
        pinnedAdapter.updateApps(list);
    }

    private void openAppInfo(String pkg) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(android.net.Uri.parse("package:" + pkg));
        startActivity(intent);
    }

    // ── Util ─────────────────────────────────────────────────────────────────

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
