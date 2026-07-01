package com.docexpiry.tracker.presentation.documents;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.docexpiry.tracker.core.common.Result;
import com.docexpiry.tracker.domain.model.Document;
import com.docexpiry.tracker.domain.repository.AuthRepository;
import com.docexpiry.tracker.domain.repository.ImageRepository;
import com.docexpiry.tracker.domain.usecase.document.AddDocumentUseCase;
import com.docexpiry.tracker.domain.usecase.document.DeleteDocumentUseCase;
import com.docexpiry.tracker.domain.usecase.document.ObserveDocumentsUseCase;
import com.docexpiry.tracker.domain.usecase.image.AttachImageUseCase;
import com.docexpiry.tracker.domain.usecase.image.RemoveImageUseCase;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("unchecked")
public class DocumentsViewModel extends ViewModel {
    private final ObserveDocumentsUseCase observeUseCase;
    private final AddDocumentUseCase addUseCase;
    private final DeleteDocumentUseCase deleteUseCase;
    private final AttachImageUseCase attachImageUseCase;
    private final RemoveImageUseCase removeImageUseCase;
    private final AuthRepository authRepository;

    private final MutableLiveData<DocumentsUiState> uiState = new MutableLiveData<>(DocumentsUiState.idle());

    private static final long DAYS_MS = 24L * 60 * 60 * 1000;
    private static final int EXPIRING_SOON_DAYS = 7;

    private LiveData<List<DocumentListItem>> groupedDocumentsLiveData;

    public DocumentsViewModel(
            ObserveDocumentsUseCase observeUseCase,
            AddDocumentUseCase addUseCase,
            DeleteDocumentUseCase deleteUseCase,
            AttachImageUseCase attachImageUseCase,
            RemoveImageUseCase removeImageUseCase,
            AuthRepository authRepository) {
        this.observeUseCase = observeUseCase;
        this.addUseCase = addUseCase;
        this.deleteUseCase = deleteUseCase;
        this.attachImageUseCase = attachImageUseCase;
        this.removeImageUseCase = removeImageUseCase;
        this.authRepository = authRepository;
    }

    public LiveData<DocumentsUiState> uiState() {
        return uiState;
    }

    public void start() {
        String uid = authRepository.currentUid();
        if (uid == null) {
            uiState.setValue(new DocumentsUiState(false, new ArrayList<>(), "Not signed in"));
            return;
        }

        setupGroupedStream(uid);
    }

    private void setupGroupedStream(String uid) {
        LiveData<Result<List<Document>>> source = observeUseCase.execute(uid);
        
        // Update uiState from the same source
        source.observeForever(result -> {
            if (result instanceof Result.Loading) {
                uiState.postValue(new DocumentsUiState(true, new ArrayList<>(), null));
            } else if (result instanceof Result.Success) {
                Result.Success<?> success = (Result.Success<?>) result;
                uiState.postValue(new DocumentsUiState(false, (List<Document>) success.data, null));
            } else if (result instanceof Result.Error) {
                Result.Error<?> error = (Result.Error<?>) result;
                uiState.postValue(new DocumentsUiState(false, new ArrayList<>(), error.message));
            }
        });

        groupedDocumentsLiveData = Transformations.map(source, result -> {
            if (result instanceof Result.Success) {
                List<Document> docs = (List<Document>) ((Result.Success<?>) result).data;
                return transformToSections(docs);
            } else {
                return new ArrayList<>();
            }
        });
    }

    private List<DocumentListItem> transformToSections(List<Document> docs) {
        List<DocumentListItem> out = new ArrayList<>();
        if (docs == null || docs.isEmpty())
            return out;

        long now = System.currentTimeMillis();
        List<Document> expired = new ArrayList<>();
        List<Document> expiringSoon = new ArrayList<>();
        List<Document> valid = new ArrayList<>();

        for (Document d : docs) {
            long diff = d.getExpiryDateMillis() - now;
            if (diff <= 0)
                expired.add(d);
            else if (diff <= EXPIRING_SOON_DAYS * DAYS_MS)
                expiringSoon.add(d);
            else
                valid.add(d);
        }

        if (!expired.isEmpty()) {
            out.add(DocumentListItem.header("Expired (" + expired.size() + ")"));
            expired.sort(Comparator.comparingLong(Document::getExpiryDateMillis));
            for (Document d : expired)
                out.add(DocumentListItem.document(d));
        }
        if (!expiringSoon.isEmpty()) {
            out.add(DocumentListItem.header("Expiring Soon (" + expiringSoon.size() + ")"));
            expiringSoon.sort(Comparator.comparingLong(Document::getExpiryDateMillis));
            for (Document d : expiringSoon)
                out.add(DocumentListItem.document(d));
        }
        if (!valid.isEmpty()) {
            out.add(DocumentListItem.header("Valid (" + valid.size() + ")"));
            valid.sort(Comparator.comparingLong(Document::getExpiryDateMillis));
            for (Document d : valid)
                out.add(DocumentListItem.document(d));
        }
        return out;
    }

    public LiveData<List<DocumentListItem>> getGroupedDocuments() {
        return groupedDocumentsLiveData;
    }

    public void add(Document document) {
        addUseCase.execute(document, result -> {
            if (result instanceof Result.Error) {
                Result.Error<?> error = (Result.Error<?>) result;
                DocumentsUiState current = uiState.getValue();
                if (current == null)
                    current = DocumentsUiState.idle();
                uiState.postValue(new DocumentsUiState(false, current.documents, error.message));
            }
        });
    }

    public void delete(String docId) {
        String uid = authRepository.currentUid();
        if (uid == null)
            return;
        deleteUseCase.execute(uid, docId, result -> {
            if (result instanceof Result.Error) {
                Result.Error<?> error = (Result.Error<?>) result;
                DocumentsUiState current = uiState.getValue();
                if (current == null)
                    current = DocumentsUiState.idle();
                uiState.postValue(new DocumentsUiState(false, current.documents, error.message));
            }
        });
    }

    public void attachImage(String docId, Uri uri, ImageRepository.Callback callback) {
        String uid = authRepository.currentUid();
        if (uid == null)
            return;
        attachImageUseCase.execute(uid, docId, uri, callback);
    }

    public void removeImage(String docId, ImageRepository.Callback callback) {
        String uid = authRepository.currentUid();
        if (uid == null)
            return;
        removeImageUseCase.execute(uid, docId, callback);
    }
}