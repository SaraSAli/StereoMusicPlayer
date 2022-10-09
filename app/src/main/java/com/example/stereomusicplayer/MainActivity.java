package com.example.stereomusicplayer;

import static com.example.stereomusicplayer.fragments.SongFragment.songAdapter;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.stereomusicplayer.fragments.AlbumFragment;
import com.example.stereomusicplayer.fragments.FavouriteFragment;
import com.example.stereomusicplayer.fragments.SongFragment;
import com.example.stereomusicplayer.model.Songs;
import com.example.stereomusicplayer.services.MusicService;
import com.example.stereomusicplayer.utils.StorageUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, View.OnClickListener {

    public static final String MUSIC_LAST_PLAYED = "MUSIC_LAST_PLAYED";
    public static final String MUSIC_FILE = "STORED_MUSIC";
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.stereomusicplayer.services.MusicService.PlayNewAudio";


    public static final String SONG_NAME = "SONG_NAME";
    public static final String ARTIST_NAME = "ARTIST_NAME";
    public static String PATH_TO_FILE = null;
    public static String SONG_NAME_TO_FILE = null;
    public static String ARTIST_NAME_TO_FILE = null;

    int position;

    private static final String TAG = "MainActivity";

    public static final int PERMISSION_REQUEST_CODE = 1;

    public static boolean shuffleBoolean = false;
    public static boolean repeatBoolean = false;
    public static boolean favourite = false;


    private String MY_SORT_PREF = "SortOrder";

    public static ArrayList<Songs> songFiles;
    public static ArrayList<Songs> albums = new ArrayList<>();

    TextView songName, artistName;
    ImageView playBtn, nextBtn, albumArt;

    MusicService musicService;
    private boolean isBound;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyPermission();
        initBottomNav();
        initViews();
    }

    void initViews() {
        songName = findViewById(R.id.song_name_miniPlayer);
        artistName = findViewById(R.id.artist_name_miniPlayer);

        albumArt = findViewById(R.id.bottom_album_art);

        playBtn = findViewById(R.id.play_pause_miniPlayer);
        nextBtn = findViewById(R.id.skip_next_bottom);

        playBtn.setOnClickListener(this);
        nextBtn.setOnClickListener(this);
        albumArt.setOnClickListener(this);
    }

    void initBottomNav() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.NavView);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();
            switch (id) {
                case R.id.nav_song:
                    selectedFragment = new SongFragment();
                    break;
                case R.id.nav_album:
                    selectedFragment = new AlbumFragment();
                    break;
                case R.id.nav_favourite:
                    selectedFragment = new FavouriteFragment();
                    break;
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment,
                            selectedFragment).commit();
            return true;
        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment, new SongFragment()).commit();
    }

    public ArrayList<Songs> getAllSongs(Context context) {
        SharedPreferences preferences = getSharedPreferences(MY_SORT_PREF, MODE_PRIVATE);
        String sortOrder = preferences.getString("sorting", "sortByName");

        ArrayList<String> duplicate = new ArrayList<>();
        ArrayList<Songs> tempSongList = new ArrayList<>();
        albums.clear();

        String order = null;

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        switch (sortOrder){
            case "sortByName":
                order = MediaStore.MediaColumns.DISPLAY_NAME + " ASC";
                break;
            case "sortByDate":
                order = MediaStore.MediaColumns.DATE_ADDED + " ASC";
                break;
            case "sortBySize":
                order = MediaStore.MediaColumns.SIZE + " DESC";
                break;
        }

        String[] projection = {
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID
        };

        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, order);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String album = cursor.getString(0);
                String title = cursor.getString(1);
                String duration = cursor.getString(2);
                String path = cursor.getString(3);
                String artist = cursor.getString(4);
                String id = cursor.getString(5);

                Songs song = new Songs(id, path, title, artist, album, duration, false);
                tempSongList.add(song);

                if (!duplicate.contains(album)) {
                    albums.add(song);
                    duplicate.add(album);
                }
            }
            cursor.close();
        }

        return tempSongList;
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences preferences = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE);
        String path = preferences.getString(MUSIC_FILE, null);
        String songNameStr = preferences.getString(SONG_NAME, null);
        String artistNameStr = preferences.getString(ARTIST_NAME, null);

        if (path != null) {
            songName.setText(songNameStr);
            artistName.setText(artistNameStr);
            setImage(path, albumArt);

            PATH_TO_FILE = path;
            SONG_NAME_TO_FILE = songNameStr;
            ARTIST_NAME_TO_FILE = artistNameStr;
        } else {
            PATH_TO_FILE = null;
            SONG_NAME_TO_FILE = null;
            ARTIST_NAME_TO_FILE = null;
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.play_pause_miniPlayer) {
            if (isBound) {
                if (musicService.isPlaying()) {
                    musicService.pauseMedia();
                    playBtn.setImageResource(R.drawable.ic_play);
                    musicService.buildNotification(PlaybackStatus.PAUSED);
                } else {
                    musicService.playMedia();
                    playBtn.setImageResource(R.drawable.ic_pause);
                    musicService.buildNotification(PlaybackStatus.PLAYING);
                }
            }
        }

        if (view.getId() == R.id.skip_next_bottom) {
            musicService.skipToNext();
            musicService.buildNotification(PlaybackStatus.PLAYING);
        }
        if (view.getId() == R.id.bottom_album_art) {
            StorageUtil storageUtil = new StorageUtil(getApplicationContext());
            position = storageUtil.loadAudioIndex();
            Intent i = new Intent(this, PlayerActivity.class);
            i.putExtra("position", position);
            startActivity(i);
        }
    }


    private void setImage(String imageUrl, ImageView image) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(imageUrl);
        byte[] imageArr = retriever.getEmbeddedPicture();
        try {
            retriever.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (image != null) {
            Glide.with(this).asBitmap().load(imageArr).into(image);
        } else {
            Glide.with(this).load(R.drawable.ic_album_art).into(image);
        }
    }

    private void verifyPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            songFiles = getAllSongs(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                songFiles = getAllSongs(this);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tool_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setOnQueryTextListener(this);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        String userInput = s.toLowerCase();
        ArrayList<Songs> myFiles = new ArrayList<>();
        for (Songs song : songFiles) {
            if (song.getTitle().toLowerCase().contains(userInput)){
                myFiles.add(song);
            }
        }
        songAdapter.updateList(myFiles);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        SharedPreferences.Editor editor = getSharedPreferences(MY_SORT_PREF,MODE_PRIVATE).edit();
        switch (item.getItemId()){
            case R.id.by_name:
                editor.putString("sorting", "sortByName");
                editor.apply();
                this.recreate();
                break;
            case R.id.by_date:
                editor.putString("sorting", "sortByDate");
                editor.apply();
                this.recreate();
                break;
            case R.id.by_size:
                editor.putString("sorting", "sortBySize");
                editor.apply();
                this.recreate();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}