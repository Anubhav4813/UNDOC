package com.docexpiry.tracker.presentation.documents.preview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.docexpiry.tracker.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImagePreviewActivity extends AppCompatActivity {
    private static final String TAG = "ImagePreviewActivity";

    public static final String EXTRA_IMAGE_PATH = "image_path";
    public static final String EXTRA_DOC_NAME = "doc_name";

    private ExecutorService bgExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        bgExecutor = Executors.newSingleThreadExecutor();

        final ImageView ivPreview = findViewById(R.id.iv_preview);
        TextView tvTitle = findViewById(R.id.tv_preview_title);
        TextView tvClose = findViewById(R.id.tv_close);

        String imagePath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
        String docName = getIntent().getStringExtra(EXTRA_DOC_NAME);

        tvTitle.setText(docName != null && !docName.trim().isEmpty() ? docName : "Image Preview");

        if (imagePath == null || imagePath.trim().isEmpty()) {
            Toast.makeText(this, "No image path provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "preview path: " + imagePath);

        final Uri uri = toUri(imagePath);

        // If it's a file, ensure it exists.
        if ((uri.getScheme() == null || "file".equals(uri.getScheme()))) {
            File f = new File(uri.getPath());
            if (!f.exists()) {
                Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        // Load using Glide off the UI thread, sized to the ImageView.
        ivPreview.setVisibility(View.INVISIBLE);
        ivPreview.post(() -> {
            int w = ivPreview.getWidth() > 0 ? ivPreview.getWidth() : 1080;
            int h = ivPreview.getHeight() > 0 ? ivPreview.getHeight() : 1920;

            RequestOptions opts = new RequestOptions()
                    .override(w, h)
                    .fitCenter()
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .format(DecodeFormat.PREFER_RGB_565);

            try {
                Glide.with(this)
                        .load(uri)
                        .apply(opts)
                        .thumbnail(0.1f)
                        .into(ivPreview);
                ivPreview.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Log.w(TAG, "Glide load failed, using fallback decode: " + e.getMessage());
                fallbackDecodeAndSet(uri, ivPreview, w, h);
            }
        });

        tvClose.setOnClickListener(v -> finish());
    }

    private Uri toUri(String imagePath) {
        Uri uri = Uri.parse(imagePath);
        if (uri.getScheme() == null || uri.getScheme().isEmpty()) {
            return Uri.fromFile(new File(imagePath));
        }
        return uri;
    }

    private void fallbackDecodeAndSet(Uri uri, ImageView iv, int reqW, int reqH) {
        bgExecutor.submit(() -> {
            Bitmap bmp = decodeSampledBitmap(uri, reqW, reqH);
            if (bmp != null) {
                runOnUiThread(() -> iv.setImageBitmap(bmp));
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(ImagePreviewActivity.this, "Unable to load image", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private Bitmap decodeSampledBitmap(Uri uri, int reqWidth, int reqHeight) {
        try {
            String scheme = uri.getScheme();
            if (scheme == null || "file".equals(scheme)) {
                File f = new File(uri.getPath());
                if (!f.exists()) return null;

                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(f.getAbsolutePath(), opts);

                opts.inSampleSize = calculateInSampleSize(opts, reqWidth, reqHeight);
                opts.inJustDecodeBounds = false;
                opts.inPreferredConfig = Bitmap.Config.RGB_565;

                return BitmapFactory.decodeFile(f.getAbsolutePath(), opts);
            } else {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                InputStream is1 = getContentResolver().openInputStream(uri);
                if (is1 == null) return null;
                BitmapFactory.decodeStream(is1, null, opts);
                is1.close();

                opts.inSampleSize = calculateInSampleSize(opts, reqWidth, reqHeight);
                opts.inJustDecodeBounds = false;
                opts.inPreferredConfig = Bitmap.Config.RGB_565;

                InputStream is2 = getContentResolver().openInputStream(uri);
                if (is2 == null) return null;
                Bitmap bmp = BitmapFactory.decodeStream(is2, null, opts);
                is2.close();
                return bmp;
            }
        } catch (OutOfMemoryError oom) {
            Log.w(TAG, "OOM while decoding image", oom);
            return null;
        } catch (IOException e) {
            Log.w(TAG, "IO error while decoding image", e);
            return null;
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            while ((height / inSampleSize) > reqHeight || (width / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bgExecutor != null && !bgExecutor.isShutdown()) {
            bgExecutor.shutdownNow();
        }
    }
}