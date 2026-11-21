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
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    TextInputLayout tilEmail, tilPassword;
    MaterialButton btnLogin;
    ProgressBar progress;
    TextView tvGoRegister;
    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progress = findViewById(R.id.progress);
        tvGoRegister = findViewById(R.id.tvGoRegister);

        btnLogin.setOnClickListener(v -> loginUser());
        tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private String text(TextInputLayout til) {
        return til.getEditText().getText().toString().trim();
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }

    private void loginUser() {
        String email = text(tilEmail);
        String password = text(tilPassword);

        tilEmail.setError(null);
        tilPassword.setError(null);

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Sahi email dalein");
            return;
        }
        if (password.isEmpty()) {
            tilPassword.setError("Password dalein");
            return;
        }

        setLoading(true);

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(result -> {
                String uid = result.getUser().getUid();
                db.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        String role = doc.getString("role");
                        if (role == null) {
                            setLoading(false);
                            Toast.makeText(this, "User ka role nahi mila", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        RoleRouter.goToDashboard(this, role);
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            })
            .addOnFailureListener(e -> {
                setLoading(false);
                Toast.makeText(this, "Login fail: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
}
