package com.docexpiry.tracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private boolean isSignUp = false;
    private FirebaseManager firebase;

    private TextView          tabLogin, tabSignup, tvForgot, tvError;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton    btnAuth;
    private ProgressBar       progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebase = new FirebaseManager();

        // If already signed in, skip straight to main screen
        if (firebase.isSignedIn()) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        tabLogin  = findViewById(R.id.tab_login);
        tabSignup = findViewById(R.id.tab_signup);
        etEmail   = findViewById(R.id.et_email);
        etPassword= findViewById(R.id.et_password);
        btnAuth   = findViewById(R.id.btn_auth);
        progress  = findViewById(R.id.progress);
        tvForgot  = findViewById(R.id.tv_forgot);
        tvError   = findViewById(R.id.tv_error);

        tabLogin.setOnClickListener(v  -> switchTab(false));
        tabSignup.setOnClickListener(v -> switchTab(true));
        btnAuth.setOnClickListener(v -> attempt());

        tvForgot.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            if (email.isEmpty()) { showError("Enter your email address first."); return; }
            setLoading(true);
            firebase.sendPasswordReset(email, (ok, err) -> runOnUiThread(() -> {
                setLoading(false);
                if (ok) {
                    new MaterialAlertDialogBuilder(this, R.style.DialogTheme)
                        .setTitle("Email Sent")
                        .setMessage("A password reset link has been sent to " + email)
                        .setPositiveButton("OK", null)
                        .show();
                } else {
                    showError(err);
                }
            }));
        });

        // Hide Google Sign-In button
        View btnGoogle = findViewById(R.id.btn_google);
        if (btnGoogle != null) btnGoogle.setVisibility(View.GONE);
    }

    private void switchTab(boolean signup) {
        isSignUp = signup;
        if (signup) {
            tabSignup.setTextColor(0xFF2563EB);
            tabSignup.setTextSize(14);
            tabLogin.setTextColor(0xFF64748B);
            btnAuth.setText("Create Account");
            tvForgot.setVisibility(View.GONE);
        } else {
            tabLogin.setTextColor(0xFF2563EB);
            tabLogin.setTextSize(14);
            tabSignup.setTextColor(0xFF64748B);
            btnAuth.setText("Sign In");
            tvForgot.setVisibility(View.VISIBLE);
        }
        tvError.setVisibility(View.GONE);
    }

    private void attempt() {
        String email    = etEmail.getText()    != null ? etEmail.getText().toString().trim()    : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (email.isEmpty())    { showError("Please enter your email."); return; }
        if (password.isEmpty()) { showError("Please enter your password."); return; }
        if (password.length() < 6) { showError("Password must be at least 6 characters."); return; }

        setLoading(true);

        FirebaseManager.AuthCallback cb = (ok, err) -> runOnUiThread(() -> {
            setLoading(false);
            if (ok) goToMain();
            else    showError(err);
        });

        if (isSignUp) firebase.signUp(email, password, cb);
        else          firebase.signIn(email, password, cb);
    }

    private void setLoading(boolean loading) {
        btnAuth.setEnabled(!loading);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        tvError.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
