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
    private final ImageRemoteDataSource remote;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public ImageRepositoryImpl(Context context, ImageRemoteDataSource remote) {
        this.context = context.getApplicationContext();
        this.remote = remote;
    }

    @Override
    public void attachLocalImage(String uid, String docId, Uri uri, Callback callback) {
        io.execute(() -> {
            try {
                File baseDir = context.getExternalFilesDir(null);
                if (baseDir == null) baseDir = context.getFilesDir();
                
                File dir = new File(baseDir, "doc_images");
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

                // Return the local path directly as requested by the user
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
            File baseDir = context.getExternalFilesDir(null);
            if (baseDir == null) baseDir = context.getFilesDir();
            
            File file = new File(new File(baseDir, "doc_images"), docId + ".jpg");
            if (file.exists() && !file.delete()) {
                callback.onComplete(new Result.Error<>("Failed to delete local image"));
                return;
            }
            
            callback.onComplete(new Result.Success<>(""));
        });
    }
}