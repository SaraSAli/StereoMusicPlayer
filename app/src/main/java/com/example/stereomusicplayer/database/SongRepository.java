package com.example.stereomusicplayer.database;

import static com.example.stereomusicplayer.database.SongRoomDatabase.databaseWriteExecutor;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.stereomusicplayer.model.Songs;

import java.util.List;

public class SongRepository {

    private SongDao songDao;
    private LiveData<List<Songs>> mAllSongs;

    SongRepository(Application application){
        SongRoomDatabase database = SongRoomDatabase.getInstance(application);
        songDao = database.songDao();
        mAllSongs = songDao.getAllSongs();
    }

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    LiveData<List<Songs>> getAllSongs() {
        return mAllSongs;
    }

    void insert(Songs song) {
        databaseWriteExecutor.execute(() -> {
            songDao.insert(song);
        });
    }

    void delete(Songs song){
        databaseWriteExecutor.execute(()->{
            songDao.delete(song);
        });
    }
}
