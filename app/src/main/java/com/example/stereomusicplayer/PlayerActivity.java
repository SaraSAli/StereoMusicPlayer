package com.example.stereomusicplayer;

import static com.example.stereomusicplayer.MainActivity.songFiles;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.stereomusicplayer.model.Songs;
import com.example.stereomusicplayer.services.MusicService;
import com.google.android.exoplayer2.ExoPlayer;

import java.io.IOException;
import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener, View.OnClickListener {

    private static final String TAG = "MyTag";


    TextView songName, artistName, albumName, durationPlayed, durationTotal;
    ImageView playBtn, nextBtn, prevBtn, shuffleBtn, repeatBtn;
    SeekBar seekBar;

    static ArrayList<Songs> songList = new ArrayList<>();

    int position;

    boolean isBound;
    MusicService musicService;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) iBinder;
            musicService = binder.getService();
            //PlayerActivity.this.onServiceConnected();
            isBound = true;
            Log.d(TAG, "Service Bound");

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
            Log.d(TAG, "onServiceDisconnected");
        }
    };

    private void playAudio(String media) {
        //Check is service is active
        if (!isBound) {
            Intent playerIntent = new Intent(this, MusicService.class);
            playerIntent.putExtra("media", media);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            playBtn.setBackgroundResource(R.drawable.ic_pause);
        } else {
            //Service is active
            //Send media with BroadcastReceiver
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initView();
        getIntentMethod();
        playAudio(songList.get(position).getPath());


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        /*if(mediaPlayer != null){
            int currentPosition = mediaPlayer.getCurrentPosition() / 1000;
            seekBar.setProgress(currentPosition);
            durationPlayed.setText(formattedTime(currentPosition));
        }*/
    }

    private String formattedTime(int currentPosition) {
        String minutes = String.valueOf(currentPosition / 60);
        String seconds = String.valueOf(currentPosition % 60);

        if(seconds.length() == 1) return minutes + ":0" + seconds;
        else return  minutes + ":" + seconds;
    }

    private void getIntentMethod() {
        Log.d(TAG, "getIncomingIntent: checking for incoming intents.");

        if (getIntent().hasExtra("position")) {
            Log.d(TAG, "getIncomingIntent: found intent extras.");

            position = getIntent().getIntExtra("position", -1);
            Log.i(TAG, "getPosition: " + position);
            setImage(songList.get(position).getPath(), songList.get(position).getTitle());
            songName.setText(songList.get(position).getTitle());
            artistName.setText(songList.get(position).getArtist());
            albumName.setText(songList.get(position).getAlbum());
            Log.i(TAG, "getIntentMethod: Position" + position);
        }
    }

    private void initView() {
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        actionBar.setDisplayHomeAsUpEnabled(true);
        songList = songFiles;

        songName = findViewById(R.id.tv_panel_song_name);
        artistName = findViewById(R.id.tv_panel_artist_name);
        albumName = findViewById(R.id.tv_panel_album_name);
        durationPlayed = findViewById(R.id.tv_pn_remain_time);
        durationTotal = findViewById(R.id.tv_pn_total_time);

        playBtn = findViewById(R.id.iv_pn_play_btn);
        nextBtn = findViewById(R.id.iv_pn_next_btn);
        prevBtn = findViewById(R.id.iv_pn_prev_btn);
        /*shuffleBtn = findViewById(R.id.shuffle);
        repeatBtn = findViewById(R.id.repeat);*/

        seekBar = findViewById(R.id.sb_pn_player);
        playBtn.setOnClickListener(this);
    }

    private void setImage(String imageUrl, String songName) {
        Log.d(TAG, "setImage: setting te image and name to widgets.");

        TextView name = findViewById(R.id.tv_panel_song_name);
        name.setText(songName);

        ImageView image = findViewById(R.id.iv_pn_cover_art_shadow_white_overlay);

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(imageUrl);
        byte[] imageArr = retriever.getEmbeddedPicture();
        try {
            retriever.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (image != null)
            Glide.with(this).asBitmap().load(imageArr).into(image);
        else {
            Glide.with(this).load(R.drawable.ic_album_art).into(image);
        }

        durationTotal.setText(String.valueOf(Integer.parseInt(songList.get(position).getDuration()) / 1000));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.iv_pn_play_btn) {
            if (isBound) {
                if (musicService.isPlaying()) {
                    musicService.pauseMedia();
                    playBtn.setBackgroundResource(R.drawable.ic_play);
                } else {
                    musicService.playMedia();
                    playBtn.setBackgroundResource(R.drawable.ic_pause);
                }
            }
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean("ServiceState", isBound);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isBound = savedInstanceState.getBoolean("ServiceState");
    }

/*    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            //service is active
            musicService.stopSelf();
        }
    }*/
}