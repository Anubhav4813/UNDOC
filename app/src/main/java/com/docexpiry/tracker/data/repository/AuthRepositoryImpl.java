package com.docexpiry.tracker.data.repository;

import com.docexpiry.tracker.data.remote.auth.AuthRemoteDataSource;
import com.docexpiry.tracker.domain.repository.AuthRepository;

public class AuthRepositoryImpl implements AuthRepository {
    private final AuthRemoteDataSource remote;

    public AuthRepositoryImpl(AuthRemoteDataSource remote) {
        this.remote = remote;
    }

    @Override
    public boolean isSignedIn() {
        return remote.isSignedIn();
    }

    @Override
    public String currentUid() {
        return remote.currentUid();
    }

    @Override
    public String currentEmail() {
        return remote.currentEmail();
    }

    @Override
    public void signIn(String email, String password, AuthCallback callback) {
        remote.signIn(email, password, callback::onComplete);
    }

    @Override
    public void signUp(String email, String password, AuthCallback callback) {
        remote.signUp(email, password, callback::onComplete);
    }

    @Override
    public void sendPasswordReset(String email, AuthCallback callback) {
        remote.sendPasswordReset(email, callback::onComplete);
    }

    @Override
    public void signOut() {
        remote.signOut();
    }
}