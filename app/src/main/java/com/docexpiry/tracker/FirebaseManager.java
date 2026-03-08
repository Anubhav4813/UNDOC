package com.docexpiry.tracker;

import android.content.Context;
import android.net.Uri;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single access point for all Firebase operations:
 *   - Auth  : email/password sign-up, sign-in, password reset
 *   - Firestore : CRUD for Document objects under /users/{uid}/documents/
 *   - Storage   : upload/download document images under /users/{uid}/images/
 */
public class FirebaseManager {

    public interface AuthCallback   { void onResult(boolean success, String error); }
    public interface DocsCallback   { void onResult(List<Document> docs, String error); }
    public interface UploadCallback { void onResult(String downloadUrl, String error); }
    public interface SimpleCallback { void onResult(String error); }

    private static final String COLLECTION = "documents";

    private final FirebaseAuth      auth;
    private final FirebaseFirestore db;
    private final FirebaseStorage   storage;

    public FirebaseManager() {
        auth    = FirebaseAuth.getInstance();
        db      = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    // ── Auth ─────────────────────────────────────────────────────────────────

    public FirebaseUser currentUser() { return auth.getCurrentUser(); }
    public boolean isSignedIn()       { return auth.getCurrentUser() != null; }

    public void signUp(String email, String password, AuthCallback cb) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(r -> cb.onResult(true, null))
            .addOnFailureListener(e -> cb.onResult(false, friendlyError(e.getMessage())));
    }

    public void signIn(String email, String password, AuthCallback cb) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(r -> cb.onResult(true, null))
            .addOnFailureListener(e -> cb.onResult(false, friendlyError(e.getMessage())));
    }

    public void sendPasswordReset(String email, AuthCallback cb) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener(r -> cb.onResult(true, null))
            .addOnFailureListener(e -> cb.onResult(false, friendlyError(e.getMessage())));
    }

    public void signOut() { auth.signOut(); }

    public void signInWithGoogle(String idToken, AuthCallback cb) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
            .addOnSuccessListener(r -> cb.onResult(true, null))
            .addOnFailureListener(e -> cb.onResult(false, friendlyError(e.getMessage())));
    }

    // ── Firestore ─────────────────────────────────────────────────────────────

    /** Download all documents for the current user from Firestore. */
    public void loadDocuments(DocsCallback cb) {
        String uid = uid();
        if (uid == null) { cb.onResult(null, "Not signed in"); return; }

        db.collection("users").document(uid).collection(COLLECTION)
            .get()
            .addOnSuccessListener(snapshot -> {
                List<Document> docs = new ArrayList<>();
                for (DocumentSnapshot ds : snapshot.getDocuments()) {
                    Document d = fromMap(ds);
                    if (d != null) docs.add(d);
                }
                cb.onResult(docs, null);
            })
            .addOnFailureListener(e -> cb.onResult(null, e.getMessage()));
    }

    /** Upload (overwrite) a single document to Firestore. */
    public void saveDocument(Document doc, SimpleCallback cb) {
        String uid = uid();
        if (uid == null) { if (cb != null) cb.onResult("Not signed in"); return; }

        db.collection("users").document(uid)
            .collection(COLLECTION).document(String.valueOf(doc.getId()))
            .set(toMap(doc))
            .addOnSuccessListener(r -> { if (cb != null) cb.onResult(null); })
            .addOnFailureListener(e -> { if (cb != null) cb.onResult(e.getMessage()); });
    }

    /** Upload all documents (full sync). */
    public void saveAllDocuments(List<Document> docs, SimpleCallback cb) {
        String uid = uid();
        if (uid == null) { if (cb != null) cb.onResult("Not signed in"); return; }

        // Use a batch write for atomicity
        com.google.firebase.firestore.WriteBatch batch = db.batch();
        com.google.firebase.firestore.CollectionReference col =
            db.collection("users").document(uid).collection(COLLECTION);

        for (Document doc : docs) {
            batch.set(col.document(String.valueOf(doc.getId())), toMap(doc));
        }
        batch.commit()
            .addOnSuccessListener(r -> { if (cb != null) cb.onResult(null); })
            .addOnFailureListener(e -> { if (cb != null) cb.onResult(e.getMessage()); });
    }

    /** Delete a single document from Firestore. */
    public void deleteDocument(int docId, SimpleCallback cb) {
        String uid = uid();
        if (uid == null) { if (cb != null) cb.onResult("Not signed in"); return; }

        db.collection("users").document(uid)
            .collection(COLLECTION).document(String.valueOf(docId))
            .delete()
            .addOnSuccessListener(r -> { if (cb != null) cb.onResult(null); })
            .addOnFailureListener(e -> { if (cb != null) cb.onResult(e.getMessage()); });
    }

    // ── Storage ───────────────────────────────────────────────────────────────

    /** Upload a local image file to Firebase Storage and return its download URL. */
    public void uploadImage(int docId, Uri localUri, Context ctx, UploadCallback cb) {
        String uid = uid();
        if (uid == null) { cb.onResult(null, "Not signed in"); return; }

        StorageReference ref = storage.getReference()
            .child("users").child(uid).child("images").child(docId + ".jpg");

        ref.putFile(localUri)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                return ref.getDownloadUrl();
            })
            .addOnSuccessListener(uri -> cb.onResult(uri.toString(), null))
            .addOnFailureListener(e -> cb.onResult(null, e.getMessage()));
    }

    /** Delete the image for a document from Firebase Storage. */
    public void deleteImage(int docId, SimpleCallback cb) {
        String uid = uid();
        if (uid == null) { if (cb != null) cb.onResult(null); return; }

        storage.getReference()
            .child("users").child(uid).child("images").child(docId + ".jpg")
            .delete()
            .addOnSuccessListener(r -> { if (cb != null) cb.onResult(null); })
            .addOnFailureListener(e -> { if (cb != null) cb.onResult(null); }); // silent fail
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String uid() {
        FirebaseUser u = auth.getCurrentUser();
        return u != null ? u.getUid() : null;
    }

    private static Map<String, Object> toMap(Document d) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",          d.getId());
        m.put("name",        d.getName());
        m.put("category",    d.getCategory());
        m.put("issueDate",   d.getIssueDateMillis());
        m.put("expiryDate",  d.getExpiryDateMillis());
        m.put("imagePath",   d.getImagePath() != null ? d.getImagePath() : "");
        m.put("tag",         d.getTag()       != null ? d.getTag()       : "");
        return m;
    }

    private static Document fromMap(DocumentSnapshot ds) {
        try {
            int    id       = ((Long) ds.get("id")).intValue();
            String name     = ds.getString("name");
            String category = ds.getString("category");
            long   issue    = (Long) ds.get("issueDate");
            long   expiry   = (Long) ds.get("expiryDate");
            String imgPath  = ds.getString("imagePath");
            String tag      = ds.getString("tag");

            Document doc = new Document(id, name, category, issue, expiry,
                (imgPath != null && !imgPath.isEmpty()) ? imgPath : null);
            if (tag != null && !tag.isEmpty()) doc.setTag(tag);
            return doc;
        } catch (Exception e) {
            return null;
        }
    }

    /** Convert Firebase error messages to user-friendly text. */
    private static String friendlyError(String raw) {
        if (raw == null) return "Something went wrong. Please try again.";
        if (raw.contains("email address is already in use"))
            return "An account with this email already exists. Sign in instead.";
        if (raw.contains("no user record") || raw.contains("INVALID_LOGIN_CREDENTIALS"))
            return "Email or password is incorrect.";
        if (raw.contains("badly formatted"))
            return "Please enter a valid email address.";
        if (raw.contains("password is invalid") || raw.contains("at least 6"))
            return "Password must be at least 6 characters.";
        if (raw.contains("network"))
            return "No internet connection. Please check your network.";
        return "Error: " + raw;
    }
}
