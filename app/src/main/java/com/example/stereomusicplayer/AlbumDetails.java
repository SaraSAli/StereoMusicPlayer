package com.example.stereomusicplayer;

import static com.example.stereomusicplayer.MainActivity.songFiles;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.stereomusicplayer.adapters.AlbumDetailsAdapter;
import com.example.stereomusicplayer.model.Songs;

import java.io.IOException;
import java.util.ArrayList;

public class AlbumDetails extends AppCompatActivity {

    RecyclerView recyclerView;
    ImageView albumPhoto;
    String albumName;
    ArrayList<Songs> albumSongs = new ArrayList<>();
    AlbumDetailsAdapter albumDetailsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_details);

        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!(albumSongs.size() < 1)){
            albumDetailsAdapter = new AlbumDetailsAdapter(this, albumSongs);
            recyclerView.setAdapter(albumDetailsAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this,
                    LinearLayoutManager.VERTICAL,false));
        }
    }

    void initView(){
        recyclerView = findViewById(R.id.recyclerView);
        albumPhoto = findViewById(R.id.album_art);
        albumName = getIntent().getStringExtra("albumName");
        int j =0;
        for (int i = 0; i < songFiles.size(); i++) {
            if(albumName.equals(songFiles.get(i).getAlbum())){
                albumSongs.add(j++,songFiles.get(i));
            }
        }

        byte[] image = getAlbumArt(albumSongs.get(0).getPath());
        if(image != null)
            Glide.with(this).load(image).into(albumPhoto);
        else
            Glide.with(this).load(R.drawable.album_art).into(albumPhoto);

    }

    private byte[] getAlbumArt(String uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);
        byte[] art = retriever.getEmbeddedPicture();
        try {
            retriever.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return art;
    }
}