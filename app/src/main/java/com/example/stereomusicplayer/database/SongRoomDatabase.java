package com.example.stereomusicplayer.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.stereomusicplayer.model.Songs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Songs.class}, version = 1, exportSchema = false)
public abstract class SongRoomDatabase extends RoomDatabase {
    private static SongRoomDatabase INSTANCE;

    public abstract SongDao songDao();
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static SongRoomDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            SongRoomDatabase.class,
                            "SongsDatabase")
                    .addCallback(sRoomDatabaseCallback)
                    .fallbackToDestructiveMigration()
                    .build();
        }

        return INSTANCE;
    }

    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            databaseWriteExecutor.execute(() -> {
                // Populate the database in the background.
                // If you want to start with more words, just add them.
                SongDao dao = INSTANCE.songDao();
                dao.deleteAll();

                Songs song = new Songs("100000", "Path", "Artist", "Album", "Duration", "ID", true);
                dao.insert(song);
            });
        }
    };

    public static void destroyInstance() {
        INSTANCE = null;
    }
}
