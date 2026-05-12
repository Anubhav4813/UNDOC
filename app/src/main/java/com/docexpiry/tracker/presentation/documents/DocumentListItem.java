package com.docexpiry.tracker.presentation.documents;

import com.docexpiry.tracker.domain.model.Document;
import java.util.Objects;

public class DocumentListItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_DOCUMENT = 1;

    public final int type;
    public final String headerTitle;
    public final Document document;

    private DocumentListItem(int type, String headerTitle, Document document) {
        this.type = type;
        this.headerTitle = headerTitle;
        this.document = document;
    }

    public static DocumentListItem header(String title) {
        return new DocumentListItem(TYPE_HEADER, title, null);
    }

    public static DocumentListItem document(Document doc) {
        return new DocumentListItem(TYPE_DOCUMENT, null, doc);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DocumentListItem)) return false;
        DocumentListItem other = (DocumentListItem) o;
        return type == other.type &&
               Objects.equals(headerTitle, other.headerTitle) &&
               Objects.equals(document != null ? document.getId() : null,
                              other.document != null ? other.document.getId() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, headerTitle, document != null ? document.getId() : null);
    }
}