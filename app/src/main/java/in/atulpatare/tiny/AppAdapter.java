package in.atulpatare.tiny;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.VH> {

    private final Listener listener;
    private List<AppInfo> allApps = new ArrayList<>();
    private List<AppInfo> shown = new ArrayList<>();
    public AppAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setApps(List<AppInfo> apps) {
        allApps = new ArrayList<>(apps);
        shown = new ArrayList<>(apps);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        shown.clear();
        if (query.isEmpty()) {
            shown.addAll(allApps);
        } else {
            String q = query.toLowerCase();
            for (AppInfo a : allApps) {
                if (a.name.toLowerCase().contains(q)) shown.add(a);
            }
        }
        notifyDataSetChanged();
    }

    public int shownCount() {
        return shown.size();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_drawer, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AppInfo app = shown.get(position);
        h.icon.setImageDrawable(app.icon);
        h.name.setText(app.name);
        h.itemView.setOnClickListener(v -> listener.onTap(app));
        h.itemView.setOnLongClickListener(v -> {
            listener.onHold(app);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return shown.size();
    }

    public interface Listener {
        void onTap(AppInfo app);

        void onHold(AppInfo app);
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;

        VH(View v) {
            super(v);
            icon = v.findViewById(R.id.app_icon);
            name = v.findViewById(R.id.app_name);
        }
    }
}
