package com.docexpiry.tracker.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.docexpiry.tracker.core.common.Result;
import com.docexpiry.tracker.data.local.dao.DocumentDao;
import com.docexpiry.tracker.data.local.entity.DocumentEntity;
import com.docexpiry.tracker.data.mapper.DocumentMapper;
import com.docexpiry.tracker.data.remote.firestore.DocumentRemoteDataSource;
import com.docexpiry.tracker.domain.model.Document;
import com.docexpiry.tracker.domain.repository.DocumentRepository;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.content.Context;
import com.docexpiry.tracker.domain.service.NotificationService;

public class DocumentRepositoryImpl implements DocumentRepository {
    private final DocumentDao dao;
    private final DocumentMapper mapper;
    private final DocumentRemoteDataSource remote;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private volatile boolean initialCloudSyncDone = false;
    private final NotificationService notificationService;

    public DocumentRepositoryImpl(
            DocumentDao dao,
            DocumentMapper mapper,
            DocumentRemoteDataSource remote,
            NotificationService notificationService) {
        this.notificationService = notificationService;
        this.dao = dao;
        this.mapper = mapper;
        this.remote = remote;
    }

    @Override
    public LiveData<Result<List<Document>>> observeDocuments(String uid) {
        MediatorLiveData<Result<List<Document>>> liveData = new MediatorLiveData<>();
        liveData.setValue(new Result.Loading<>());

        if (!initialCloudSyncDone) {
            initialCloudSyncDone = true;
            remote.fetchAll(uid, (cloudDocs, error) -> {
                if (error == null && cloudDocs != null) {
                    io.execute(() -> {
                        for (DocumentEntity cloudDoc : cloudDocs) {
                            DocumentEntity local = dao.getById(uid, cloudDoc.id);
                            if (local != null) {
                                // Keep local imagePath if cloud version doesn't have one
                                if ((cloudDoc.imagePath == null || cloudDoc.imagePath.trim().isEmpty())
                                        && (local.imagePath != null && !local.imagePath.trim().isEmpty())) {
                                    cloudDoc.imagePath = local.imagePath;
                                }
                                // Only update if cloud is newer or equal (to allow initial sync)
                                if (cloudDoc.updatedAt >= local.updatedAt) {
                                    dao.insert(cloudDoc);
                                }
                            } else {
                                dao.insert(cloudDoc);
                            }
                        }
                    });
                }
            });
        }

        LiveData<List<DocumentEntity>> source = dao.observeByUser(uid);
        liveData.addSource(source, entities -> liveData.setValue(new Result.Success<>(mapper.toDomainList(entities))));
        return liveData;
    }

    @Override
    public void addDocument(Document document, Callback callback) {
        io.execute(() -> {
            try {
                DocumentEntity entity = mapper.toEntity(document);
                dao.insert(entity);

                // schedule expiry notifications for this document
                notificationService.scheduleForDocument(document);

                remote.upsert(document.getUid(), entity, error -> {
                    if (error == null)
                        callback.onComplete(new Result.Success<>(null));
                    else
                        callback.onComplete(new Result.Error<>(error));
                });
            } catch (Exception e) {
                callback.onComplete(new Result.Error<>(e.getMessage()));
            }
        });
    }

    @Override
    public void deleteDocument(String uid, String docId, Callback callback) {
        io.execute(() -> {
            try {
                dao.markDeleted(uid, docId, System.currentTimeMillis());

                notificationService.cancelForDocument(docId);

                remote.delete(uid, docId, error -> {
                    if (error == null)
                        callback.onComplete(new Result.Success<>(null));
                    else
                        callback.onComplete(new Result.Error<>(error));
                });
            } catch (Exception e) {
                callback.onComplete(new Result.Error<>(e.getMessage()));
            }
        });
    }
}