package com.docexpiry.tracker.data.remote.auth;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthRemoteDataSource {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public boolean isSignedIn() {
        return auth.getCurrentUser() != null;
    }

    public String currentUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public String currentEmail() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    public void signIn(String email, String password, Callback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onComplete(true, null))
                .addOnFailureListener(error -> callback.onComplete(false, friendlyError(error.getMessage())));
    }

    public void signUp(String email, String password, Callback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onComplete(true, null))
                .addOnFailureListener(error -> callback.onComplete(false, friendlyError(error.getMessage())));
    }

    public void sendPasswordReset(String email, Callback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(result -> callback.onComplete(true, null))
                .addOnFailureListener(error -> callback.onComplete(false, friendlyError(error.getMessage())));
    }

    public void signOut() {
        auth.signOut();
    }

    public interface Callback {
        void onComplete(boolean success, String error);
    }

    private String friendlyError(String raw) {
        if (raw == null) return "Something went wrong.";
        if (raw.contains("email address is already in use")) return "An account with this email already exists.";
        if (raw.contains("INVALID_LOGIN_CREDENTIALS") || raw.contains("no user record")) return "Email or password is incorrect.";
        if (raw.contains("badly formatted")) return "Please enter a valid email address.";
        if (raw.contains("password is invalid") || raw.contains("at least 6")) return "Password must be at least 6 characters.";
        if (raw.contains("network")) return "No internet connection. Please check your network.";
        return raw;
    }
}