package com.docexpiry.tracker.domain.usecase.image;

import com.docexpiry.tracker.domain.repository.ImageRepository;

public class RemoveImageUseCase {
    private final ImageRepository repository;

    public RemoveImageUseCase(ImageRepository repository) {
        this.repository = repository;
    }

    public void execute(String uid, String docId, ImageRepository.Callback callback) {
        repository.removeImage(uid, docId, callback);
    }
}