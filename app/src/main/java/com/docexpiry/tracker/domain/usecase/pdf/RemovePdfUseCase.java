package com.docexpiry.tracker.domain.usecase.pdf;

import com.docexpiry.tracker.domain.repository.ImageRepository;

public class RemovePdfUseCase {
    private final ImageRepository repository;

    public RemovePdfUseCase(ImageRepository repository) {
        this.repository = repository;
    }

    public void execute(String uid, String docId, ImageRepository.Callback callback) {
        repository.removePdf(uid, docId, callback);
    }
}
