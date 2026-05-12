package com.docexpiry.tracker.data.local.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.docexpiry.tracker.data.local.dao.DocumentDao;
import com.docexpiry.tracker.data.local.entity.DocumentEntity;

@Database(entities = {DocumentEntity.class}, version = 1, exportSchema = false)
public abstract class UndocDatabase extends RoomDatabase {
    private static volatile UndocDatabase instance;

    public abstract DocumentDao documentDao();

    public static UndocDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (UndocDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            UndocDatabase.class,
                            "undoc.db"
                    ).build();
                }
            }
        }
        return instance;
    }
}