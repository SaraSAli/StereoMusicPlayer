package com.example.stereomusicplayer.database;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.stereomusicplayer.model.Songs;

import java.util.List;

public class SongsViewModel extends AndroidViewModel {

    private final LiveData<List<Songs>> songsList;
    private SongRepository songRepository;


    public SongsViewModel(@NonNull Application application) {
        super(application);
        /*songsList = SongRoomDatabase
                .getInstance(getApplication())
                .songDao()
                .getAllSongs();*/
        songRepository = new SongRepository(application);
        songsList = songRepository.getAllSongs();
    }

    public LiveData<List<Songs>> getSongs() {
        return songsList;
    }

    public void insert(Songs song) {
        songRepository.insert(song);
    }
}
