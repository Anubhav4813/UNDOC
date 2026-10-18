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
    private final String pdfPath;

    public Document(
            String id,
            String uid,
            String name,
            String category,
            long issueDateMillis,
            long expiryDateMillis,
            String tag,
            String imagePath,
            String pdfPath
    ) {
        this.id = id;
        this.uid = uid;
        this.name = name;
        this.category = category;
        this.issueDateMillis = issueDateMillis;
        this.expiryDateMillis = expiryDateMillis;
        this.tag = tag;
        this.imagePath = imagePath;
        this.pdfPath = pdfPath;
    }

    public String getId() { return id; }
    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public long getIssueDateMillis() { return issueDateMillis; }
    public long getExpiryDateMillis() { return expiryDateMillis; }
    public String getTag() { return tag; }
    public String getImagePath() { return imagePath; }
    public String getPdfPath() { return pdfPath; }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiryDateMillis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Document)) return false;
        Document document = (Document) o;
        return issueDateMillis == document.issueDateMillis &&
               expiryDateMillis == document.expiryDateMillis &&
               java.util.Objects.equals(id, document.id) &&
               java.util.Objects.equals(uid, document.uid) &&
               java.util.Objects.equals(name, document.name) &&
               java.util.Objects.equals(category, document.category) &&
               java.util.Objects.equals(tag, document.tag) &&
               java.util.Objects.equals(imagePath, document.imagePath) &&
               java.util.Objects.equals(pdfPath, document.pdfPath);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, uid, name, category, issueDateMillis, expiryDateMillis, tag, imagePath, pdfPath);
    }
}