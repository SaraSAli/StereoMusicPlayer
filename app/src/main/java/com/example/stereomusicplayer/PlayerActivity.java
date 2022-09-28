package com.example.stereomusicplayer;

import static com.example.stereomusicplayer.MainActivity.repeatBoolean;
import static com.example.stereomusicplayer.MainActivity.shuffleBoolean;
import static com.example.stereomusicplayer.MainActivity.songFiles;
import static com.example.stereomusicplayer.adapters.AlbumDetailsAdapter.albumFiles;

import android.annotation.SuppressLint;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

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
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener, View.OnClickListener {

    private static final String TAG = "PlayerActivity";
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.stereomusicplayer.services.MusicService.PlayNewAudio";


    TextView songName, artistName, albumName, durationPlayed, durationTotal;
    ImageButton playBtn, nextBtn, prevBtn, shuffleBtn, repeatBtn;
    SeekBar seekBar;

    PlayerActivityViewModel viewModel;

    static ArrayList<Songs> songList = new ArrayList<>();

    int position;
    int audioIndex;
    Songs activeAudio;

    boolean isBound;
    MusicService musicService;
    Runnable runnable;
    Handler handler = new Handler();

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
                if (b) {
                    if (isBound && musicService.isPlaying()) {
                        //musicService.seekTo((i * musicService.getDuration()) / 100);
                        musicService.seekTo(i * 1000);
                    }
                }
                int duration = musicService.getCurrentPosition();
                String time = formatDuration(duration);
                durationPlayed.setText(time);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        runnable = new Runnable() {
            @Override
            public void run() {
                if (isBound) {
                    Log.d(TAG, "run: musicService getCurrentPosition: " + musicService.getCurrentPosition()/1000);
                    //seekBar.setProgress(musicService.getCurrentPosition() / 1000);
                    int mCurrentPosition = musicService.getCurrentPosition();
                    seekBar.setProgress(mCurrentPosition/1000);
                    durationPlayed.setText(formatDuration(mCurrentPosition));

                    int totalDuration = musicService.getDuration();
                    if(seekBar.getProgress() == totalDuration) updateMetaData();

                }
                handler.postDelayed(this, 1000);
            }
        };
        runnable.run();
    }

    private void playAudio(String media, int index) {
        //Check is service is active
        if (!isBound) {
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio(songList);
            //storage.storeAudioIndex(audioIndex);
            storage.storeAudioIndex(index);

            Intent playerIntent = new Intent(this, MusicService.class);
            playerIntent.putExtra("media", media);
            playerIntent.putExtra("position", index);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            playBtn.setBackgroundResource(R.drawable.ic_pause);
        } else {
            //Store the new audioIndex to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudioIndex(index);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }


    @SuppressLint("DefaultLocale")
    private String formatDuration(int duration) {
        long minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS);
        long seconds = TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS)
                - minutes * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES);

        return String.format("%02d:%02d", minutes, seconds);
    }

    private void getIntentMethod() {
        Log.d(TAG, "getIncomingIntent: checking for incoming intents.");

        if (getIntent().hasExtra("position")) {
            Log.d(TAG, "getIncomingIntent: found intent extras.");

            position = getIntent().getIntExtra("position", -1);

            StorageUtil storage = new StorageUtil(getApplicationContext());
            songList = storage.loadAudio();
            audioIndex = storage.loadAudioIndex();
            Log.d(TAG, "getIntentMethod: This is audioIndex: "+audioIndex);

            Log.i(TAG, "getPosition: " + position);
            setImage(songList.get(position).getPath(), songList.get(position).getTitle());
            songName.setText(songList.get(position).getTitle());
            artistName.setText(songList.get(position).getArtist());
            albumName.setText(songList.get(position).getAlbum());

            int duration = Integer.parseInt(songList.get(position).getDuration());
            String totalDuration = formatDuration(duration);
            durationTotal.setText(totalDuration);
            Log.i(TAG, "getIntentMethod: Position " + position);
        }
        seekBar.setMax(Integer.parseInt(songList.get(position).getDuration())/1000);
    }

    void updateMetaData(){

        activeAudio = musicService.getActiveAudio();

        setImage(activeAudio.getPath(), activeAudio.getTitle());


        songName.setText(activeAudio.getTitle());
        artistName.setText(activeAudio.getArtist());
        albumName.setText(activeAudio.getAlbum());

        int duration = Integer.parseInt(activeAudio.getDuration());
        String totalDuration = formatDuration(duration);
        durationTotal.setText(totalDuration);
    }

    private void initView() {
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        actionBar.setDisplayHomeAsUpEnabled(true);

        String getSender = getIntent().getStringExtra("sender");

        if(getSender != null && getSender.equals("albumDetails"))
            songList = albumFiles;
        else
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
        if (image != null) {
            imageAnimation(this, image, imageArr);
            //Glide.with(this).asBitmap().load(imageArr).into(image);
        }

        else {
            Glide.with(this).load(R.drawable.ic_album_art).into(image);
        }
    }

    void imageAnimation(Context context, ImageView imageView, byte[] bitmap){
        Animation animationOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
        Animation animationIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);

        animationOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Glide.with(context).asBitmap().load(bitmap).into(imageView);
                animationIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                imageView.startAnimation(animationIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        imageView.startAnimation(animationOut);
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
            //getIntentMethod();
            updateMetaData();
        }
        if (view.getId() == R.id.previous) {
            musicService.skipToPrevious();
            //getIntentMethod();
            updateMetaData();
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