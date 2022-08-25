package com.example.stereomusicplayer;

import static com.example.stereomusicplayer.MainActivity.songFiles;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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

import java.io.IOException;
import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener, View.OnClickListener {

    private static final String TAG = "MyTag";


    TextView songName, artistName, albumName, durationPlayed, durationTotal;
    ImageView playBtn, nextBtn, prevBtn, shuffleBtn, repeatBtn;
    SeekBar seekBar;

    MediaPlayer mediaPlayer;

    static ArrayList<Songs> songList = new ArrayList<>();

    int position;
    Uri uri;

    boolean mBound;
    MusicService musicService;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) iBinder;
            musicService = binder.getService();
            mBound=true;
            Log.d(TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
            Log.d(TAG, "onServiceDisconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initView();
        getIntentMethod();
        startSong();

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(mediaPlayer != null && b){
                    mediaPlayer.seekTo(i * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        if(mediaPlayer != null){
            int currentPosition = mediaPlayer.getCurrentPosition() / 1000;
            seekBar.setProgress(currentPosition);
            durationPlayed.setText(formattedTime(currentPosition));
        }
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

            int position = getIntent().getIntExtra("position", -1);
            Log.i(TAG, "getPosition: " + position);
            setImage(songList.get(position).getPath(), songList.get(position).getTitle());
            songName.setText(songList.get(position).getTitle());
            artistName.setText(songList.get(position).getArtist());
            albumName.setText(songList.get(position).getAlbum());
        }
    }

    private void initView() {
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        actionBar.setDisplayHomeAsUpEnabled(true);
        songList = songFiles;

        songName = findViewById(R.id.tv_song_name);
        artistName = findViewById(R.id.tv_artist_name);
        albumName = findViewById(R.id.tv_album_name);
        durationPlayed = findViewById(R.id.time_left);
        durationTotal = findViewById(R.id.time_duration);

        playBtn = findViewById(R.id.play);
        nextBtn = findViewById(R.id.next);
        prevBtn = findViewById(R.id.previous);
        shuffleBtn = findViewById(R.id.shuffle);
        repeatBtn = findViewById(R.id.repeat);

        seekBar = findViewById(R.id.seekBar);
        playBtn.setOnClickListener(this);
    }

    private void startSong(){
        position = getIntent().getIntExtra("position",-1);
        songList = songFiles;
        if(songList != null){
            playBtn.setImageResource(R.drawable.ic_pause);
            uri = Uri.parse((songList.get(position).getPath()));
        }
        if(mediaPlayer == null){
            /*mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = MediaPlayer.create(this,uri);
            mediaPlayer.start();*/
            Intent serviceIntent = new Intent(MusicService.class.getName());
            serviceIntent.putExtra("position",position);
            startService(serviceIntent);
        }
        /*else{
            mediaPlayer = MediaPlayer.create(this,uri);
            mediaPlayer.start();

        }*/
    }

    private void setImage(String imageUrl, String songName) {
        Log.d(TAG, "setImage: setting te image and name to widgets.");

        TextView name = findViewById(R.id.tv_song_name);
        name.setText(songName);

        ImageView image = findViewById(R.id.album_art);

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
        if (view.getId() == R.id.play) {
            if(musicService.isPlaying()) {
                if (mediaPlayer != null) {
                    musicService.pause();
                    playBtn.setImageResource(R.drawable.ic_play);
                }
            }else{
                if(mediaPlayer != null){
                    musicService.play();
                    playBtn.setImageResource(R.drawable.ic_pause);
                }
            }

        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

    }
}