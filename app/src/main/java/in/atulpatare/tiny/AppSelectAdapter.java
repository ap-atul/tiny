package in.atulpatare.tiny;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class AppSelectAdapter extends RecyclerView.Adapter<AppSelectAdapter.VH> {

    private static final int MAX = 5;
    private final LinkedHashSet<String> selected;
    private final Listener listener;
    private List<AppInfo> allApps = new ArrayList<>();
    private List<AppInfo> shown = new ArrayList<>();
    public AppSelectAdapter(LinkedHashSet<String> currentPinned, Listener listener) {
        this.selected = currentPinned;
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

    public LinkedHashSet<String> getSelected() {
        return selected;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_select, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AppInfo app = shown.get(position);
        h.icon.setImageDrawable(app.icon);
        h.name.setText(app.name);
        h.check.setChecked(selected.contains(app.packageName));

        h.itemView.setOnClickListener(v -> {
            if (selected.contains(app.packageName)) {
                selected.remove(app.packageName);
            } else if (selected.size() < MAX) {
                selected.add(app.packageName);
            }
            notifyItemChanged(h.getAdapterPosition());
            listener.onChanged(selected.size());
        });
    }

    @Override
    public int getItemCount() {
        return shown.size();
    }

    public interface Listener {
        void onChanged(int count);
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;
        final CheckBox check;

        VH(View v) {
            super(v);
            icon = v.findViewById(R.id.app_icon);
            name = v.findViewById(R.id.app_name);
            check = v.findViewById(R.id.app_check);
        }
    }
}
