package com.example.stereomusicplayer.interfaces;

import com.example.stereomusicplayer.model.Songs;

import java.io.IOException;

public interface PlayerInterface {
    void playMedia();
    void stopMedia();
    void pauseMedia();
    void resumeMedia();
    void seekTo(int position);
    boolean isPlaying();
    long getDuration();
    int getCurrentStreamPosition();

}