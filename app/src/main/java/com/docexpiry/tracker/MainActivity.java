package com.docexpiry.tracker;

import android.app.Activity;
import android.app.DatePickerDialog;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private List<Document> docs;
    private DocumentAdapter adapter;
    private DocumentRepository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private android.widget.LinearLayout tvEmpty;
    private android.widget.TextView tvDocCount;
    private int pendingImageDocId = -1; // which doc we're picking image for
    private static final int CAMERA_PERMISSION_CODE = 101;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private String tempCameraPath;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (adapter != null) adapter.updateCountdowns();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repo = new DocumentRepository(this);
        docs = repo.loadAll();

        tvEmpty    = (android.widget.LinearLayout) findViewById(R.id.tv_empty);
        tvDocCount = (android.widget.TextView) findViewById(R.id.tv_doc_count);

        RecyclerView rv = findViewById(R.id.recycler_view);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DocumentAdapter(docs, new DocumentAdapter.OnItemClickListener() {
            @Override public void onDelete(int position) { confirmDelete(position); }
            @Override public void onImageClick(int position) { showImageOptions(position); }
            @Override public void onCardClick(int position) { showDocumentDetail(position); }
        });
        rv.setAdapter(adapter);

        updateEmptyState();

        // FAB
        android.widget.TextView fab = (android.widget.TextView) findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> showAddDialog());

        // Image picker from gallery
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    saveImageForDoc(uri, pendingImageDocId);
                }
            });

        // Camera
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    saveImagePathForDoc(tempCameraPath, pendingImageDocId);
                }
            });

        handler.post(ticker);
    }

    // ───────────────────────────────────────────────
    // ADD DOCUMENT DIALOG
    // ───────────────────────────────────────────────
    private void showAddDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_document, null);

        TextInputEditText etName   = view.findViewById(R.id.et_doc_name);
        TextInputEditText etIssue  = view.findViewById(R.id.et_issue_date);
        TextInputEditText etExpiry = view.findViewById(R.id.et_expiry_date);
        AutoCompleteTextView acCategory = view.findViewById(R.id.ac_category);

        String[] categories = {"Vehicle", "Identity", "Insurance", "Medical", "Travel", "Education", "Property", "Other"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        acCategory.setAdapter(catAdapter);
        acCategory.setText("Vehicle", false);

        etIssue.setOnClickListener(v  -> pickDate(etIssue));
        etExpiry.setOnClickListener(v -> pickDate(etExpiry));
        etIssue.setFocusable(false);
        etExpiry.setFocusable(false);

        new MaterialAlertDialogBuilder(this, R.style.DialogTheme)
            .setTitle("Add New Document")
            .setView(view)
            .setPositiveButton("Add Document", (dialog, which) -> {
                String name     = etName.getText() != null ? etName.getText().toString().trim() : "";
                String issue    = etIssue.getText() != null ? etIssue.getText().toString().trim() : "";
                String expiry   = etExpiry.getText() != null ? etExpiry.getText().toString().trim() : "";
                String category = acCategory.getText().toString().trim();

                if (name.isEmpty())   { toast("Please enter document name"); return; }
                if (issue.isEmpty())  { toast("Please select issue date"); return; }
                if (expiry.isEmpty()) { toast("Please select expiry date"); return; }

                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    long issueMs  = sdf.parse(issue).getTime();
                    long expiryMs = sdf.parse(expiry).getTime();
                    if (expiryMs <= issueMs) { toast("Expiry must be after issue date"); return; }

                    Document doc = new Document(repo.nextId(docs), name, category.isEmpty() ? "Other" : category, issueMs, expiryMs, null);
                    docs.add(doc);
                    repo.saveAll(docs);
                    adapter.notifyItemInserted(docs.size() - 1);
                    updateEmptyState();
                    toast("Document added!");
                } catch (Exception e) {
                    toast("Invalid date. Use DD/MM/YYYY format");
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void pickDate(TextInputEditText et) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setPositiveButtonText("Set")
            .setNegativeButtonText("Cancel")
            .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            java.util.Calendar cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
            cal.setTimeInMillis(selection);
            et.setText(String.format(java.util.Locale.getDefault(), "%02d/%02d/%04d",
                cal.get(java.util.Calendar.DAY_OF_MONTH),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.YEAR)));
        });

        picker.show(getSupportFragmentManager(), "date_picker");
    }

    // ───────────────────────────────────────────────
    // IMAGE OPTIONS BOTTOM SHEET
    // ───────────────────────────────────────────────
    private void showImageOptions(int position) {
        pendingImageDocId = docs.get(position).getId();
        Document doc = docs.get(position);

        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View v = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_image, null);
        sheet.setContentView(v);

        v.findViewById(R.id.btn_gallery).setOnClickListener(x -> {
            sheet.dismiss();
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        v.findViewById(R.id.btn_camera).setOnClickListener(x -> {
            sheet.dismiss();
            launchCamera();
        });

        View btnView = v.findViewById(R.id.btn_view_image);
        if (doc.getImagePath() != null && !doc.getImagePath().isEmpty()) {
            btnView.setVisibility(View.VISIBLE);
            btnView.setOnClickListener(x -> {
                sheet.dismiss();
                showFullImage(doc.getImagePath());
            });
        } else {
            btnView.setVisibility(View.GONE);
        }

        View btnRemove = v.findViewById(R.id.btn_remove_image);
        if (doc.getImagePath() != null && !doc.getImagePath().isEmpty()) {
            btnRemove.setVisibility(View.VISIBLE);
            btnRemove.setOnClickListener(x -> {
                sheet.dismiss();
                doc.setImagePath(null);
                repo.saveAll(docs);
                int idx = docs.indexOf(doc);
                if (idx >= 0) adapter.notifyItemChanged(idx);
                toast("Image removed");
            });
        } else {
            btnRemove.setVisibility(View.GONE);
        }

        sheet.show();
    }

    private void launchCamera() {
        // Check camera permission first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            return;
        }
        openCameraIntent();
    }

    private void openCameraIntent() {
        try {
            // Create output file
            File imgFile = new File(getFilesDir(), "cam_" + System.currentTimeMillis() + ".jpg");
            imgFile.getParentFile().mkdirs();
            imgFile.createNewFile();
            tempCameraPath = imgFile.getAbsolutePath();

            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                this, getPackageName() + ".provider", imgFile);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            // Grant write permission to every app that could handle this intent
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            // Grant permission explicitly to each resolving camera app (needed on Android 11+)
            java.util.List<android.content.pm.ResolveInfo> resolvedList =
                getPackageManager().queryIntentActivities(intent,
                    android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
            for (android.content.pm.ResolveInfo info : resolvedList) {
                grantUriPermission(info.activityInfo.packageName, uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            // Launch directly — don't use resolveActivity() which fails on Android 11+
            cameraLauncher.launch(intent);

        } catch (Exception e) {
            toast("Could not open camera: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCameraIntent();
            } else {
                toast("Camera permission denied. Please allow it in Settings.");
            }
        }
    }

    private void saveImageForDoc(Uri uri, int docId) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            File outFile = new File(getFilesDir(), "doc_img_" + docId + "_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream out = new FileOutputStream(outFile);
            Bitmap bmp = BitmapFactory.decodeStream(in);
            // Scale down to max 1024px
            bmp = scaleBitmap(bmp, 1024);
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.close();
            if (in != null) in.close();
            saveImagePathForDoc(outFile.getAbsolutePath(), docId);
        } catch (Exception e) {
            toast("Failed to save image");
        }
    }

    private void saveImagePathForDoc(String path, int docId) {
        for (int i = 0; i < docs.size(); i++) {
            if (docs.get(i).getId() == docId) {
                docs.get(i).setImagePath(path);
                repo.saveAll(docs);
                adapter.notifyItemChanged(i);
                toast("Image saved!");
                return;
            }
        }
    }

    private Bitmap scaleBitmap(Bitmap src, int maxSize) {
        int w = src.getWidth(), h = src.getHeight();
        if (w <= maxSize && h <= maxSize) return src;
        float ratio = Math.min((float) maxSize / w, (float) maxSize / h);
        return Bitmap.createScaledBitmap(src, (int)(w * ratio), (int)(h * ratio), true);
    }

    // ───────────────────────────────────────────────
    // FULL SCREEN IMAGE VIEWER
    // ───────────────────────────────────────────────
    private void showFullImage(String imagePath) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_image_viewer, null);
        ImageView iv = view.findViewById(R.id.iv_full_image);
        Bitmap bmp = BitmapFactory.decodeFile(imagePath);
        if (bmp != null) iv.setImageBitmap(bmp);

        new MaterialAlertDialogBuilder(this, R.style.FullscreenDialogTheme)
            .setView(view)
            .setPositiveButton("Close", null)
            .show();
    }

    // ───────────────────────────────────────────────
    // DOCUMENT DETAIL BOTTOM SHEET
    // ───────────────────────────────────────────────
    private void showDocumentDetail(int position) {
        Document doc = docs.get(position);
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View v = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_detail, null);
        sheet.setContentView(v);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        ((TextView)v.findViewById(R.id.tv_detail_name)).setText(doc.getName());
        ((TextView)v.findViewById(R.id.tv_detail_category)).setText(doc.getCategory());
        ((TextView)v.findViewById(R.id.tv_detail_issue)).setText(sdf.format(new java.util.Date(doc.getIssueDateMillis())));
        ((TextView)v.findViewById(R.id.tv_detail_expiry)).setText(sdf.format(new java.util.Date(doc.getExpiryDateMillis())));
        ((TextView)v.findViewById(R.id.tv_detail_status)).setText(doc.getStatusLabel());
        ((TextView)v.findViewById(R.id.tv_detail_status)).setTextColor(doc.getStatusColor());
        ((TextView)v.findViewById(R.id.tv_detail_days)).setText(doc.isExpired() ? "Expired" : doc.getRemainingDays() + " days remaining");

        ImageView ivDetail = v.findViewById(R.id.iv_detail_image);
        if (doc.getImagePath() != null && !doc.getImagePath().isEmpty()) {
            Bitmap bmp = BitmapFactory.decodeFile(doc.getImagePath());
            if (bmp != null) { ivDetail.setImageBitmap(bmp); ivDetail.setVisibility(View.VISIBLE); }
        }

        v.findViewById(R.id.btn_detail_close).setOnClickListener(x -> sheet.dismiss());
        sheet.show();
    }

    // ───────────────────────────────────────────────
    // DELETE
    // ───────────────────────────────────────────────
    private void confirmDelete(int position) {
        new MaterialAlertDialogBuilder(this, R.style.DialogTheme)
            .setTitle("Delete Document")
            .setMessage("Remove \"" + docs.get(position).getName() + "\" from tracker?")
            .setPositiveButton("Delete", (d, w) -> {
                docs.remove(position);
                repo.saveAll(docs);
                adapter.notifyItemRemoved(position);
                updateEmptyState();
                toast("Document removed");
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void updateEmptyState() {
        boolean empty = docs.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        tvDocCount.setText(docs.size() + " document" + (docs.size() == 1 ? "" : "s"));
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(ticker);
    }
}
