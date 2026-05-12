package com.docexpiry.tracker.presentation.auth;

public class LoginUiState {
    public final boolean loading;
    public final boolean signedIn;
    public final String error;

    public LoginUiState(boolean loading, boolean signedIn, String error) {
        this.loading = loading;
        this.signedIn = signedIn;
        this.error = error;
    }

    public static LoginUiState idle() {
        return new LoginUiState(false, false, null);
    }
}