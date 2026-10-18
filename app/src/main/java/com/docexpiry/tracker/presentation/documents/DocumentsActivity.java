package com.docexpiry.tracker.presentation.documents;

import android.graphics.Color;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.docexpiry.tracker.R;
import com.docexpiry.tracker.data.local.db.UndocDatabase;
import com.docexpiry.tracker.data.mapper.DocumentMapper;
import com.docexpiry.tracker.data.remote.auth.AuthRemoteDataSource;
import com.docexpiry.tracker.data.remote.firestore.DocumentRemoteDataSource;
import com.docexpiry.tracker.data.remote.storage.ImageRemoteDataSource;
import com.docexpiry.tracker.data.repository.AuthRepositoryImpl;
import com.docexpiry.tracker.data.repository.DocumentRepositoryImpl;
import com.docexpiry.tracker.data.repository.ImageRepositoryImpl;
import com.docexpiry.tracker.domain.model.Document;
import com.docexpiry.tracker.domain.repository.AuthRepository;
import com.docexpiry.tracker.domain.repository.DocumentRepository;
import com.docexpiry.tracker.domain.repository.ImageRepository;
import com.docexpiry.tracker.domain.usecase.document.AddDocumentUseCase;
import com.docexpiry.tracker.domain.usecase.document.DeleteDocumentUseCase;
import com.docexpiry.tracker.domain.usecase.document.ObserveDocumentsUseCase;
import com.docexpiry.tracker.domain.usecase.image.AttachImageUseCase;
import com.docexpiry.tracker.domain.usecase.image.RemoveImageUseCase;
import com.docexpiry.tracker.domain.usecase.pdf.AttachPdfUseCase;
import com.docexpiry.tracker.domain.usecase.pdf.RemovePdfUseCase;
import com.docexpiry.tracker.presentation.auth.LoginActivity;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import com.docexpiry.tracker.core.common.Result;
import java.util.Locale;
import java.util.UUID;
import com.docexpiry.tracker.data.local.dao.DocumentDao;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.net.Uri;
import java.io.File;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import com.docexpiry.tracker.presentation.documents.preview.ImagePreviewActivity;
import com.docexpiry.tracker.domain.service.NotificationService;
import com.docexpiry.tracker.data.worker.NotificationServiceImpl;
import android.os.Build;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class DocumentsActivity extends AppCompatActivity {
    private static final int REQUEST_PICK_IMAGE = 1001;
    private static final int REQUEST_IMAGE_CAPTURE = 1002;
    private static final int REQUEST_GALLERY_FOR_NEW_DOC = 1003;
    private static final int REQUEST_PICK_PDF = 1004;
    private static final int REQUEST_CAMERA_PERMISSION = 2001;

    private DocumentsViewModel viewModel;
    private DocumentsListAdapter adapter;
    private RecyclerView recyclerView;
    private TextView tvEmptyDocuments;
    private AuthRepository authRepository;
    private Document currentDocForImage;
    private DocumentDao documentDao;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private File cameraPhotoFile;
    private Uri cameraPhotoUri;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (adapter != null)
                adapter.updateCountdowns();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        UndocDatabase db = UndocDatabase.getInstance(this);
        documentDao = db.documentDao();

        NotificationService notificationService = new NotificationServiceImpl(getApplicationContext());

        DocumentRepository documentRepository = new DocumentRepositoryImpl(
                documentDao,
                new DocumentMapper(),
                new DocumentRemoteDataSource(),
                notificationService);

        authRepository = new AuthRepositoryImpl(new AuthRemoteDataSource());

        ImageRepository imageRepository = new ImageRepositoryImpl(
                getApplicationContext(),
                new ImageRemoteDataSource());

        DocumentsViewModelFactory factory = new DocumentsViewModelFactory(
                new ObserveDocumentsUseCase(documentRepository),
                new AddDocumentUseCase(documentRepository),
                new DeleteDocumentUseCase(documentRepository),
                new AttachImageUseCase(imageRepository),
                new RemoveImageUseCase(imageRepository),
                new AttachPdfUseCase(imageRepository),
                new RemovePdfUseCase(imageRepository),
                authRepository);

        viewModel = new ViewModelProvider(this, factory).get(DocumentsViewModel.class);

        recyclerView = findViewById(R.id.recycler_view);
        tvEmptyDocuments = findViewById(R.id.tv_empty_documents);
        TextView btnLogout = findViewById(R.id.btn_logout);
        FloatingActionButton btnAdd = findViewById(R.id.btn_add_document);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DocumentsListAdapter(new DocumentsListAdapter.ActionListener() {
            @Override
            public void onDelete(Document doc) {
                new MaterialAlertDialogBuilder(DocumentsActivity.this, R.style.DialogTheme)
                        .setTitle("Delete document")
                        .setMessage("Delete \"" + (doc.getName() != null ? doc.getName() : "this document")
                                + "\"? This action cannot be undone.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (dialog, which) -> {
                            viewModel.delete(doc.getId());
                            // optional: show undo via Snackbar (requires CoordinatorLayout root)
                            // Snackbar.make(recyclerView, "Deleted", Snackbar.LENGTH_LONG)
                            // .setAction("Undo", v -> { /* re-add or cancel delete logic */ })
                            // .show();
                        })
                        .show();
            }

            @Override
            public void onClick(Document doc) {
                showDetailSheet(doc);
            }

            @Override
            public void onImageClick(Document doc) {
                if (doc.getImagePath() != null && !doc.getImagePath().trim().isEmpty()) {
                    // If image exists, preview it
                    Intent intent = new Intent(DocumentsActivity.this, ImagePreviewActivity.class);
                    intent.putExtra(ImagePreviewActivity.EXTRA_IMAGE_PATH, doc.getImagePath());
                    intent.putExtra(ImagePreviewActivity.EXTRA_DOC_NAME, doc.getName());
                    startActivity(intent);
                } else if (doc.getPdfPath() != null && !doc.getPdfPath().trim().isEmpty()) {
                    viewPdf(doc.getPdfPath());
                } else {
                    // Otherwise, show options to pick image or PDF
                    showImageOptions(doc);
                }
            }
        });
        recyclerView.setAdapter(adapter);

        btnLogout.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this, R.style.DialogTheme)
                    .setTitle("Confirm logout")
                    .setMessage("Are you sure you want to sign out?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Sign out", (dialog, which) -> {
                        authRepository.signOut();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    })
                    .show();
        });

        btnAdd.setOnClickListener(v -> {
            String[] options = {"Take Photo", "Choose from Gallery", "Upload PDF", "Manual Entry"};
            new MaterialAlertDialogBuilder(this, R.style.DialogTheme)
                    .setTitle("Add Document")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            checkCameraPermissionAndLaunch();
                        } else if (which == 1) {
                            pickFromGalleryForNewDoc();
                        } else if (which == 2) {
                            pickPdf();
                        } else {
                            showAddDialog();
                        }
                    })
                    .show();
        });

        viewModel.start();

        viewModel.getGroupedDocuments().observe(this, items -> {
            adapter.submitList(items == null ? new java.util.ArrayList<>() : items);
            boolean isEmpty = items == null || items.isEmpty();
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            tvEmptyDocuments.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        });

        viewModel.uiState().observe(this, state -> {
            if (state == null)
                return;
            if (state.error != null) {
                Toast.makeText(this, state.error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(ticker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    private void showAddDialog() {
        showAddDialog("", "", null, null);
    }

    private void showAddDialog(String prefilledIssue, String prefilledExpiry, Uri imageUri, Uri pdfUri) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_document, null);

        TextInputEditText etName = view.findViewById(R.id.et_doc_name);
        TextInputEditText etTag = view.findViewById(R.id.et_doc_tag);
        TextInputEditText etIssue = view.findViewById(R.id.et_issue_date);
        TextInputEditText etExpiry = view.findViewById(R.id.et_expiry_date);
        AutoCompleteTextView acCategory = view.findViewById(R.id.ac_category);

        etIssue.setText(prefilledIssue);
        etExpiry.setText(prefilledExpiry);

        String[] categories = {
                "Vehicle", "Identity", "Insurance", "Medical",
                "Travel", "Education", "Property", "Other"
        };
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categories);
        acCategory.setAdapter(catAdapter);
        acCategory.setText("Vehicle", false);

        etIssue.setOnClickListener(v -> pickDate(etIssue));
        etExpiry.setOnClickListener(v -> pickDate(etExpiry));
        etIssue.setFocusable(false);
        etExpiry.setFocusable(false);

        new MaterialAlertDialogBuilder(this, R.style.DialogTheme)
                .setTitle("Add New Document")
                .setView(view)
                .setPositiveButton("Add Document", (dialog, which) -> {
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String tag = etTag.getText() != null ? etTag.getText().toString().trim() : "";
                    String issue = etIssue.getText() != null ? etIssue.getText().toString().trim() : "";
                    String expiry = etExpiry.getText() != null ? etExpiry.getText().toString().trim() : "";
                    String category = acCategory.getText().toString().trim();

                    if (name.isEmpty()) {
                        toast("Please enter document name");
                        return;
                    }
                    if (issue.isEmpty()) {
                        toast("Please select issue date");
                        return;
                    }
                    if (expiry.isEmpty()) {
                        toast("Please select expiry date");
                        return;
                    }

                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        long issueMs = sdf.parse(issue).getTime();
                        long expiryMs = sdf.parse(expiry).getTime();

                        if (expiryMs <= issueMs) {
                            toast("Expiry must be after issue date");
                            return;
                        }

                        String uid = authRepository.currentUid();
                        if (uid == null) {
                            toast("Not signed in");
                            return;
                        }

                        String docId = UUID.randomUUID().toString();
                        if (imageUri != null) {
                            viewModel.attachImage(docId, imageUri, result -> {
                                String path = null;
                                if (result instanceof Result.Success) {
                                    path = (String) ((Result.Success<?>) result).data;
                                } else if (result instanceof Result.Error) {
                                    runOnUiThread(() -> toast("Failed to save image locally: " + ((Result.Error<?>) result).message));
                                }

                                Document finalDoc = new Document(
                                        docId, uid, name,
                                        category.isEmpty() ? "Other" : category,
                                        issueMs, expiryMs,
                                        tag.isEmpty() ? null : tag,
                                        path,
                                        null);
                                viewModel.add(finalDoc);
                            });
                        } else if (pdfUri != null) {
                            viewModel.attachPdf(docId, pdfUri, result -> {
                                String path = null;
                                if (result instanceof Result.Success) {
                                    path = (String) ((Result.Success<?>) result).data;
                                } else if (result instanceof Result.Error) {
                                    runOnUiThread(() -> toast("Failed to save PDF locally: " + ((Result.Error<?>) result).message));
                                }

                                Document finalDoc = new Document(
                                        docId, uid, name,
                                        category.isEmpty() ? "Other" : category,
                                        issueMs, expiryMs,
                                        tag.isEmpty() ? null : tag,
                                        null,
                                        path);
                                viewModel.add(finalDoc);
                            });
                        } else {
                            Document doc = new Document(
                                    docId,
                                    uid,
                                    name,
                                    category.isEmpty() ? "Other" : category,
                                    issueMs,
                                    expiryMs,
                                    tag.isEmpty() ? null : tag,
                                    null,
                                    null);
                            viewModel.add(doc);
                        }
                        
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
            et.setText(String.format(
                    Locale.getDefault(),
                    "%02d/%02d/%04d",
                    cal.get(java.util.Calendar.DAY_OF_MONTH),
                    cal.get(java.util.Calendar.MONTH) + 1,
                    cal.get(java.util.Calendar.YEAR)));
        });

        picker.show(getSupportFragmentManager(), "date_picker");
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            dispatchTakePictureIntent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                toast("Camera permission is required to scan documents");
                showAddDialog("", "", null, null);
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                cameraPhotoFile = createImageFile();
                if (cameraPhotoFile != null) {
                    cameraPhotoUri = FileProvider.getUriForFile(this,
                            "com.docexpiry.tracker.provider",
                            cameraPhotoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            } catch (IOException ex) {
                toast("Error creating image file");
                showAddDialog("", "", null, null);
            }
        } else {
            toast("No camera app found");
            showAddDialog("", "", null, null);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void pickFromGalleryForNewDoc() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY_FOR_NEW_DOC);
    }

    private void pickPdf() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select PDF"), REQUEST_PICK_PDF);
    }

    private void runPdfTextRecognition(Uri uri) {
        ioExecutor.execute(() -> {
            try {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
                if (pfd != null) {
                    PdfRenderer renderer = new PdfRenderer(pfd);
                    StringBuilder fullText = new StringBuilder();
                    int pageCount = Math.min(renderer.getPageCount(), 3); // Check first 3 pages

                    for (int i = 0; i < pageCount; i++) {
                        PdfRenderer.Page page = renderer.openPage(i);
                        Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        
                        InputImage image = InputImage.fromBitmap(bitmap, 0);
                        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                        
                        try {
                            com.google.android.gms.tasks.Task<com.google.mlkit.vision.text.Text> result = recognizer.process(image);
                            com.google.mlkit.vision.text.Text text = com.google.android.gms.tasks.Tasks.await(result);
                            fullText.append(text.getText()).append("\n");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        page.close();
                    }
                    renderer.close();
                    pfd.close();

                    runOnUiThread(() -> extractDatesAndShowDialog(fullText.toString(), null, uri));
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> toast("Error reading PDF: " + e.getMessage()));
            }
        });
    }

    private void runTextRecognition(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image)
                    .addOnSuccessListener(text -> {
                        String fullText = text.getText();
                        extractDatesAndShowDialog(fullText, uri, null);
                    })
                    .addOnFailureListener(e -> {
                        toast("OCR failed: " + e.getMessage());
                        showAddDialog("", "", uri, null);
                    });
        } catch (IOException e) {
            toast("Error loading image for OCR");
            showAddDialog("", "", uri, null);
        }
    }

    private void extractDatesAndShowDialog(String text, Uri imageUri, Uri pdfUri) {
        String lowerText = text.toLowerCase(Locale.getDefault());

        // Simple regex for dd/MM/yyyy, dd-MM-yyyy, dd.MM.yyyy OR yyyy/MM/dd, etc.
        // Group 1: DD/MM/YYYY format, Group 2: YYYY/MM/DD format
        Pattern pattern = Pattern.compile("\\b(?:(\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{4})|(\\d{4}[/.-]\\d{1,2}[/.-]\\d{1,2}))\\b");
        Matcher matcher = pattern.matcher(text);

        class DateCandidate {
            final String date;
            final int pos;
            final boolean isExp;
            final boolean isIss;

            DateCandidate(String d, int p) {
                this.date = d.replace('-', '/').replace('.', '/');
                this.pos = p;

                // Look at context (approx 50 chars before the date)
                int start = Math.max(0, p - 50);
                String context = lowerText.substring(start, p);

                isExp = context.contains("exp") || context.contains("valid upto") || context.contains("val") || context.contains("until") || context.contains("till") || context.contains("doe") || context.contains("validity");
                isIss = context.contains("iss") || context.contains("doi") || context.contains("date of issue") || context.contains("mfd") || context.contains("reg") || context.contains("from") || context.contains("issue");
            }
        }

        ArrayList<DateCandidate> candidates = new ArrayList<>();
        while (matcher.find()) {
            String d = "";
            if (matcher.group(1) != null) {
                d = matcher.group(1);
            } else if (matcher.group(2) != null) {
                // Convert YYYY/MM/DD to DD/MM/YYYY
                String raw = matcher.group(2);
                String[] parts = raw.split("[/.-]");
                if (parts.length >= 3) {
                    d = parts[2] + "/" + parts[1] + "/" + parts[0];
                }
            }
            if (!d.isEmpty()) {
                candidates.add(new DateCandidate(d, matcher.start()));
            }
        }

        String issueStr = "";
        String expiryStr = "";

        // Try to find the best candidates based on context
        for (DateCandidate c : candidates) {
            if (c.isIss && issueStr.isEmpty()) issueStr = c.date;
            if (c.isExp && expiryStr.isEmpty()) expiryStr = c.date;
        }

        // Chronological list for fallback
        ArrayList<String> allDates = new ArrayList<>();
        for (DateCandidate c : candidates) {
            allDates.add(c.date);
        }
        if (allDates.size() >= 2) {
            Collections.sort(allDates, (d1, d2) -> {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    Date date1 = sdf.parse(d1);
                    Date date2 = sdf.parse(d2);
                    if (date1 == null || date2 == null) return 0;
                    return date1.compareTo(date2);
                } catch (Exception e) {
                    return 0;
                }
            });
        }

        // Fallback: if we are missing one or both, be smarter
        if (issueStr.isEmpty() || expiryStr.isEmpty()) {
            if (allDates.size() >= 2) {
                if (issueStr.isEmpty() && expiryStr.isEmpty()) {
                    if (allDates.size() >= 3) {
                        issueStr = allDates.get(1); // Skip DOB likely
                        expiryStr = allDates.get(allDates.size() - 1);
                    } else {
                        issueStr = allDates.get(0);
                        expiryStr = allDates.get(allDates.size() - 1);
                    }
                } else if (issueStr.isEmpty()) {
                    // Expiry was found via keyword, issue wasn't. Pick earliest that isn't expiry.
                    for (String d : allDates) {
                        if (!d.equals(expiryStr)) {
                            issueStr = d;
                            break;
                        }
                    }
                } else if (expiryStr.isEmpty()) {
                    // Issue was found via keyword, expiry wasn't. Pick latest that isn't issue.
                    for (int i = allDates.size() - 1; i >= 0; i--) {
                        if (!allDates.get(i).equals(issueStr)) {
                            expiryStr = allDates.get(i);
                            break;
                        }
                    }
                }
            } else if (allDates.size() == 1) {
                if (expiryStr.isEmpty()) expiryStr = allDates.get(0);
            }
        }

        // Final sanity check: Expiry should be after Issue
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            if (!issueStr.isEmpty() && !expiryStr.isEmpty()) {
                Date dIss = sdf.parse(issueStr);
                Date dExp = sdf.parse(expiryStr);
                if (dIss != null && dExp != null && dIss.after(dExp)) {
                    // Swap them
                    String temp = issueStr;
                    issueStr = expiryStr;
                    expiryStr = temp;
                }
            }
        } catch (Exception ignored) {}

        showAddDialog(issueStr, expiryStr, imageUri, pdfUri);
    }

    private void pickImageForDocument(Document doc) {
        showImageOptions(doc);
    }

    private void showImageOptions(Document doc) {
        View sheet = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_image, null);
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        dialog.setContentView(sheet);

        sheet.findViewById(R.id.btn_camera).setOnClickListener(v -> {
            dialog.dismiss();
            checkCameraPermissionAndLaunch();
        });
        sheet.findViewById(R.id.btn_gallery).setOnClickListener(v -> {
            dialog.dismiss();
            currentDocForImage = doc;
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
        });
        sheet.findViewById(R.id.btn_pdf).setOnClickListener(v -> {
            dialog.dismiss();
            currentDocForImage = doc;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            startActivityForResult(Intent.createChooser(intent, "Select PDF"), REQUEST_PICK_PDF);
        });

        dialog.show();
    }

    private void showDetailSheet(Document doc) {
        View sheet = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_detail, null);
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetTheme);
        dialog.setContentView(sheet);

        TextView tvName = sheet.findViewById(R.id.tv_detail_name);
        TextView tvCategory = sheet.findViewById(R.id.tv_detail_category);
        TextView tvStatus = sheet.findViewById(R.id.tv_detail_status);
        TextView tvDays = sheet.findViewById(R.id.tv_detail_days);
        TextView tvIssue = sheet.findViewById(R.id.tv_detail_issue);
        TextView tvExpiry = sheet.findViewById(R.id.tv_detail_expiry);
        ImageView ivImage = sheet.findViewById(R.id.iv_detail_image);
        View layoutPdf = sheet.findViewById(R.id.layout_detail_pdf);
        View btnClose = sheet.findViewById(R.id.btn_detail_close);

        tvName.setText(doc.getName());
        tvCategory.setText(doc.getCategory());

        long now = System.currentTimeMillis();
        long diff = doc.getExpiryDateMillis() - now;
        long days = diff / (24 * 60 * 60 * 1000);

        if (doc.isExpired()) {
            tvStatus.setText("EXPIRED");
            tvStatus.setBackgroundResource(R.drawable.bg_sheet_btn_danger);
            tvDays.setText("Document has expired");
            tvDays.setTextColor(Color.RED);
        } else {
            tvStatus.setText("ACTIVE");
            tvStatus.setBackgroundResource(R.drawable.bg_status_chip);
            tvDays.setText(days + " days remaining");
            tvDays.setTextColor(getResources().getColor(R.color.ui_primary));
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        tvIssue.setText(sdf.format(new Date(doc.getIssueDateMillis())));
        tvExpiry.setText(sdf.format(new Date(doc.getExpiryDateMillis())));

        if (doc.getImagePath() != null && !doc.getImagePath().isEmpty()) {
            ivImage.setVisibility(View.VISIBLE);
            layoutPdf.setVisibility(View.GONE);
            com.bumptech.glide.Glide.with(this)
                    .load(doc.getImagePath())
                    .into(ivImage);
            ivImage.setOnClickListener(v -> {
                Intent intent = new Intent(this, ImagePreviewActivity.class);
                intent.putExtra(ImagePreviewActivity.EXTRA_IMAGE_PATH, doc.getImagePath());
                intent.putExtra(ImagePreviewActivity.EXTRA_DOC_NAME, doc.getName());
                startActivity(intent);
            });
        } else if (doc.getPdfPath() != null && !doc.getPdfPath().isEmpty()) {
            ivImage.setVisibility(View.GONE);
            layoutPdf.setVisibility(View.VISIBLE);
            layoutPdf.setOnClickListener(v -> viewPdf(doc.getPdfPath()));
        } else {
            ivImage.setVisibility(View.GONE);
            layoutPdf.setVisibility(View.GONE);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void viewPdf(String path) {
        try {
            File file = new File(path);
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            toast("Cannot open PDF: " + e.getMessage());
        }
    }

    private Uri toUri(String imagePath) {
        Uri uri = Uri.parse(imagePath);
        if (uri.getScheme() == null) {
            return Uri.fromFile(new File(imagePath));
        }
        return uri;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            android.net.Uri imageUri = data.getData();
            if (currentDocForImage != null && imageUri != null) {
                Document targetDoc = currentDocForImage;
                currentDocForImage = null;
                viewModel.attachImage(targetDoc.getId(), imageUri, result -> {
                    if (result instanceof Result.Success) {
                        String localPath = (String) ((Result.Success<?>) result).data;

                        Document updatedDoc = new Document(
                                targetDoc.getId(),
                                targetDoc.getUid(),
                                targetDoc.getName(),
                                targetDoc.getCategory(),
                                targetDoc.getIssueDateMillis(),
                                targetDoc.getExpiryDateMillis(),
                                targetDoc.getTag(),
                                localPath,
                                targetDoc.getPdfPath());
                        viewModel.add(updatedDoc);

                        // debug: show path and existence
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Saved: " + localPath, Toast.LENGTH_LONG).show();
                            Intent preview = new Intent(DocumentsActivity.this, ImagePreviewActivity.class);
                            preview.putExtra(ImagePreviewActivity.EXTRA_IMAGE_PATH, localPath);
                            preview.putExtra(ImagePreviewActivity.EXTRA_DOC_NAME, targetDoc.getName());
                            startActivity(preview);
                        });
                    } else if (result instanceof Result.Error) {
                        String err = ((Result.Error<?>) result).message;
                        runOnUiThread(() -> toast("Save failed: " + err));
                    }
                });
            }
        } else if (requestCode == REQUEST_PICK_PDF && resultCode == RESULT_OK && data != null) {
            Uri pdfUri = data.getData();
            if (currentDocForImage != null && pdfUri != null) {
                Document targetDoc = currentDocForImage;
                currentDocForImage = null;
                viewModel.attachPdf(targetDoc.getId(), pdfUri, result -> {
                    if (result instanceof Result.Success) {
                        String localPath = (String) ((Result.Success<?>) result).data;
                        Document updatedDoc = new Document(
                                targetDoc.getId(),
                                targetDoc.getUid(),
                                targetDoc.getName(),
                                targetDoc.getCategory(),
                                targetDoc.getIssueDateMillis(),
                                targetDoc.getExpiryDateMillis(),
                                targetDoc.getTag(),
                                targetDoc.getImagePath(),
                                localPath);
                        viewModel.add(updatedDoc);
                        runOnUiThread(() -> toast("PDF saved locally"));
                    } else if (result instanceof Result.Error) {
                        String err = ((Result.Error<?>) result).message;
                        runOnUiThread(() -> toast("Save failed: " + err));
                    }
                });
            } else if (pdfUri != null) {
                // Main "Add Document" -> "Upload PDF" flow
                runPdfTextRecognition(pdfUri);
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (cameraPhotoUri != null) {
                runTextRecognition(cameraPhotoUri);
            }
        } else if (requestCode == REQUEST_GALLERY_FOR_NEW_DOC && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                runTextRecognition(selectedImageUri);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(ticker);
        ioExecutor.shutdown();
    }
}