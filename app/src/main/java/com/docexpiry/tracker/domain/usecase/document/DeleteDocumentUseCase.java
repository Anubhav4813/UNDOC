package com.docexpiry.tracker.domain.usecase.document;

import com.docexpiry.tracker.domain.repository.DocumentRepository;

public class DeleteDocumentUseCase {
    private final DocumentRepository repository;

    public DeleteDocumentUseCase(DocumentRepository repository) {
        this.repository = repository;
    }

    public void execute(String uid, String docId, DocumentRepository.Callback callback) {
        repository.deleteDocument(uid, docId, callback);
    }
}