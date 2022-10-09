package com.example.stereomusicplayer.fragments;

import static com.example.stereomusicplayer.MainActivity.ARTIST_NAME_TO_FILE;
import static com.example.stereomusicplayer.MainActivity.PATH_TO_FILE;
import static com.example.stereomusicplayer.MainActivity.SONG_NAME_TO_FILE;
import static com.example.stereomusicplayer.MainActivity.songFiles;
import static com.example.stereomusicplayer.PlayerActivity.Broadcast_PLAY_NEW_AUDIO;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.stereomusicplayer.PlaybackStatus;
import com.example.stereomusicplayer.R;
import com.example.stereomusicplayer.services.MusicService;
import com.example.stereomusicplayer.utils.StorageUtil;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class NowPlayingFragment extends Fragment implements View.OnClickListener {

    ImageView nextBtn, albumArt;
    TextView songName, artistName;
    FloatingActionButton playPauseBtn;
    View view;

    MusicService musicService;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.stereomusicplayer.services.MusicService.PlayNewAudio";


    private static final String TAG = "NowPlayingFragment";
    private boolean isBound;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) iBinder;
            musicService = binder.getService();
            isBound = true;
            Log.d(TAG, "Service Bound");

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
            Log.d(TAG, "onServiceDisconnected");
        }
    };

    public NowPlayingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_now_playing, container, false);
        initViews(view);

        nextBtn.setOnClickListener(this);
        playPauseBtn.setOnClickListener(this);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
            if (PATH_TO_FILE != null) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(PATH_TO_FILE);
                byte[] imageArr = retriever.getEmbeddedPicture();
                if (imageArr != null)
                    Glide.with(getContext()).asBitmap().load(imageArr).into(albumArt);
                else
                    Glide.with(this).load(R.drawable.ic_album_art).into(albumArt);

                songName.setText(SONG_NAME_TO_FILE);
                artistName.setText(ARTIST_NAME_TO_FILE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getContext() != null) {
            getContext().unbindService(serviceConnection);
        }
    }

    void initViews(View view) {
        artistName = view.findViewById(R.id.artist_name_miniPlayer);
        songName = view.findViewById(R.id.song_name_miniPlayer);
        albumArt = view.findViewById(R.id.bottom_album_art);
        nextBtn = view.findViewById(R.id.skip_next_bottom);
        playPauseBtn = view.findViewById(R.id.play_pause_miniPlayer);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.play_pause_miniPlayer) {
            if (musicService != null) {
                if (musicService.isPlaying()) {
                    musicService.pauseMedia();
                    playPauseBtn.setImageResource(R.drawable.ic_play);
                    musicService.buildNotification(PlaybackStatus.PAUSED);
                } else {
                    musicService.playMedia();
                    playPauseBtn.setImageResource(R.drawable.ic_pause);
                    musicService.buildNotification(PlaybackStatus.PLAYING);
                }
            }
        }

        if (view.getId() == R.id.skip_next_bottom) {
            if (isBound) {
                musicService.skipToNext();
                musicService.buildNotification(PlaybackStatus.PLAYING);
            }
        }
    }
}