package com.docexpiry.tracker.domain.service;

import com.docexpiry.tracker.domain.model.Document;

public interface NotificationService {
    void scheduleForDocument(Document document);
    void cancelForDocument(String docId);
}
