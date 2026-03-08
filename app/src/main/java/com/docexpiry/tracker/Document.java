package com.docexpiry.tracker;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class Document implements Serializable {
    private int id;
    private String name;
    private String category;
    private long issueDateMillis;
    private long expiryDateMillis;
    private String imagePath; // local file path for uploaded image
    private String tag;        // vehicle no., owner name, etc.

    public Document(int id, String name, String category, long issueDateMillis, long expiryDateMillis, String imagePath) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.issueDateMillis = issueDateMillis;
        this.expiryDateMillis = expiryDateMillis;
        this.imagePath = imagePath;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public long getIssueDateMillis() { return issueDateMillis; }
    public long getExpiryDateMillis() { return expiryDateMillis; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public int getProgressPercent() {
        long now = System.currentTimeMillis();
        if (now >= expiryDateMillis) return 0;
        if (now <= issueDateMillis) return 100;
        long total = expiryDateMillis - issueDateMillis;
        long remaining = expiryDateMillis - now;
        return (int) ((remaining * 100L) / total);
    }

    public long getRemainingMillis() {
        return Math.max(0, expiryDateMillis - System.currentTimeMillis());
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiryDateMillis;
    }

    public long getRemainingDays() {
        return TimeUnit.MILLISECONDS.toDays(getRemainingMillis());
    }

    public long getRemainingHours() {
        return TimeUnit.MILLISECONDS.toHours(getRemainingMillis()) % 24;
    }

    public long getRemainingMinutes() {
        return TimeUnit.MILLISECONDS.toMinutes(getRemainingMillis()) % 60;
    }

    public long getRemainingSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(getRemainingMillis()) % 60;
    }

    // 0=VALID, 1=WARNING, 2=CRITICAL, 3=EXPIRED
    public int getStatusLevel() {
        if (isExpired()) return 3;
        int p = getProgressPercent();
        if (p > 50) return 0;
        if (p > 20) return 1;
        return 2;
    }

    public String getStatusLabel() {
        String[] labels = {"Valid", "Expiring Soon", "Critical", "Expired"};
        return labels[getStatusLevel()];
    }

    public int getStatusColor() {
        int[] colors = {0xFF2ECC71, 0xFFFFD600, 0xFFFF6B35, 0xFFFF3060};
        return colors[getStatusLevel()];
    }
}
