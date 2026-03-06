package com.docexpiry.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class DocumentRepository {
    private static final String PREFS_NAME = "doc_expiry_prefs_v2";
    private static final String KEY_DOCS = "documents";

    private final SharedPreferences prefs;

    public DocumentRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<Document> loadAll() {
        List<Document> list = new ArrayList<>();
        String json = prefs.getString(KEY_DOCS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new Document(
                    obj.getInt("id"),
                    obj.getString("name"),
                    obj.optString("category", "Document"),
                    obj.getLong("issueDate"),
                    obj.getLong("expiryDate"),
                    obj.optString("imagePath", null)
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public void saveAll(List<Document> docs) {
        try {
            JSONArray arr = new JSONArray();
            for (Document d : docs) {
                JSONObject obj = new JSONObject();
                obj.put("id", d.getId());
                obj.put("name", d.getName());
                obj.put("category", d.getCategory());
                obj.put("issueDate", d.getIssueDateMillis());
                obj.put("expiryDate", d.getExpiryDateMillis());
                if (d.getImagePath() != null) obj.put("imagePath", d.getImagePath());
                arr.put(obj);
            }
            prefs.edit().putString(KEY_DOCS, arr.toString()).apply();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public int nextId(List<Document> docs) {
        int max = 0;
        for (Document d : docs) if (d.getId() > max) max = d.getId();
        return max + 1;
    }
}
