package com.smartattendance.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                toLogin();
                return;
            }
            FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        String role = doc.getString("role");
                        if (role == null) toLogin();
                        else RoleRouter.goToDashboard(this, role);
                    })
                    .addOnFailureListener(e -> toLogin());
        }, 1200);
    }

    private void toLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
