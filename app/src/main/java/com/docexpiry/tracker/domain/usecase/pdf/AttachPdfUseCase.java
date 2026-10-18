package com.docexpiry.tracker.domain.usecase.pdf;

import android.net.Uri;
import com.docexpiry.tracker.domain.repository.ImageRepository;

public class AttachPdfUseCase {
    private final ImageRepository repository;

    public AttachPdfUseCase(ImageRepository repository) {
        this.repository = repository;
    }

    public void execute(String uid, String docId, Uri uri, ImageRepository.Callback callback) {
        repository.attachLocalPdf(uid, docId, uri, callback);
    }
}
