package com.docexpiry.tracker.domain.usecase.auth;

import com.docexpiry.tracker.domain.repository.AuthRepository;

public class ResetPasswordUseCase {
    private final AuthRepository repository;

    public ResetPasswordUseCase(AuthRepository repository) {
        this.repository = repository;
    }

    public void execute(String email, AuthRepository.AuthCallback callback) {
        repository.sendPasswordReset(email, callback);
    }
}