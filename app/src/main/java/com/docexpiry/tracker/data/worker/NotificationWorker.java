package com.docexpiry.tracker.data.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class NotificationWorker extends Worker {
    public static final String KEY_DOC_ID = "docId";
    public static final String KEY_TITLE = "title";
    public static final String KEY_DAYS_LEFT = "daysLeft";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data d = getInputData();
        String docId = d.getString(KEY_DOC_ID);
        String title = d.getString(KEY_TITLE);
        int daysLeft = d.getInt(KEY_DAYS_LEFT, 0);

        try {
            NotificationHelper.showExpiryNotification(getApplicationContext(), docId, title, daysLeft);
            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }
}
