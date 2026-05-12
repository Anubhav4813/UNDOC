package com.docexpiry.tracker.domain.usecase.document;

import androidx.lifecycle.LiveData;
import com.docexpiry.tracker.core.common.Result;
import com.docexpiry.tracker.domain.model.Document;
import com.docexpiry.tracker.domain.repository.DocumentRepository;
import java.util.List;

public class ObserveDocumentsUseCase {
    private final DocumentRepository repository;

    public ObserveDocumentsUseCase(DocumentRepository repository) {
        this.repository = repository;
    }

    public LiveData<Result<List<Document>>> execute(String uid) {
        return repository.observeDocuments(uid);
    }
}