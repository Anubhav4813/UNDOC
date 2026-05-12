package com.docexpiry.tracker.presentation.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.docexpiry.tracker.domain.repository.AuthRepository;
import com.docexpiry.tracker.domain.usecase.auth.ResetPasswordUseCase;
import com.docexpiry.tracker.domain.usecase.auth.SignInUseCase;
import com.docexpiry.tracker.domain.usecase.auth.SignUpUseCase;

public class AuthViewModelFactory implements ViewModelProvider.Factory {
    private final SignInUseCase signInUseCase;
    private final SignUpUseCase signUpUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final AuthRepository authRepository;

    public AuthViewModelFactory(
            SignInUseCase signInUseCase,
            SignUpUseCase signUpUseCase,
            ResetPasswordUseCase resetPasswordUseCase,
            AuthRepository authRepository
    ) {
        this.signInUseCase = signInUseCase;
        this.signUpUseCase = signUpUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.authRepository = authRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new AuthViewModel(signInUseCase, signUpUseCase, resetPasswordUseCase, authRepository);
    }
}