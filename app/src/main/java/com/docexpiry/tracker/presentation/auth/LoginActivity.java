package com.docexpiry.tracker.presentation.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.docexpiry.tracker.R;
import com.docexpiry.tracker.data.remote.auth.AuthRemoteDataSource;
import com.docexpiry.tracker.data.repository.AuthRepositoryImpl;
import com.docexpiry.tracker.domain.repository.AuthRepository;
import com.docexpiry.tracker.domain.usecase.auth.ResetPasswordUseCase;
import com.docexpiry.tracker.domain.usecase.auth.SignInUseCase;
import com.docexpiry.tracker.domain.usecase.auth.SignUpUseCase;
import com.docexpiry.tracker.presentation.documents.DocumentsActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {
    private boolean isSignUp = false;
    private AuthViewModel viewModel;

    private TextView tabLogin;
    private TextView tabSignup;
    private TextView tvForgot;
    private TextView tvError;
    private ImageView ivLogo;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private MaterialButton btnAuth;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AuthRepository repository = new AuthRepositoryImpl(new AuthRemoteDataSource());

        AuthViewModelFactory factory = new AuthViewModelFactory(
                new SignInUseCase(repository),
                new SignUpUseCase(repository),
                new ResetPasswordUseCase(repository),
                repository);

        viewModel = new ViewModelProvider(this, factory).get(AuthViewModel.class);

        if (viewModel.isAlreadySignedIn()) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        tabLogin = findViewById(R.id.tab_login);
        tabSignup = findViewById(R.id.tab_signup);
        ivLogo = findViewById(R.id.iv_logo);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnAuth = findViewById(R.id.btn_auth);
        progress = findViewById(R.id.progress);
        tvForgot = findViewById(R.id.tv_forgot);
        tvError = findViewById(R.id.tv_error);

        tabLogin.setOnClickListener(v -> switchTab(false));
        tabSignup.setOnClickListener(v -> switchTab(true));
        btnAuth.setOnClickListener(v -> attempt());

        tvForgot.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        View btnGoogle = findViewById(R.id.btn_google);
        if (btnGoogle != null)
            btnGoogle.setVisibility(View.GONE);

        viewModel.uiState().observe(this, state -> {
            setLoading(state.loading);
            if (state.signedIn) {
                goToMain();
            } else if (state.error != null) {
                showError(state.error);
            }
        });
    }

    private void switchTab(boolean signup) {
        isSignUp = signup;
        if (signup) {
            tabSignup.setTextColor(0xFF2563EB);
            tabLogin.setTextColor(0xFF64748B);
            btnAuth.setText("Create Account");
            tvForgot.setVisibility(View.GONE);
        } else {
            tabLogin.setTextColor(0xFF2563EB);
            tabSignup.setTextColor(0xFF64748B);
            btnAuth.setText("Sign In");
            tvForgot.setVisibility(View.VISIBLE);
        }
        tvError.setVisibility(View.GONE);
    }

    private void attempt() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (email.isEmpty()) {
            showError("Please enter your email.");
            return;
        }
        if (password.isEmpty()) {
            showError("Please enter your password.");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }

        setLoading(true);

        if (isSignUp) {
            viewModel.signUp(email, password);
        } else {
            viewModel.signIn(email, password);
        }
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
        startActivity(new Intent(this, DocumentsActivity.class));
        finish();
    }
}