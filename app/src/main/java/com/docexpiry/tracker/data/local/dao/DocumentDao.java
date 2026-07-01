package com.docexpiry.tracker.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.docexpiry.tracker.data.local.entity.DocumentEntity;
import java.util.List;

@Dao
public interface DocumentDao {
    @Query("SELECT * FROM documents WHERE uid = :uid AND deleted = 0 ORDER BY expiryDateMillis ASC")
    LiveData<List<DocumentEntity>> observeByUser(String uid);

    @Query("SELECT * FROM documents WHERE uid = :uid AND id = :id")
    DocumentEntity getById(String uid, String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DocumentEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DocumentEntity> entities);

    @Update
    void update(DocumentEntity entity);

    @Query("UPDATE documents SET deleted = 1, dirty = 1, updatedAt = :updatedAt WHERE uid = :uid AND id = :docId")
    void markDeleted(String uid, String docId, long updatedAt);

    @Query("SELECT * FROM documents WHERE uid = :uid AND dirty = 1")
    List<DocumentEntity> getDirty(String uid);

    @Query("UPDATE documents SET dirty = 0 WHERE uid = :uid AND id IN (:ids)")
    void clearDirty(String uid, List<String> ids);

    @Query("UPDATE documents SET imagePath = :imagePath, dirty = 1, updatedAt = :updatedAt WHERE uid = :uid AND id = :docId")
    void updateImagePath(String uid, String docId, String imagePath, long updatedAt);

    @Query("UPDATE documents SET imagePath = NULL, dirty = 1, updatedAt = :updatedAt WHERE uid = :uid AND id = :docId")
    void clearImagePath(String uid, String docId, long updatedAt);
}