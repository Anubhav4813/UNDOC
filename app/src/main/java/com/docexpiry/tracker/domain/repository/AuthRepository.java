package com.docexpiry.tracker.domain.repository;

public interface AuthRepository {
    interface AuthCallback {
        void onComplete(boolean success, String error);
    }

    boolean isSignedIn();
    String currentUid();
    String currentEmail();

    void signIn(String email, String password, AuthCallback callback);
    void signUp(String email, String password, AuthCallback callback);
    void sendPasswordReset(String email, AuthCallback callback);
    void signOut();
}