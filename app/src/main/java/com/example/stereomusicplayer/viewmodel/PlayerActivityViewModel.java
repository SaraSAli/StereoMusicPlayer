package com.example.stereomusicplayer.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import com.example.stereomusicplayer.services.MusicService;

import java.io.Closeable;

public class PlayerActivityViewModel extends ViewModel {
    MusicService musicService;

    public PlayerActivityViewModel() {
    }


    @Override
    protected void onCleared() {
        super.onCleared();
    }

    public MusicService getMusicService() {
        return musicService;
    }

    public void setMusicService(MusicService musicService) {
        this.musicService = musicService;
    }
}
