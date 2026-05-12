package com.docexpiry.tracker.domain.model;

public class Document {
    private final String id;
    private final String uid;
    private final String name;
    private final String category;
    private final long issueDateMillis;
    private final long expiryDateMillis;
    private final String tag;
    private final String imagePath;

    public Document(
            String id,
            String uid,
            String name,
            String category,
            long issueDateMillis,
            long expiryDateMillis,
            String tag,
            String imagePath
    ) {
        this.id = id;
        this.uid = uid;
        this.name = name;
        this.category = category;
        this.issueDateMillis = issueDateMillis;
        this.expiryDateMillis = expiryDateMillis;
        this.tag = tag;
        this.imagePath = imagePath;
    }

    public String getId() { return id; }
    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public long getIssueDateMillis() { return issueDateMillis; }
    public long getExpiryDateMillis() { return expiryDateMillis; }
    public String getTag() { return tag; }
    public String getImagePath() { return imagePath; }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiryDateMillis;
    }
}