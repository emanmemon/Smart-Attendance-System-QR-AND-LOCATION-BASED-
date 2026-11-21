package com.smartattendance.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentDashboardActivity extends AppCompatActivity {

    FirebaseFirestore db;
    String uid, myName = "Student";
    List<Course> list = new ArrayList<>();
    CourseAdapter adapter;
    TextView tvHello, tvEmpty, tvOverall, tvOverallSub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();
        tvHello = findViewById(R.id.tvHello);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvOverall = findViewById(R.id.tvOverall);
        tvOverallSub = findViewById(R.id.tvOverallSub);

        RecyclerView rv = findViewById(R.id.rvCourses);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CourseAdapter(list, false, null);
        rv.setAdapter(adapter);

        findViewById(R.id.fabScan).setOnClickListener(v ->
                startActivity(new Intent(this, ScanActivity.class)));
        findViewById(R.id.tvJoin).setOnClickListener(v ->
                Utils.inputDialog(this, "Join course", "Course code (e.g. K7M2QX)",
                        code -> joinCourse(code.toUpperCase(Locale.ROOT))));
        findViewById(R.id.tvLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

        db.collection("users").document(uid).get().addOnSuccessListener(d -> {
            String n = d.getString("name");
            if (n != null) {
                myName = n;
                tvHello.setText(n);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCourses();
    }

    private void loadCourses() {
        db.collection("courses").whereArrayContains("studentIds", uid).get().addOnSuccessListener(snap -> {
            list.clear();
            for (DocumentSnapshot d : snap) {
                list.add(new Course(d.getId(), d.getString("name"), d.getString("code"), d.getString("teacherName")));
            }
            adapter.notifyDataSetChanged();
            tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            updateOverall();
            for (Course c : list) loadStats(c);
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void loadStats(Course c) {
        db.collection("sessions").whereEqualTo("courseId", c.id).get().addOnSuccessListener(s -> {
            c.total = s.size();
            db.collection("attendance")
                    .whereEqualTo("studentId", uid)
                    .whereEqualTo("courseId", c.id)
                    .get().addOnSuccessListener(a -> {
                        c.present = a.size();
                        adapter.notifyDataSetChanged();
                        updateOverall();
                    });
        });
    }

    private void updateOverall() {
        int p = 0, t = 0;
        for (Course c : list) {
            p += c.present;
            t += c.total;
        }
        int pct = t == 0 ? 0 : Math.round(p * 100f / t);
        tvOverall.setText(pct + "%");
        tvOverallSub.setText(t == 0 ? "Abhi koi class nahi hui" : p + " / " + t + " classes attend ki");
    }

    private void joinCourse(String code) {
        db.collection("courses").whereEqualTo("code", code).limit(1).get().addOnSuccessListener(snap -> {
            if (snap.isEmpty()) {
                Toast.makeText(this, "Ye code galat hai", Toast.LENGTH_LONG).show();
                return;
            }
            DocumentSnapshot d = snap.getDocuments().get(0);
            Map<String, Object> up = new HashMap<>();
            up.put("studentIds", FieldValue.arrayUnion(uid));
            up.put("studentNames." + uid, myName);
            d.getReference().update(up).addOnSuccessListener(x -> {
                Toast.makeText(this, "Course join ho gaya", Toast.LENGTH_SHORT).show();
                loadCourses();
            }).addOnFailureListener(e ->
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }
}
