package com.docexpiry.tracker.presentation.documents;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.docexpiry.tracker.domain.repository.AuthRepository;
import com.docexpiry.tracker.domain.usecase.document.AddDocumentUseCase;
import com.docexpiry.tracker.domain.usecase.document.DeleteDocumentUseCase;
import com.docexpiry.tracker.domain.usecase.document.ObserveDocumentsUseCase;
import com.docexpiry.tracker.domain.usecase.image.AttachImageUseCase;
import com.docexpiry.tracker.domain.usecase.image.RemoveImageUseCase;
import com.docexpiry.tracker.domain.usecase.pdf.AttachPdfUseCase;
import com.docexpiry.tracker.domain.usecase.pdf.RemovePdfUseCase;

public class DocumentsViewModelFactory implements ViewModelProvider.Factory {
    private final ObserveDocumentsUseCase observeUseCase;
    private final AddDocumentUseCase addUseCase;
    private final DeleteDocumentUseCase deleteUseCase;
    private final AttachImageUseCase attachImageUseCase;
    private final RemoveImageUseCase removeImageUseCase;
    private final AttachPdfUseCase attachPdfUseCase;
    private final RemovePdfUseCase removePdfUseCase;
    private final AuthRepository authRepository;

    public DocumentsViewModelFactory(
            ObserveDocumentsUseCase observeUseCase,
            AddDocumentUseCase addUseCase,
            DeleteDocumentUseCase deleteUseCase,
            AttachImageUseCase attachImageUseCase,
            RemoveImageUseCase removeImageUseCase,
            AttachPdfUseCase attachPdfUseCase,
            RemovePdfUseCase removePdfUseCase,
            AuthRepository authRepository
    ) {
        this.observeUseCase = observeUseCase;
        this.addUseCase = addUseCase;
        this.deleteUseCase = deleteUseCase;
        this.attachImageUseCase = attachImageUseCase;
        this.removeImageUseCase = removeImageUseCase;
        this.attachPdfUseCase = attachPdfUseCase;
        this.removePdfUseCase = removePdfUseCase;
        this.authRepository = authRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new DocumentsViewModel(
                observeUseCase,
                addUseCase,
                deleteUseCase,
                attachImageUseCase,
                removeImageUseCase,
                attachPdfUseCase,
                removePdfUseCase,
                authRepository
        );
    }
}