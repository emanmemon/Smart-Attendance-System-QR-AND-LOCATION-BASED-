package com.smartattendance.app;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    FirebaseFirestore db;
    String courseId, courseName;
    LinearLayout llRows;
    TextView tvSummary;
    StringBuilder csv = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        db = FirebaseFirestore.getInstance();
        courseId = getIntent().getStringExtra("courseId");
        courseName = getIntent().getStringExtra("courseName");
        llRows = findViewById(R.id.llRows);
        tvSummary = findViewById(R.id.tvSummary);
        ((TextView) findViewById(R.id.tvCourse)).setText(courseName);

        findViewById(R.id.btnExport).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, "Attendance - " + courseName);
            i.putExtra(Intent.EXTRA_TEXT, csv.toString());
            startActivity(Intent.createChooser(i, "Share report"));
        });

        load();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        db.collection("courses").document(courseId).get().addOnSuccessListener(course -> {
            List<?> ids = (List<?>) course.get("studentIds");
            Map<String, Object> names = (Map<String, Object>) course.get("studentNames");

            db.collection("sessions").whereEqualTo("courseId", courseId).get().addOnSuccessListener(ss -> {
                int total = ss.size();
                db.collection("attendance").whereEqualTo("courseId", courseId).get().addOnSuccessListener(as -> {
                    Map<String, Integer> counts = new HashMap<>();
                    for (DocumentSnapshot d : as) {
                        String s = d.getString("studentId");
                        counts.put(s, counts.getOrDefault(s, 0) + 1);
                    }
                    render(ids, names, counts, total);
                });
            });
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void render(List<?> ids, Map<String, Object> names, Map<String, Integer> counts, int total) {
        tvSummary.setText("Total classes: " + total + "  |  Students: " + (ids == null ? 0 : ids.size()));
        csv.setLength(0);
        csv.append("Name,Present,Total,Percentage\n");
        llRows.removeAllViews();
        if (ids == null) return;

        List<String[]> rows = new ArrayList<>();
        for (Object o : ids) {
            String id = (String) o;
            String n = (names != null && names.get(id) != null) ? names.get(id).toString() : "Student";
            rows.add(new String[]{n, id});
        }
        Collections.sort(rows, (a, b) -> a[0].compareToIgnoreCase(b[0]));

        for (String[] r : rows) {
            int present = counts.getOrDefault(r[1], 0);
            int pct = total == 0 ? 0 : Math.round(present * 100f / total);

            View row = getLayoutInflater().inflate(R.layout.item_report_row, llRows, false);
            ((TextView) row.findViewById(R.id.tvName)).setText(r[0]);
            ((TextView) row.findViewById(R.id.tvCount)).setText(present + "/" + total);

            TextView chip = row.findViewById(R.id.tvPercent);
            chip.setText(pct + "%");
            int bg = pct >= 75 ? R.color.success_bg : pct >= 60 ? R.color.warning_bg : R.color.danger_bg;
            int fg = pct >= 75 ? R.color.success : pct >= 60 ? R.color.warning : R.color.danger;
            ((GradientDrawable) chip.getBackground().mutate()).setColor(ContextCompat.getColor(this, bg));
            chip.setTextColor(ContextCompat.getColor(this, fg));
            llRows.addView(row);

            csv.append('"').append(r[0].replace("\"", "\"\"")).append("\",")
               .append(present).append(',').append(total).append(',').append(pct).append("%\n");
        }
    }
}
