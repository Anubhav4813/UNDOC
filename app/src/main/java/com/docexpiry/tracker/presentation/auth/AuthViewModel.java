package com.docexpiry.tracker.presentation.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.docexpiry.tracker.domain.repository.AuthRepository;
import com.docexpiry.tracker.domain.usecase.auth.ResetPasswordUseCase;
import com.docexpiry.tracker.domain.usecase.auth.SignInUseCase;
import com.docexpiry.tracker.domain.usecase.auth.SignUpUseCase;

public class AuthViewModel extends ViewModel {
    private final SignInUseCase signInUseCase;
    private final SignUpUseCase signUpUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final AuthRepository authRepository;

    private final MutableLiveData<LoginUiState> uiState = new MutableLiveData<>(LoginUiState.idle());

    public AuthViewModel(
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

    public LiveData<LoginUiState> uiState() {
        return uiState;
    }

    public boolean isAlreadySignedIn() {
        return authRepository.isSignedIn();
    }

    public void signIn(String email, String password) {
        uiState.setValue(new LoginUiState(true, false, null));
        signInUseCase.execute(email, password, (success, error) ->
                uiState.postValue(new LoginUiState(false, success, error)));
    }

    public void signUp(String email, String password) {
        uiState.setValue(new LoginUiState(true, false, null));
        signUpUseCase.execute(email, password, (success, error) ->
                uiState.postValue(new LoginUiState(false, success, error)));
    }

    public void resetPassword(String email) {
        uiState.setValue(new LoginUiState(true, false, null));
        resetPasswordUseCase.execute(email, (success, error) ->
                uiState.postValue(new LoginUiState(false, false, error)));
    }
}