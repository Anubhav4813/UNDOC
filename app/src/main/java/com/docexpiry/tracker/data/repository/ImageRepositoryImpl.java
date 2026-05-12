package com.docexpiry.tracker.data.repository;

import android.content.Context;
import android.net.Uri;

import com.docexpiry.tracker.core.common.Result;
import com.docexpiry.tracker.data.remote.storage.ImageRemoteDataSource;
import com.docexpiry.tracker.domain.repository.ImageRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageRepositoryImpl implements ImageRepository {
    private final Context context;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    // Keep this constructor signature so existing wiring does not break.
    public ImageRepositoryImpl(Context context, ImageRemoteDataSource remote) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void attachLocalImage(String uid, String docId, Uri uri, Callback callback) {
        io.execute(() -> {
            try {
                File dir = new File(context.getFilesDir(), "doc_images");
                if (!dir.exists() && !dir.mkdirs()) {
                    callback.onComplete(new Result.Error<>("Failed to create local image directory"));
                    return;
                }

                File outFile = new File(dir, docId + ".jpg");

                try (InputStream in = context.getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(outFile, false)) {

                    if (in == null) {
                        callback.onComplete(new Result.Error<>("Unable to read selected image"));
                        return;
                    }

                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                }

                callback.onComplete(new Result.Success<>(outFile.getAbsolutePath()));
            } catch (Exception e) {
                callback.onComplete(new Result.Error<>(
                        e.getMessage() != null ? e.getMessage() : "Local save failed"
                ));
            }
        });
    }

    @Override
    public void removeImage(String uid, String docId, Callback callback) {
        io.execute(() -> {
            File file = new File(new File(context.getFilesDir(), "doc_images"), docId + ".jpg");
            if (file.exists() && !file.delete()) {
                callback.onComplete(new Result.Error<>("Failed to delete local image"));
                return;
            }
            callback.onComplete(new Result.Success<>(""));
        });
    }
}