package com.example.stereomusicplayer;

import static com.example.stereomusicplayer.MainActivity.songFiles;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.stereomusicplayer.model.Songs;
import com.example.stereomusicplayer.services.MusicService;
import com.example.stereomusicplayer.utils.StorageUtil;
import com.example.stereomusicplayer.viewmodel.PlayerActivityViewModel;

import java.io.IOException;
import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener, View.OnClickListener {

    private static final String TAG = "MyTag";
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.stereomusicplayer.services.MusicService.PlayNewAudio";


    TextView songName, artistName, albumName, durationPlayed, durationTotal;
    ImageButton playBtn, nextBtn, prevBtn, shuffleBtn, repeatBtn;
    SeekBar seekBar;

    boolean shuffleBoolean, repeatBoolean;

    PlayerActivityViewModel viewModel;

    static ArrayList<Songs> songList = new ArrayList<>();

    int position;
    int audioIndex;

    boolean isBound;
    MusicService musicService;
    Handler mHandler;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initView();
        getIntentMethod();
        playAudio(songList.get(position).getPath(), position);


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
    }

    private void playAudio(String media, int index) {
        //Check is service is active
        if (!isBound) {
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio(songList);
            storage.storeAudioIndex(position);

            Intent playerIntent = new Intent(this, MusicService.class);
            playerIntent.putExtra("media", media);
            playerIntent.putExtra("position", index);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            playBtn.setBackgroundResource(R.drawable.ic_pause);
        } else {
            //Store the new audioIndex to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudioIndex(position);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }

    private String formattedTime(int currentPosition) {
        String minutes = String.valueOf(currentPosition / 60);
        String seconds = String.valueOf(currentPosition % 60);

        if (seconds.length() == 1) return minutes + ":0" + seconds;
        else return minutes + ":" + seconds;
    }

    private void getIntentMethod() {
        Log.d(TAG, "getIncomingIntent: checking for incoming intents.");

        if (getIntent().hasExtra("position")) {
            Log.d(TAG, "getIncomingIntent: found intent extras.");

            position = getIntent().getIntExtra("position", -1);

            StorageUtil storage = new StorageUtil(getApplicationContext());
            //audioList = storage.loadAudio();
            audioIndex = storage.loadAudioIndex();
            Log.d(TAG, "getIntentMethod: This is audioIndex: "+audioIndex);

            Log.i(TAG, "getPosition: " + position);
            setImage(songList.get(audioIndex).getPath(), songList.get(audioIndex).getTitle());
            songName.setText(songList.get(audioIndex).getTitle());
            artistName.setText(songList.get(audioIndex).getArtist());
            albumName.setText(songList.get(audioIndex).getAlbum());
            Log.i(TAG, "getIntentMethod: Position " + position);
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
        nextBtn.setOnClickListener(this);
        prevBtn.setOnClickListener(this);
        repeatBtn.setOnClickListener(this);
        shuffleBtn.setOnClickListener(this);
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
        String totalDuration = String.valueOf(Integer.parseInt(songList.get(position).getDuration()) / 1000);
        durationTotal.setText(totalDuration);
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
        if (view.getId() == R.id.next) {
            musicService.skipToNext();
            getIntentMethod();
        }
        if (view.getId() == R.id.previous) {
            musicService.skipToPrevious();
            getIntentMethod();
        }
        if (view.getId() == R.id.shuffle) {
            if (shuffleBoolean) {
                shuffleBoolean = false;
                shuffleBtn.setImageResource(R.drawable.ic_shuffle_off);
            } else {
                shuffleBoolean = true;
                shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
            }
        }
        if (view.getId() == R.id.repeat) {
            if (repeatBoolean) {
                repeatBoolean = false;
                repeatBtn.setImageResource(R.drawable.ic_repeat_off);
            } else {
                repeatBoolean = true;
                repeatBtn.setImageResource(R.drawable.ic_repeat_on);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            //service is active
            musicService.stopSelf();
        }
    }

    @Override
    public void onBackPressed() {

        // If the user is currently looking at the first step, allow the system to handle the
        // Back button. This calls finish() on this activity and pops the back stack.
        super.onBackPressed();
        finish();

    }
}