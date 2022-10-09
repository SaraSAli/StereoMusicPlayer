package com.example.stereomusicplayer;

import static com.example.stereomusicplayer.MainActivity.repeatBoolean;
import static com.example.stereomusicplayer.MainActivity.shuffleBoolean;
import static com.example.stereomusicplayer.MainActivity.songFiles;
import static com.example.stereomusicplayer.adapters.AlbumDetailsAdapter.albumFiles;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.stereomusicplayer.database.SongRoomDatabase;
import com.example.stereomusicplayer.fragments.FavouriteFragment;
import com.example.stereomusicplayer.model.Songs;
import com.example.stereomusicplayer.services.MusicService;
import com.example.stereomusicplayer.utils.StorageUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String EXTRA_SONG= "com.example.android.wordlistsql.SONG";
    public static final String EXTRA_NAME = "com.example.stereomusicplayer.NAME";
    public static final String EXTRA_ALBUM = "com.example.stereomusicplayer.ALBUM";
    public static final String EXTRA_ARTIST = "com.example.stereomusicplayer.ARTIST";
    private static final String TAG = "PlayerActivity";
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.stereomusicplayer.services.MusicService.PlayNewAudio";
    public static final String Broadcast_UPDATE_AUDIO_METADATA = "com.example.stereomusicplayer.UpdateMetadata";


    TextView songName, artistName, albumName, durationPlayed, durationTotal;
    ImageButton playBtn, nextBtn, prevBtn;
    ImageView shuffleBtn, repeatBtn, addToFavourite, openMenu;
    SeekBar seekBar;
    FrameLayout myFramelayout;

    static ArrayList<Songs> songList = new ArrayList<>();

    int position;
    int audioIndex;
    Songs activeAudio;

    boolean isBound;

    SongRoomDatabase database;
    MusicService musicService;
    Runnable runnable;
    Handler handler = new Handler();

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

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initView();
        getIntentMethod();
        playAudio(songList.get(position).getPath(), position);
        setAnimation();

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
                if (isBound && musicService.isPlaying()) {
                    //seekBar.setProgress(musicService.getCurrentPosition() / 1000);
                    int mCurrentPosition = musicService.getCurrentPosition();
                    seekBar.setProgress(mCurrentPosition / 1000);
                    durationPlayed.setText(formatDuration(mCurrentPosition));

                    int totalDuration = musicService.getDuration();
                    if (seekBar.getProgress() == totalDuration) updateMetaData();

                }
                handler.postDelayed(this, 1000);
            }
        };
        runnable.run();

        register_playNextAudio();
        register_updateMetadata();
        register_updateButton();
    }

    @Override
    protected void onResume() {
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        Toast.makeText(this, "onResume: + index " + position, Toast.LENGTH_SHORT).show();
        getIntentMethod();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        /*unbindService(serviceConnection);
        Toast.makeText(this, "onPause: unbind + index "+ position, Toast.LENGTH_SHORT).show();*/
    }

    private void playAudio(String media, int index) {
        //Check is service is active
        if (!isBound) {
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio(songList);
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
        if (getIntent().hasExtra("position")) {
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

        if (getSender != null && getSender.equals("albumDetails"))
            songList = albumFiles;
        else if (getSender != null && getSender.equals("favourites")) {
            //Do something
        } else
            songList = songFiles;

        myFramelayout = findViewById(R.id.container);

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
        addToFavourite = findViewById(R.id.add_to_playlist);
        openMenu = findViewById(R.id.open_menu);

        seekBar = findViewById(R.id.seekBar);
        playBtn.setOnClickListener(this);
        nextBtn.setOnClickListener(this);
        prevBtn.setOnClickListener(this);
        repeatBtn.setOnClickListener(this);
        shuffleBtn.setOnClickListener(this);
        addToFavourite.setOnClickListener(this);
        openMenu.setOnClickListener(this);
    }

    private void setImage(String imageUrl, String songName) {
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

    private BroadcastReceiver playNextAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            musicService.skipToNext();
            updateMetaData();
        }
    };

    private BroadcastReceiver updateMetadata = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateMetaData();
        }
    };

    private BroadcastReceiver updateButton = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle args = intent.getBundleExtra("DATA");
            PlaybackStatus playbackStatus = (PlaybackStatus) args.getSerializable("Action");
            if(playbackStatus == PlaybackStatus.PAUSED){
                playBtn.setBackgroundResource(R.drawable.ic_play);
            }else if(playbackStatus == PlaybackStatus.PLAYING){
                playBtn.setBackgroundResource(R.drawable.ic_pause);
            }
            //updateMetaData();
        }
    };

    private void register_playNextAudio() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(MusicService.Broadcast_PLAY_NEXT_AUDIO);
        registerReceiver(playNextAudio, filter);
    }

    private void register_updateMetadata(){

        IntentFilter filter = new IntentFilter(MusicService.Broadcast_UPDATE_AUDIO_METADATA);
        registerReceiver(updateMetadata, filter);
    }

    private void register_updateButton(){

        IntentFilter filter = new IntentFilter(MusicService.Broadcast_UPDATE_BUTTON);
        registerReceiver(updateButton, filter);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.play) {
            if (isBound) {
                if (musicService.isPlaying()) {
                    musicService.pauseMedia();
                    playBtn.setBackgroundResource(R.drawable.ic_play);
                    musicService.buildNotification(PlaybackStatus.PAUSED);
                } else {
                    musicService.playMedia();
                    playBtn.setBackgroundResource(R.drawable.ic_pause);
                    musicService.buildNotification(PlaybackStatus.PLAYING);
                }
            }
        }
        if (view.getId() == R.id.next) {
            musicService.skipToNext();
            //getIntentMethod();
            updateMetaData();
            musicService.buildNotification(PlaybackStatus.PLAYING);
        }
        if (view.getId() == R.id.previous) {
            musicService.skipToPrevious();
            //getIntentMethod();
            updateMetaData();
            musicService.buildNotification(PlaybackStatus.PLAYING);
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
        if (view.getId() == R.id.add_to_playlist) {
            if (songList.get(position).isFavourite()) {
                addToFavourite.setImageResource(R.drawable.add_to_playlist);
                songList.get(position).setFavourite(false);
                Songs song = songList.get(position);
                database = SongRoomDatabase.getInstance(getApplicationContext());
                database.songDao().delete(song);
                //favouriteFiles.remove(position);
            } else {
                addToFavourite.setImageResource(R.drawable.added_to_playlist);
                songList.get(position).setFavourite(true);
                addToFavourite();
            }
        }
        if (view.getId() == R.id.open_menu) {
            myFramelayout.setVisibility(View.VISIBLE);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new FavouriteFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }

    void addToFavourite() {
        Songs song = new Songs(String.valueOf(position)
                , songList.get(position).getTitle()
                , songList.get(position).getArtist()
                , songList.get(position).getAlbum()
                , songList.get(position).getDuration()
                , songList.get(position).getPath()
                , position);

        /*database = SongRoomDatabase.getInstance(getApplicationContext());
        database.songDao().insert(song);*/

        Intent replyIntent = new Intent();
        replyIntent.putExtra(EXTRA_SONG, song);
        setResult(RESULT_OK, replyIntent);
        //finish();
    }

    public void setAnimation() {
        Slide slide = new Slide();
        slide.setSlideEdge(Gravity.TOP);
        slide.setDuration(400);
        slide.setInterpolator(new AccelerateDecelerateInterpolator());
        getWindow().setExitTransition(slide);
        getWindow().setEnterTransition(slide);
    }

    @Override
    public void onBackPressed() {
        // If the user is currently looking at the first step, allow the system to handle the
        // Back button. This calls finish() on this activity and pops the back stack.
        super.onBackPressed();
        finish();
    }
}