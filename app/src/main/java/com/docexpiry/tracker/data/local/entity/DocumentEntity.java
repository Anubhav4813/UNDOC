package com.docexpiry.tracker.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "documents")
public class DocumentEntity {
    @PrimaryKey
    @NonNull
    public String id;

    public String uid;
    public String name;
    public String category;
    public long issueDateMillis;
    public long expiryDateMillis;
    public String tag;
    public String imagePath;
    public String pdfPath;
    public long updatedAt;
    public boolean dirty;
    public boolean deleted;
}