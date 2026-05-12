package com.docexpiry.tracker.presentation.documents;

import com.docexpiry.tracker.domain.model.Document;
import java.util.ArrayList;
import java.util.List;

public class DocumentsUiState {
    public final boolean loading;
    public final List<Document> documents;
    public final String error;

    public DocumentsUiState(boolean loading, List<Document> documents, String error) {
        this.loading = loading;
        this.documents = documents != null ? documents : new ArrayList<>();
        this.error = error;
    }

    public static DocumentsUiState idle() {
        return new DocumentsUiState(false, new ArrayList<>(), null);
    }
}