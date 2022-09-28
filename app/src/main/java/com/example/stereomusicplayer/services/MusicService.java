package com.example.stereomusicplayer.services;

import static com.example.stereomusicplayer.MainActivity.songFiles;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.stereomusicplayer.PlayerActivity;
import com.example.stereomusicplayer.R;
import com.example.stereomusicplayer.interfaces.PlayerInterface;
import com.example.stereomusicplayer.model.Songs;
import com.example.stereomusicplayer.utils.StorageUtil;

import java.io.IOException;
import java.util.ArrayList;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnInfoListener, PlayerInterface,
        AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "Music Service";
    private MediaPlayer mediaPlayer;
    private String mediaFile;
    Uri uri;

    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    ArrayList<Songs> audioList;
    private int audioIndex = -1;
    private Songs activeAudio;

    int mBuffer = 0;

    //Used to pause/resume MediaPlayer
    private int resumePosition;

    //AudioFocus
    private AudioManager audioManager;

    private final IBinder serviceBinder = new MusicBinder();

    //Handle incoming phone calls
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    @Override
    public void onCreate() {
        super.onCreate();

        audioList = songFiles;
        register_playNewAudio();
        Toast.makeText(this, "Service started...", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "onCreate() , service started...");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            //Load data from SharedPreferences
            mediaFile = intent.getExtras().getString("media");
            uri = Uri.parse(mediaFile);
            //audioIndex = Integer.parseInt(intent.getExtras().getString("position"));

            StorageUtil storage = new StorageUtil(getApplicationContext());
            audioList = storage.loadAudio();
            audioIndex = storage.loadAudioIndex();
            Log.d(TAG, "onStartCommand: This is audioIndex: "+audioIndex);

            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                activeAudio = audioList.get(audioIndex);
            } else {
                //stopSelf();
                activeAudio = audioList.get(0);
            }
        } catch (NullPointerException e) {
            stopSelf();
        }

        //Request audio focus
        if (!requestAudioFocus()) {
            //Could not gain focus
            stopSelf();
        }

        if (mediaSessionManager == null) {
            //initMediaSession();
            initMediaPlayer();
            //buildNotification(PlaybackStatus.PLAYING);
        }

        //Handle Intent action from MediaSession.TransportControls
        //handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mediaSession.release();
        //removeNotification();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        stopSelf();
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }

        mediaPlayer = null;

        removeAudioFocus();
        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }


        //unregister BroadcastReceivers
        //unregisterReceiver(becomingNoisyReceiver);
        //unregisterReceiver(playNewAudio);

        //clear cached playlist
        //new StorageUtil(getApplicationContext()).clearCachedAudioPlaylist();

        Toast.makeText(this, "Service stopped...", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "onDestroy() , service stopped...");
    }

    public void skipToNext() {
        Log.i(TAG, "skipToNext: Before audioIndex: "+audioIndex);
        if (audioIndex == audioList.size() - 1) {
            //if last in playlist
            audioIndex = 0;
            activeAudio = audioList.get(audioIndex);
        } else {
            //get next in playlist
            activeAudio = audioList.get(++audioIndex);
        }
        Log.i(TAG, "skipToNext: After audioIndex: "+audioIndex);

        //Update stored index
        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);


        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
        //updateMetaData();
    }

    public void skipToPrevious() {

        if (audioIndex == 0) {
            //if first in playlist
            //set index to the last of audioList
            audioIndex = audioList.size() - 1;
            activeAudio = audioList.get(audioIndex);
        } else {
            //get previous in playlist
            activeAudio = audioList.get(--audioIndex);
        }

        //Update stored index
        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);

        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
        //updateMetaData();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        /*//Invoked when playback of a media source has completed.
        stopMedia();
        //stop the service
        stopSelf();*/
        skipToNext();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        //Invoked when there has been an error during an asynchronous operation
        switch (i) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + i1);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + i1);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + i1);
                break;
        }
        return false;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        mBuffer = percent;

    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        //Invoked when the media source is ready for playback.
        playMedia();
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {

    }

    @Override
    public void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            //isPlaying.setValue(true);
        }
    }

    @Override
    public void stopMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    @Override
    public void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
        }
    }

    @Override
    public void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            seekTo(getResumePosition());
            mediaPlayer.start();
        }
    }

    @Override
    public void seekTo(int position) {
        mediaPlayer.seekTo(position);
        Log.d(TAG, "seekTo: position: "+ position);
    }

    @Override
    public boolean isPlaying() {
        if (mediaPlayer != null)
            return mediaPlayer.isPlaying();
        return false;
    }

    @Override
    public int getResumePosition() {
        return resumePosition;
    }


    @Override
    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        //Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            // Set the data source to the mediaFile location
            //mediaPlayer.setDataSource(mediaFile);
            mediaPlayer.setDataSource(activeAudio.getPath());
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind()");
        return serviceBinder;
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopService(new Intent(this, MusicService.class));
    }


    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //Get the new media index form SharedPreferences
            audioIndex = new StorageUtil(getApplicationContext()).loadAudioIndex();
            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                activeAudio = audioList.get(audioIndex);
            } else {
                //stopSelf();
                activeAudio = audioList.get(0);
            }

            //A PLAY_NEW_AUDIO action received
            //reset mediaPlayer to play the new Audio
            stopMedia();
            mediaPlayer.reset();
            initMediaPlayer();
            //updateMetaData();
        }
    };



    @Override
    public void onAudioFocusChange(int focusState) {

        //Invoked when the audio focus of the system is updated.
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) mediaPlayer.start();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }

    private void register_playNewAudio() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(PlayerActivity.Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio, filter);
    }

    public Songs getActiveAudio() {
        return activeAudio != null ? activeAudio : audioList.get(0);
    }
}