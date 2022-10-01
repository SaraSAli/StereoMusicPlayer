package com.example.stereomusicplayer.interfaces;

public interface PlayerInterface {
    void playMedia();
    void stopMedia();
    void pauseMedia();
    void resumeMedia();
    void seekTo(int position);
    boolean isPlaying();
    int getResumePosition();
    int getCurrentPosition();
    int getDuration();
    void skipToNext();
    void skipToPrevious();
}