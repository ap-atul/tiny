package in.atulpatare.tiny;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import in.atulpatare.tiny.ui.models.AppInfo;

public class PinnedAppsAdapter extends RecyclerView.Adapter<PinnedAppsAdapter.VH> {

    private final Listener listener;
    private List<AppInfo> apps;
    public PinnedAppsAdapter(List<AppInfo> apps, Listener listener) {
        this.apps = new ArrayList<>(apps);
        this.listener = listener;
    }

    public List<AppInfo> getApps() {
        return apps;
    }

    public void updateApps(List<AppInfo> newApps) {
        this.apps = new ArrayList<>(newApps);
        notifyDataSetChanged();
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
        AppInfo app = apps.get(position);
        h.name.setText(app.name);
        h.itemView.setOnClickListener(v -> listener.onTap(app));
        h.itemView.setOnLongClickListener(v -> {
            listener.onHold(app, h.getBindingAdapterPosition());
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    public interface Listener {
        void onTap(AppInfo app);

        void onHold(AppInfo app, int position);
    }

    public static class VH extends RecyclerView.ViewHolder {
        final TextView name;

        VH(View v) {
            super(v);
            name = v.findViewById(R.id.app_name);
        }
    }
}
