package com.docexpiry.tracker.data.remote.firestore;

import com.docexpiry.tracker.data.local.entity.DocumentEntity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentRemoteDataSource {
    private static final String COLLECTION = "documents";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback {
        void onDone(String error);
    }

    public interface FetchCallback {
        void onDone(List<DocumentEntity> documents, String error);
    }

    public void upsert(String uid, DocumentEntity entity, Callback callback) {
        db.collection("users")
                .document(uid)
                .collection(COLLECTION)
                .document(entity.id)
                .set(toMap(entity))
                .addOnSuccessListener(v -> callback.onDone(null))
                .addOnFailureListener(e -> callback.onDone(e.getMessage()));
    }

    public void delete(String uid, String docId, Callback callback) {
        db.collection("users")
                .document(uid)
                .collection(COLLECTION)
                .document(docId)
                .delete()
                .addOnSuccessListener(v -> callback.onDone(null))
                .addOnFailureListener(e -> callback.onDone(e.getMessage()));
    }

    public void fetchAll(String uid, FetchCallback callback) {
        db.collection("users")
                .document(uid)
                .collection(COLLECTION)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DocumentEntity> list = new ArrayList<>();
                    for (DocumentSnapshot ds : snapshot.getDocuments()) {
                        DocumentEntity entity = fromSnapshot(uid, ds);
                        if (entity != null) list.add(entity);
                    }
                    callback.onDone(list, null);
                })
                .addOnFailureListener(e -> callback.onDone(new ArrayList<>(), e.getMessage()));
    }

    private Map<String, Object> toMap(DocumentEntity entity) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", entity.id);
        map.put("uid", entity.uid);
        map.put("name", entity.name);
        map.put("category", entity.category);
        map.put("issueDateMillis", entity.issueDateMillis);
        map.put("expiryDateMillis", entity.expiryDateMillis);
        map.put("tag", entity.tag != null ? entity.tag : "");
        map.put("imagePath", entity.imagePath != null ? entity.imagePath : "");
        map.put("pdfPath", entity.pdfPath != null ? entity.pdfPath : "");
        map.put("updatedAt", entity.updatedAt);
        map.put("deleted", entity.deleted);
        return map;
    }

    private DocumentEntity fromSnapshot(String uid, DocumentSnapshot ds) {
        try {
            DocumentEntity entity = new DocumentEntity();

            entity.id = ds.contains("id")
                    ? String.valueOf(ds.get("id"))
                    : ds.getId();

            entity.uid = uid;
            entity.name = ds.getString("name");
            if (entity.name == null) entity.name = "Document";

            String category = ds.getString("category");
            entity.category = (category == null || category.trim().isEmpty()) ? "Other" : category;

            entity.issueDateMillis = getLong(ds, "issueDateMillis", getLong(ds, "issueDate", 0L));
            entity.expiryDateMillis = getLong(ds, "expiryDateMillis", getLong(ds, "expiryDate", 0L));

            String tag = ds.getString("tag");
            entity.tag = (tag == null || tag.trim().isEmpty()) ? null : tag;

            String imagePath = ds.getString("imagePath");
            entity.imagePath = (imagePath == null || imagePath.trim().isEmpty()) ? null : imagePath;

            String pdfPath = ds.getString("pdfPath");
            entity.pdfPath = (pdfPath == null || pdfPath.trim().isEmpty()) ? null : pdfPath;

            entity.updatedAt = getLong(ds, "updatedAt", System.currentTimeMillis());
            entity.deleted = Boolean.TRUE.equals(ds.getBoolean("deleted"));
            entity.dirty = false;

            return entity;
        } catch (Exception e) {
            return null;
        }
    }

    private long getLong(DocumentSnapshot ds, String key, long fallback) {
        Object value = ds.get(key);
        if (value instanceof Number) return ((Number) value).longValue();
        return fallback;
    }
}