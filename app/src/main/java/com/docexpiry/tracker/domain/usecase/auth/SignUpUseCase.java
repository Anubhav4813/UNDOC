package com.docexpiry.tracker.domain.usecase.auth;

import com.docexpiry.tracker.domain.repository.AuthRepository;

public class SignUpUseCase {
    private final AuthRepository repository;

    public SignUpUseCase(AuthRepository repository) {
        this.repository = repository;
    }

    public void execute(String email, String password, AuthRepository.AuthCallback callback) {
        repository.signUp(email, password, callback);
    }
}