package com.docexpiry.tracker.presentation.documents;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.docexpiry.tracker.R;
import com.docexpiry.tracker.domain.model.Document;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DocumentsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<DocumentListItem> items = new ArrayList<>();
    private final ActionListener listener;
    private RecyclerView recyclerView;

    public interface ActionListener {
        void onDelete(Document doc);

        void onClick(Document doc);

        void onImageClick(Document doc);
    }

    public DocumentsListAdapter(ActionListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == DocumentListItem.TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_section_header, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_document, parent, false);
            return new DocumentViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DocumentListItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item.headerTitle);
        } else if (holder instanceof DocumentViewHolder) {
            ((DocumentViewHolder) holder).bind(item.document);
        }
    }

    public void submitList(List<DocumentListItem> newList) {
        List<DocumentListItem> safeNewList = (newList == null) ? new ArrayList<>() : newList;
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffCallback(this.items, safeNewList));
        this.items.clear();
        this.items.addAll(safeNewList);
        diff.dispatchUpdatesTo(this);
    }

    // Called every second from Activity ticker.
    public void updateCountdowns() {
        if (recyclerView == null) return;

        long now = System.currentTimeMillis();
        int childCount = recyclerView.getChildCount();

        for (int i = 0; i < childCount; i++) {
            View child = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(child);
            if (vh instanceof DocumentViewHolder) {
                ((DocumentViewHolder) vh).updateCountdown(now);
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.sectionTitle);
        }

        void bind(String text) {
            title.setText(text == null ? "" : text);
        }
    }

    private class DocumentViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView expiry;
        final TextView issueDate;
        final TextView countdown;
        final TextView percentLabel;
        final TextView categoryIcon;
        final TextView category;
        final TextView docTag;
        final TextView status;
        final ProgressBar progressBar;
        final ImageView ivThumbnail;
        final TextView tvImageSublabel;
        final View addImageContainer;
        Document doc;

        DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_doc_name);
            expiry = itemView.findViewById(R.id.tv_expiry_date);
            issueDate = itemView.findViewById(R.id.tv_issue_date);
            countdown = itemView.findViewById(R.id.tv_countdown);
            percentLabel = itemView.findViewById(R.id.tv_percent);
            categoryIcon = itemView.findViewById(R.id.tv_category_icon);
            category = itemView.findViewById(R.id.tv_category);
            docTag = itemView.findViewById(R.id.tv_doc_tag);
            status = itemView.findViewById(R.id.tv_status);
            progressBar = itemView.findViewById(R.id.progress_bar);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            tvImageSublabel = itemView.findViewById(R.id.tv_image_sublabel);
            addImageContainer = itemView.findViewById(R.id.tv_add_image);

            itemView.setOnClickListener(v -> {
                if (doc != null && listener != null) listener.onClick(doc);
            });

            View deleteBtn = itemView.findViewById(R.id.btn_delete);
            if (deleteBtn != null) {
                deleteBtn.setOnClickListener(v -> {
                    if (doc != null && listener != null) listener.onDelete(doc);
                });
            }

            View imageContainer = itemView.findViewById(R.id.image_container);
            if (imageContainer != null) {
                imageContainer.setOnClickListener(v -> {
                    if (doc != null && listener != null) listener.onImageClick(doc);
                });
            }
        }

        void bind(Document d) {
            this.doc = d;
            title.setText(d.getName() != null ? d.getName() : "Untitled");

            if (d.getIssueDateMillis() > 0) {
                issueDate.setText(
                        android.text.format.DateFormat.getDateFormat(itemView.getContext())
                                .format(d.getIssueDateMillis())
                );
            } else {
                issueDate.setText("-");
            }

            expiry.setText(
                    android.text.format.DateFormat.getDateFormat(itemView.getContext())
                            .format(d.getExpiryDateMillis())
            );

            String cat = d.getCategory() != null ? d.getCategory() : "Other";
            category.setText(cat);
            categoryIcon.setText(cat.isEmpty() ? "?" : cat.substring(0, 1).toUpperCase(Locale.getDefault()));

            if (d.getTag() != null && !d.getTag().isEmpty()) {
                docTag.setVisibility(View.VISIBLE);
                docTag.setText(d.getTag());
            } else {
                docTag.setVisibility(View.GONE);
            }

            if (d.getImagePath() != null && !d.getImagePath().isEmpty()) {
                ivThumbnail.setVisibility(View.VISIBLE);
                addImageContainer.setVisibility(View.GONE);
                tvImageSublabel.setText("Tap to view full image");
                
                Glide.with(itemView.getContext())
                        .load(d.getImagePath())
                        .centerCrop()
                        .into(ivThumbnail);
            } else {
                ivThumbnail.setVisibility(View.GONE);
                addImageContainer.setVisibility(View.VISIBLE);
                tvImageSublabel.setText("Tap to upload or capture");
                Glide.with(itemView.getContext()).clear(ivThumbnail);
            }

            updateCountdown(System.currentTimeMillis());
        }

        // This method is required by adapter.updateCountdowns().
        void updateCountdown(long now) {
            if (doc == null) return;

            long issueMs = doc.getIssueDateMillis();
            long expiryMs = doc.getExpiryDateMillis();

            long total = Math.max(1L, expiryMs - issueMs);
            long remaining = Math.max(0L, expiryMs - now);

            int percent = (int) Math.max(0L, Math.min(100L, (remaining * 100L) / total));
            progressBar.setProgress(percent);
            percentLabel.setText(percent + "%");

            if (remaining <= 0L) {
                countdown.setText("Expired");
                status.setText("Expired");
                return;
            }

            long days = TimeUnit.MILLISECONDS.toDays(remaining);
            long hours = TimeUnit.MILLISECONDS.toHours(remaining) % 24;
            long mins = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60;
            long secs = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60;

            countdown.setText(String.format(
                    Locale.getDefault(),
                    "%dd %02dh %02dm %02ds",
                    days, hours, mins, secs
            ));

            if (days <= 3) {
                status.setText("Urgent");
            } else if (days <= 7) {
                status.setText("Upcoming");
            } else {
                status.setText("");
            }
        }
    }

    private static class DiffCallback extends DiffUtil.Callback {
        private final List<DocumentListItem> oldList;
        private final List<DocumentListItem> newList;

        DiffCallback(List<DocumentListItem> oldList, List<DocumentListItem> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            DocumentListItem a = oldList.get(oldItemPosition);
            DocumentListItem b = newList.get(newItemPosition);

            if (a.type != b.type) return false;

            if (a.type == DocumentListItem.TYPE_HEADER) {
                String ah = a.headerTitle == null ? "" : a.headerTitle;
                String bh = b.headerTitle == null ? "" : b.headerTitle;
                return ah.equals(bh);
            }

            return a.document != null
                    && b.document != null
                    && a.document.getId() != null
                    && a.document.getId().equals(b.document.getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            DocumentListItem a = oldList.get(oldItemPosition);
            DocumentListItem b = newList.get(newItemPosition);
            return a.equals(b);
        }
    }
}