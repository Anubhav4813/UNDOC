package com.docexpiry.tracker.data.remote.storage;

import android.content.Context;
import android.net.Uri;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ImageRemoteDataSource {
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    public interface Callback {
        void onComplete(String downloadUrl, String error);
    }

    public void uploadImage(String uid, String docId, Uri uri, Callback callback) {
        StorageReference ref = storage.getReference()
                .child("users")
                .child(uid)
                .child("images")
                .child(docId + ".jpg");

        ref.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> callback.onComplete(downloadUri.toString(), null))
                .addOnFailureListener(error -> callback.onComplete(null, error.getMessage()));
    }

    public void deleteImage(String uid, String docId, Callback callback) {
        storage.getReference()
                .child("users")
                .child(uid)
                .child("images")
                .child(docId + ".jpg")
                .delete()
                .addOnSuccessListener(v -> callback.onComplete(null, null))
                .addOnFailureListener(error -> callback.onComplete(null, error.getMessage()));
    }
}