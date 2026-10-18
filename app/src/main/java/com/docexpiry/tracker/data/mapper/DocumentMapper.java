package com.docexpiry.tracker.data.mapper;

import com.docexpiry.tracker.data.local.entity.DocumentEntity;
import com.docexpiry.tracker.domain.model.Document;
import java.util.ArrayList;
import java.util.List;

public class DocumentMapper {

    public Document toDomain(DocumentEntity entity) {
        return new Document(
                entity.id,
                entity.uid,
                entity.name,
                entity.category,
                entity.issueDateMillis,
                entity.expiryDateMillis,
                entity.tag,
                entity.imagePath,
                entity.pdfPath
        );
    }

    public DocumentEntity toEntity(Document document) {
        DocumentEntity entity = new DocumentEntity();
        entity.id = document.getId();
        entity.uid = document.getUid();
        entity.name = document.getName();
        entity.category = document.getCategory();
        entity.issueDateMillis = document.getIssueDateMillis();
        entity.expiryDateMillis = document.getExpiryDateMillis();
        entity.tag = document.getTag();
        entity.imagePath = document.getImagePath();
        entity.pdfPath = document.getPdfPath();
        entity.updatedAt = System.currentTimeMillis();
        entity.dirty = true;
        entity.deleted = false;
        return entity;
    }

    public List<Document> toDomainList(List<DocumentEntity> entities) {
        List<Document> out = new ArrayList<>();
        for (DocumentEntity entity : entities) {
            out.add(toDomain(entity));
        }
        return out;
    }
}