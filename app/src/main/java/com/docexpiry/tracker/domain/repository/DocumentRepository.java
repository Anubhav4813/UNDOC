package com.docexpiry.tracker.domain.repository;

import androidx.lifecycle.LiveData;
import com.docexpiry.tracker.core.common.Result;
import com.docexpiry.tracker.domain.model.Document;
import java.util.List;

public interface DocumentRepository {
    LiveData<Result<List<Document>>> observeDocuments(String uid);
    void addDocument(Document document, Callback callback);
    void deleteDocument(String uid, String docId, Callback callback);

    interface Callback {
        void onComplete(Result<Void> result);
    }
}