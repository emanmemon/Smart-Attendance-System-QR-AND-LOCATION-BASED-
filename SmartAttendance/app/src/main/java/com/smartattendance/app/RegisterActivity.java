package com.smartattendance.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    TextInputLayout tilName, tilEmail, tilPassword;
    MaterialButtonToggleGroup toggleRole;
    MaterialButton btnRegister;
    ProgressBar progress;
    TextView tvGoLogin;
    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        toggleRole = findViewById(R.id.toggleRole);
        btnRegister = findViewById(R.id.btnRegister);
        progress = findViewById(R.id.progress);
        tvGoLogin = findViewById(R.id.tvGoLogin);

        btnRegister.setOnClickListener(v -> registerUser());
        tvGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private String text(TextInputLayout til) {
        return til.getEditText().getText().toString().trim();
    }

    private boolean validate(String name, String email, String password) {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        boolean ok = true;

        if (name.isEmpty()) {
            tilName.setError("Naam likhein");
            ok = false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Sahi email dalein");
            ok = false;
        }
        if (password.length() < 6) {
            tilPassword.setError("Kam az kam 6 characters");
            ok = false;
        }
        return ok;
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }

    private void registerUser() {
        String name = text(tilName);
        String email = text(tilEmail);
        String password = text(tilPassword);
        String role = toggleRole.getCheckedButtonId() == R.id.btnTeacher ? "teacher" : "student";

        if (!validate(name, email, password)) return;

        setLoading(true);

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(result -> {
                String uid = result.getUser().getUid();

                Map<String, Object> user = new HashMap<>();
                user.put("name", name);
                user.put("email", email);
                user.put("role", role);

                db.collection("users").document(uid).set(user)
                    .addOnSuccessListener(unused -> RoleRouter.goToDashboard(this, role))
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            })
            .addOnFailureListener(e -> {
                setLoading(false);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
}
