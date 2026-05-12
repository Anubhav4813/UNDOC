package com.docexpiry.tracker.presentation.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.docexpiry.tracker.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private MaterialButton btnSend;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.et_email);
        btnSend = findViewById(R.id.btn_send_link);
        progressBar = findViewById(R.id.progress_bar);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.tv_login_back).setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> handleReset());
    }

    private void handleReset() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }

        showLoading(true);

        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                Toast.makeText(ForgotPasswordActivity.this, 
                    "Reset link sent to your email!", Toast.LENGTH_LONG).show();
                finish();
            } else {
                String error;
                if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                    error = "This email is not registered.";
                } else {
                    error = "Failed to send reset email. Please check your connection.";
                }
                Toast.makeText(ForgotPasswordActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSend.setEnabled(!loading);
        etEmail.setEnabled(!loading);
    }
}