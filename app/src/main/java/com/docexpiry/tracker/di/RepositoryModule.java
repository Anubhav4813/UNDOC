// di/RepositoryModule.java
package com.docexpiry.tracker.di;

import android.content.Context;
import com.docexpiry.tracker.data.local.dao.DocumentDao;
import com.docexpiry.tracker.data.local.db.UndocDatabase;
import com.docexpiry.tracker.data.mapper.DocumentMapper;
import com.docexpiry.tracker.data.remote.firestore.DocumentRemoteDataSource;
import com.docexpiry.tracker.data.repository.DocumentRepositoryImpl;
import com.docexpiry.tracker.data.worker.NotificationServiceImpl;
import com.docexpiry.tracker.domain.repository.DocumentRepository;
import com.docexpiry.tracker.domain.service.NotificationService;
import com.docexpiry.tracker.domain.usecase.document.DeleteDocumentUseCase;
import com.docexpiry.tracker.domain.usecase.document.ObserveDocumentsUseCase;

public class RepositoryModule {
    private final DocumentRepository documentRepository;

    private RepositoryModule(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public static RepositoryModule build(Context context) {
        UndocDatabase db = UndocDatabase.getInstance(context);
        DocumentDao dao = db.documentDao();
        DocumentMapper mapper = new DocumentMapper();
        DocumentRemoteDataSource remote = new DocumentRemoteDataSource();
        NotificationService notificationService = new NotificationServiceImpl(context);
        DocumentRepository repository = new DocumentRepositoryImpl(dao, mapper, remote, notificationService);
        return new RepositoryModule(repository);
    }

    public ObserveDocumentsUseCase provideObserveDocumentsUseCase() {
        return new ObserveDocumentsUseCase(documentRepository);
    }

    public DeleteDocumentUseCase provideDeleteDocumentUseCase() {
        return new DeleteDocumentUseCase(documentRepository);
    }
}