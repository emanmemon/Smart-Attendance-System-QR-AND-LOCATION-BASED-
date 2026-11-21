package com.smartattendance.app;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.VH> {

    public interface Listener {
        void onStart(Course c);
        void onReport(Course c);
    }

    private final List<Course> items;
    private final boolean teacherMode;
    private final Listener listener;

    public CourseAdapter(List<Course> items, boolean teacherMode, Listener listener) {
        this.items = items;
        this.teacherMode = teacherMode;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Course c = items.get(position);
        Context ctx = h.itemView.getContext();
        h.tvName.setText(c.name);

        if (teacherMode) {
            h.tvSub.setText("Join code: " + c.code);
            h.tvPercent.setVisibility(View.GONE);
            h.actions.setVisibility(View.VISIBLE);
            h.btnStart.setOnClickListener(v -> listener.onStart(c));
            h.btnReport.setOnClickListener(v -> listener.onReport(c));
        } else {
            h.actions.setVisibility(View.GONE);
            h.tvPercent.setVisibility(View.VISIBLE);
            int bg, fg;
            String label;
            String sub = c.teacherName == null ? "" : c.teacherName;
            if (c.total == 0) {
                label = "No classes";
                bg = R.color.bg;
                fg = R.color.text_secondary;
            } else {
                int pct = Math.round(c.present * 100f / c.total);
                label = pct + "%";
                if (pct >= 75) { bg = R.color.success_bg; fg = R.color.success; }
                else if (pct >= 60) { bg = R.color.warning_bg; fg = R.color.warning; }
                else { bg = R.color.danger_bg; fg = R.color.danger; }
                if (pct < 75) sub += "  |  Low attendance";
            }
            h.tvSub.setText(sub);
            h.tvPercent.setText(label);
            ((GradientDrawable) h.tvPercent.getBackground().mutate()).setColor(ContextCompat.getColor(ctx, bg));
            h.tvPercent.setTextColor(ContextCompat.getColor(ctx, fg));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvSub, tvPercent;
        View actions;
        MaterialButton btnStart, btnReport;

        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvSub = v.findViewById(R.id.tvSub);
            tvPercent = v.findViewById(R.id.tvPercent);
            actions = v.findViewById(R.id.actions);
            btnStart = v.findViewById(R.id.btnStart);
            btnReport = v.findViewById(R.id.btnReport);
        }
    }
}
