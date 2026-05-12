package com.docexpiry.tracker.data.worker;

import android.content.Context;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.docexpiry.tracker.domain.model.Document;
import com.docexpiry.tracker.domain.service.NotificationService;
import java.util.concurrent.TimeUnit;

public class NotificationServiceImpl implements NotificationService {
    private final Context context;
    private static final long DAY_MS = 24L * 60 * 60 * 1000;
    private static final int[] THRESHOLDS = new int[]{15, 7, 3, 1};

    public NotificationServiceImpl(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void scheduleForDocument(Document doc) {
        if (doc == null) return;
        long expiry = doc.getExpiryDateMillis();
        long now = System.currentTimeMillis();

        for (int days : THRESHOLDS) {
            long triggerAt = expiry - days * DAY_MS;
            long delay = triggerAt - now;
            if (delay <= 0) continue;

            Data input = new Data.Builder()
                    .putString(NotificationWorker.KEY_DOC_ID, doc.getId())
                    .putString(NotificationWorker.KEY_TITLE, doc.getName())
                    .putInt(NotificationWorker.KEY_DAYS_LEFT, days)
                    .build();

            String unique = "undoc_notify_" + doc.getId() + "_" + days;
            OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(input)
                    .build();

            WorkManager.getInstance(context).enqueueUniqueWork(unique, ExistingWorkPolicy.REPLACE, req);
        }
    }

    @Override
    public void cancelForDocument(String docId) {
        if (docId == null) return;
        for (int days : THRESHOLDS) {
            WorkManager.getInstance(context).cancelUniqueWork("undoc_notify_" + docId + "_" + days);
        }
    }
}
