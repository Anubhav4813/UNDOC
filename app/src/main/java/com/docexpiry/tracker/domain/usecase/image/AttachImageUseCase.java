package com.docexpiry.tracker.domain.usecase.image;

import android.net.Uri;
import com.docexpiry.tracker.domain.repository.ImageRepository;

public class AttachImageUseCase {
    private final ImageRepository repository;

    public AttachImageUseCase(ImageRepository repository) {
        this.repository = repository;
    }

    public void execute(String uid, String docId, Uri uri, ImageRepository.Callback callback) {
        repository.attachLocalImage(uid, docId, uri, callback);
    }
}