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
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherDashboardActivity extends AppCompatActivity {

    FirebaseFirestore db;
    String uid, myName = "Teacher";
    List<Course> list = new ArrayList<>();
    CourseAdapter adapter;
    TextView tvHello, tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();
        tvHello = findViewById(R.id.tvHello);
        tvEmpty = findViewById(R.id.tvEmpty);

        RecyclerView rv = findViewById(R.id.rvCourses);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CourseAdapter(list, true, new CourseAdapter.Listener() {
            @Override
            public void onStart(Course c) {
                Intent i = new Intent(TeacherDashboardActivity.this, QrSessionActivity.class);
                i.putExtra("courseId", c.id);
                i.putExtra("courseName", c.name);
                startActivity(i);
            }

            @Override
            public void onReport(Course c) {
                Intent i = new Intent(TeacherDashboardActivity.this, ReportActivity.class);
                i.putExtra("courseId", c.id);
                i.putExtra("courseName", c.name);
                startActivity(i);
            }
        });
        rv.setAdapter(adapter);

        findViewById(R.id.fabAdd).setOnClickListener(v ->
                Utils.inputDialog(this, "New course", "Course name (e.g. Mobile App Dev)", this::createCourse));

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
                tvHello.setText("Hello, " + n);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCourses();
    }

    private void loadCourses() {
        db.collection("courses").whereEqualTo("teacherId", uid).get().addOnSuccessListener(snap -> {
            list.clear();
            for (DocumentSnapshot d : snap) {
                list.add(new Course(d.getId(), d.getString("name"), d.getString("code"), d.getString("teacherName")));
            }
            adapter.notifyDataSetChanged();
            tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void createCourse(String name) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("code", Utils.randomCode(6));
        m.put("teacherId", uid);
        m.put("teacherName", myName);
        m.put("studentIds", new ArrayList<String>());
        m.put("studentNames", new HashMap<String, Object>());
        m.put("createdAt", System.currentTimeMillis());
        db.collection("courses").add(m)
                .addOnSuccessListener(r -> loadCourses())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
