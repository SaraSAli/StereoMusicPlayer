package com.example.stereomusicplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import com.example.stereomusicplayer.fragments.AlbumFragment;
import com.example.stereomusicplayer.fragments.ArtistFragment;
import com.example.stereomusicplayer.fragments.SongFragment;
import com.example.stereomusicplayer.model.Songs;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private TabLayout mTablayout;
    private ViewPager2 mViewPager;
    ScreenSlidePagerAdapter pagerAdapter;

    private String[] titles = {"Songs", "Albums", "Artists"};
    public static final int PERMISSION_REQUEST_CODE = 1;

    public static ArrayList<Songs> songFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViewPager();
        verifyPermission();
    }

    private void initViewPager() {
        mTablayout = findViewById(R.id.tab_layout);
        mViewPager = findViewById(R.id.viewPager2);

        pagerAdapter = new ScreenSlidePagerAdapter(this);

        mViewPager.setAdapter(pagerAdapter);
        new TabLayoutMediator(mTablayout,mViewPager,(((tab, position) -> tab.setText(titles[position])))).attach();
    }

    @Override
    public void onBackPressed() {
        if (mViewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
        }
    }

    private void verifyPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            //Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
            songFiles = getAllSongs(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
                songFiles = getAllSongs(this);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    public static ArrayList<Songs> getAllSongs(Context context) {
        ArrayList<Songs> tempSongList = new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ARTIST,
        };
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String album = cursor.getString(0);
                String title = cursor.getString(1);
                String duration = cursor.getString(2);
                String path = cursor.getString(3);
                String artist = cursor.getString(4);

                Songs song = new Songs(path, title, artist, album, duration);
                tempSongList.add(song);
            }
            cursor.close();
        }

        return tempSongList;
    }

    public static class ScreenSlidePagerAdapter extends FragmentStateAdapter {

        private final String[] titles = {"Songs", "Albums", "Artists"};

        public ScreenSlidePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new SongFragment();
                case 1:
                    return new AlbumFragment();
            }
            return new ArtistFragment();
        }

        @Override
        public int getItemCount() {
            return titles.length;
        }
    }
}