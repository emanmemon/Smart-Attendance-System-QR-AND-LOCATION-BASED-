package com.smartattendance.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QrSessionActivity extends AppCompatActivity {

    static final int REFRESH_SECONDS = 30;

    FirebaseFirestore db;
    String courseId, courseName, sessionId, token = "", prevToken = "";
    int secondsLeft = REFRESH_SECONDS, present = 0, totalStudents = 0;
    boolean ended = false;
    ImageView ivQr;
    TextView tvStatus, tvTimer, tvCount;
    Handler handler = new Handler(Looper.getMainLooper());
    ListenerRegistration countListener;

    ActivityResultLauncher<String> permLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startWithLocation();
                else {
                    Toast.makeText(this, "Location permission zaroori hai", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    Runnable tick = new Runnable() {
        @Override
        public void run() {
            secondsLeft--;
            if (secondsLeft <= 0) {
                refreshToken();
                secondsLeft = REFRESH_SECONDS;
            }
            tvTimer.setText("New code in " + secondsLeft + "s");
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_session);

        db = FirebaseFirestore.getInstance();
        courseId = getIntent().getStringExtra("courseId");
        courseName = getIntent().getStringExtra("courseName");

        ivQr = findViewById(R.id.ivQr);
        tvStatus = findViewById(R.id.tvStatus);
        tvTimer = findViewById(R.id.tvTimer);
        tvCount = findViewById(R.id.tvCount);
        ((TextView) findViewById(R.id.tvCourse)).setText(courseName);
        findViewById(R.id.btnEnd).setOnClickListener(v -> endSession());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startWithLocation();
        } else {
            permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void startWithLocation() {
        tvStatus.setText("Location le raha hoon…");
        LocationServices.getFusedLocationProviderClient(this)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(loc -> {
                    if (loc == null) {
                        Toast.makeText(this, "GPS on karke dobara try karein", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    createSession(loc.getLatitude(), loc.getLongitude());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void createSession(double lat, double lng) {
        token = Utils.randomCode(12);
        Map<String, Object> m = new HashMap<>();
        m.put("courseId", courseId);
        m.put("courseName", courseName);
        m.put("teacherId", FirebaseAuth.getInstance().getUid());
        m.put("lat", lat);
        m.put("lng", lng);
        m.put("startTime", System.currentTimeMillis());
        m.put("active", true);
        m.put("token", token);
        m.put("prevToken", "");

        db.collection("sessions").add(m).addOnSuccessListener(ref -> {
            sessionId = ref.getId();
            tvStatus.setText("Students ko ye QR scan karwayein");
            showQr();
            handler.postDelayed(tick, 1000);
            listenCount();
            loadTotal();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void showQr() {
        try {
            Bitmap bmp = new BarcodeEncoder().encodeBitmap(sessionId + "|" + token, BarcodeFormat.QR_CODE, 800, 800);
            ivQr.setImageBitmap(bmp);
        } catch (Exception e) {
            Toast.makeText(this, "QR error", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshToken() {
        prevToken = token;
        token = Utils.randomCode(12);
        db.collection("sessions").document(sessionId).update("token", token, "prevToken", prevToken);
        showQr();
    }

    private void listenCount() {
        countListener = db.collection("attendance").whereEqualTo("sessionId", sessionId)
                .addSnapshotListener((snap, e) -> {
                    if (snap != null) {
                        present = snap.size();
                        updateCount();
                    }
                });
    }

    private void loadTotal() {
        db.collection("courses").document(courseId).get().addOnSuccessListener(d -> {
            List<?> ids = (List<?>) d.get("studentIds");
            totalStudents = ids == null ? 0 : ids.size();
            updateCount();
        });
    }

    private void updateCount() {
        tvCount.setText("Present: " + present + " / " + totalStudents);
    }

    private void endSession() {
        handler.removeCallbacks(tick);
        if (sessionId == null) {
            finish();
            return;
        }
        db.collection("sessions").document(sessionId)
                .update("active", false, "endTime", System.currentTimeMillis())
                .addOnCompleteListener(t -> {
                    ended = true;
                    finish();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(tick);
        if (countListener != null) countListener.remove();
        if (isFinishing() && !ended && sessionId != null) {
            db.collection("sessions").document(sessionId).update("active", false);
        }
    }
}
