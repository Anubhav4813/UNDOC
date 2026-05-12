package com.docexpiry.tracker.domain.repository;

import android.net.Uri;
import com.docexpiry.tracker.core.common.Result;

public interface ImageRepository {
    void attachLocalImage(String uid, String docId, Uri uri, Callback callback);
    void removeImage(String uid, String docId, Callback callback);

    interface Callback {
        void onComplete(Result<String> result);
    }
}