package com.docexpiry.tracker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.VH> {

    public interface OnItemClickListener {
        void onDelete(int position);
        void onImageClick(int position);
        void onCardClick(int position);
    }

    private final List<Document> docs;
    private final OnItemClickListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public DocumentAdapter(List<Document> docs, OnItemClickListener listener) {
        this.docs = docs;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_document, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Document doc = docs.get(position);

        // Name and category
        h.tvName.setText(doc.getName());
        h.tvCategory.setText(doc.getCategory());
        h.tvCategoryIcon.setText(getCategoryEmoji(doc.getCategory()));

        // Dates - just the formatted date, no "Issued" or "Expires" prefix
        h.tvIssue.setText(sdf.format(new Date(doc.getIssueDateMillis())));
        h.tvExpiry.setText(sdf.format(new Date(doc.getExpiryDateMillis())));

        // Status badge
        h.tvStatus.setText(doc.getStatusLabel().toUpperCase());
        h.tvStatus.setTextColor(doc.getStatusColor());
        h.tvStatus.setBackgroundTintList(
            ColorStateList.valueOf((doc.getStatusColor() & 0x00FFFFFF) | 0x22000000));

        // Progress and countdown (shared with tick updates)
        bindLiveData(h, doc);

        // Image
        bindImage(h, doc);

        // Click listeners
        h.imageContainer.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) listener.onImageClick(pos);
        });
        h.btnDelete.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) listener.onDelete(pos);
        });
        h.itemView.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) listener.onCardClick(pos);
        });
    }

    // Partial update every second — only progress + countdown, no full rebind
    @Override
    public void onBindViewHolder(@NonNull VH h, int pos, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && "tick".equals(payloads.get(0))) {
            bindLiveData(h, docs.get(pos));
        } else {
            onBindViewHolder(h, pos);
        }
    }

    private void bindLiveData(VH h, Document doc) {
        // Progress bar
        int progress = doc.getProgressPercent();
        h.progressBar.setMax(100);
        h.progressBar.setProgress(progress);
        h.progressBar.setProgressTintList(ColorStateList.valueOf(doc.getStatusColor()));
        h.tvPercent.setText(progress + "%");
        h.tvPercent.setTextColor(doc.getStatusColor());

        // Countdown
        if (doc.isExpired()) {
            h.tvCountdown.setText("This document has expired");
            h.tvCountdown.setTextColor(0xFFFF3060);
        } else {
            h.tvCountdown.setText(String.format(Locale.getDefault(),
                "%02d days  %02d hrs  %02d min  %02d sec",
                doc.getRemainingDays(),
                doc.getRemainingHours(),
                doc.getRemainingMinutes(),
                doc.getRemainingSeconds()));
            h.tvCountdown.setTextColor(0xFF374151);
        }
    }

    private void bindImage(VH h, Document doc) {
        if (doc.getImagePath() != null && !doc.getImagePath().isEmpty()) {
            try {
                Bitmap bmp = BitmapFactory.decodeFile(doc.getImagePath());
                if (bmp != null) {
                    h.ivThumbnail.setImageBitmap(bmp);
                    h.ivThumbnail.setVisibility(View.VISIBLE);
                    h.tvAddImage.setVisibility(View.GONE);
                    return;
                }
            } catch (Exception ignored) {}
        }
        h.ivThumbnail.setVisibility(View.GONE);
        h.tvAddImage.setVisibility(View.VISIBLE);
    }

    public void updateCountdowns() {
        notifyItemRangeChanged(0, docs.size(), "tick");
    }

    private String getCategoryEmoji(String category) {
        if (category == null) return "Doc";
        switch (category) {
            case "Vehicle":   return "Car";
            case "Identity":  return "ID";
            case "Insurance": return "Ins";
            case "Medical":   return "Med";
            case "Travel":    return "Air";
            case "Education": return "Edu";
            case "Property":  return "Prop";
            default:          return "Doc";
        }
    }

    @Override public int getItemCount() { return docs.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvCategory, tvCategoryIcon, tvIssue, tvExpiry;
        TextView tvStatus, tvCountdown, tvPercent;
        android.widget.LinearLayout tvAddImage;
        ProgressBar progressBar;
        View btnDelete, imageContainer;
        ImageView ivThumbnail;

        VH(View v) {
            super(v);
            tvName         = v.findViewById(R.id.tv_doc_name);
            tvCategory     = v.findViewById(R.id.tv_category);
            tvCategoryIcon = v.findViewById(R.id.tv_category_icon);
            tvIssue        = v.findViewById(R.id.tv_issue_date);
            tvExpiry       = v.findViewById(R.id.tv_expiry_date);
            tvStatus       = v.findViewById(R.id.tv_status);
            tvCountdown    = v.findViewById(R.id.tv_countdown);
            tvPercent      = v.findViewById(R.id.tv_percent);
            tvAddImage     = v.findViewById(R.id.tv_add_image);
            progressBar    = v.findViewById(R.id.progress_bar);
            btnDelete      = v.findViewById(R.id.btn_delete);
            imageContainer = v.findViewById(R.id.image_container);
            ivThumbnail    = v.findViewById(R.id.iv_thumbnail);
        }
    }
}
