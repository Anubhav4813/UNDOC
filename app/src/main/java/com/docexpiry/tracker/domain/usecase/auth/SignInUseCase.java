package com.docexpiry.tracker.domain.usecase.auth;

import com.docexpiry.tracker.domain.repository.AuthRepository;

public class SignInUseCase {
    private final AuthRepository repository;

    public SignInUseCase(AuthRepository repository) {
        this.repository = repository;
    }

    public void execute(String email, String password, AuthRepository.AuthCallback callback) {
        repository.signIn(email, password, callback);
    }
}