package com.docexpiry.tracker.domain.usecase.document;

import com.docexpiry.tracker.core.common.Result;
import com.docexpiry.tracker.domain.model.Document;
import com.docexpiry.tracker.domain.repository.DocumentRepository;

public class UpdateDocumentUseCase {
    private final DocumentRepository repository;

    public UpdateDocumentUseCase(DocumentRepository repository) {
        this.repository = repository;
    }

    public void execute(Document document, DocumentRepository.Callback callback) {
        repository.addDocument(document, callback);
    }
}