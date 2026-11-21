package com.smartattendance.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScanActivity extends AppCompatActivity {

    static final float MAX_DISTANCE_M = 100f;

    FirebaseFirestore db;
    String uid, sessionId, courseId;
    double sLat, sLng;
    ProgressBar progress;
    TextView tvTitle, tvMsg;
    View btnDone;

    ActivityResultLauncher<String> locPerm = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchScanner();
                else showResult(false, "Location permission chahiye",
                        "Settings mein location allow karke dobara try karein.");
            });

    ActivityResultLauncher<ScanOptions> scanner = registerForActivityResult(new ScanContract(), r -> {
        if (r.getContents() == null) finish();
        else checkSession(r.getContents());
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();
        progress = findViewById(R.id.progress);
        tvTitle = findViewById(R.id.tvTitle);
        tvMsg = findViewById(R.id.tvMsg);
        btnDone = findViewById(R.id.btnDone);
        btnDone.setOnClickListener(v -> finish());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            launchScanner();
        } else {
            locPerm.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void launchScanner() {
        ScanOptions o = new ScanOptions();
        o.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        o.setPrompt("Teacher ka QR scan karein");
        o.setBeepEnabled(false);
        o.setOrientationLocked(false);
        scanner.launch(o);
    }

    // Check 1: QR sahi aur taaza hai?
    private void checkSession(String raw) {
        loading("QR check ho raha hai…");
        String[] p = raw.split("\\|");
        if (p.length != 2) {
            showResult(false, "Ye attendance QR nahi hai", "Teacher ki screen ka QR scan karein.");
            return;
        }
        sessionId = p[0];
        String scanned = p[1];

        db.collection("sessions").document(sessionId).get().addOnSuccessListener(d -> {
            if (!d.exists() || !Boolean.TRUE.equals(d.getBoolean("active"))) {
                showResult(false, "Session khatam", "Ye class ka session ab active nahi hai.");
                return;
            }
            String t = d.getString("token"), pt = d.getString("prevToken");
            if (!scanned.equals(t) && !scanned.equals(pt)) {
                showResult(false, "QR purana hai", "Screenshot kaam nahi karta. Teacher ki screen se abhi scan karein.");
                return;
            }
            Double lat = d.getDouble("lat"), lng = d.getDouble("lng");
            if (lat == null || lng == null) {
                showResult(false, "Session data adhura hai", "Teacher se naya session start karwayein.");
                return;
            }
            courseId = d.getString("courseId");
            sLat = lat;
            sLng = lng;
            checkEnrollment();
        }).addOnFailureListener(e -> showResult(false, "Network error", e.getMessage()));
    }

    // Check 2: student is course mein hai?
    private void checkEnrollment() {
        db.collection("courses").document(courseId).get().addOnSuccessListener(d -> {
            List<?> ids = (List<?>) d.get("studentIds");
            if (ids == null || !ids.contains(uid)) {
                showResult(false, "Aap enrolled nahi hain", "Pehle dashboard se course join karein.");
                return;
            }
            checkDuplicate();
        }).addOnFailureListener(e -> showResult(false, "Network error", e.getMessage()));
    }

    // Check 3: attendance pehle se to nahi lagi?
    private void checkDuplicate() {
        db.collection("attendance").document(sessionId + "_" + uid).get().addOnSuccessListener(d -> {
            if (d.exists()) {
                showResult(true, "Already marked", "Is class ki attendance pehle hi lag chuki hai.");
                return;
            }
            checkLocation();
        }).addOnFailureListener(e -> showResult(false, "Network error", e.getMessage()));
    }

    // Check 4 aur 5: fake GPS aur distance
    @SuppressLint("MissingPermission")
    @SuppressWarnings("deprecation")
    private void checkLocation() {
        loading("Location check ho rahi hai…");
        LocationServices.getFusedLocationProviderClient(this)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(loc -> {
                    if (loc == null) {
                        showResult(false, "Location nahi mili", "GPS on karein aur dobara try karein.");
                        return;
                    }
                    boolean mock = Build.VERSION.SDK_INT >= 31 ? loc.isMock() : loc.isFromMockProvider();
                    if (mock) {
                        showResult(false, "Fake GPS detect hua", "Mock location band karke dobara try karein.");
                        return;
                    }
                    float[] res = new float[1];
                    Location.distanceBetween(loc.getLatitude(), loc.getLongitude(), sLat, sLng, res);
                    if (res[0] > MAX_DISTANCE_M) {
                        showResult(false, "Aap class se door hain",
                                Math.round(res[0]) + " m door. Limit " + (int) MAX_DISTANCE_M + " m hai.");
                        return;
                    }
                    saveAttendance(res[0]);
                })
                .addOnFailureListener(e -> showResult(false, "Location error", e.getMessage()));
    }

    private void saveAttendance(float distance) {
        Map<String, Object> m = new HashMap<>();
        m.put("sessionId", sessionId);
        m.put("courseId", courseId);
        m.put("studentId", uid);
        m.put("timestamp", System.currentTimeMillis());
        m.put("distance", (double) distance);

        db.collection("attendance").document(sessionId + "_" + uid).set(m)
                .addOnSuccessListener(x -> showResult(true, "Attendance marked",
                        "Aap class se " + Math.round(distance) + " m door the."))
                .addOnFailureListener(e -> showResult(false, "Save nahi hui", e.getMessage()));
    }

    private void loading(String msg) {
        progress.setVisibility(View.VISIBLE);
        tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        tvTitle.setText(msg);
        tvMsg.setText("");
        btnDone.setVisibility(View.GONE);
    }

    private void showResult(boolean ok, String title, String msg) {
        progress.setVisibility(View.GONE);
        tvTitle.setText(title);
        tvTitle.setTextColor(ContextCompat.getColor(this, ok ? R.color.success : R.color.danger));
        tvMsg.setText(msg);
        btnDone.setVisibility(View.VISIBLE);
    }
}
