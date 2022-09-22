package com.example.stereomusicplayer.fragments;

import static com.example.stereomusicplayer.MainActivity.songFiles;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.stereomusicplayer.R;
import com.example.stereomusicplayer.adapters.AlbumAdapter;
import com.example.stereomusicplayer.adapters.SongAdapter;

public class AlbumFragment extends Fragment {

    private RecyclerView recyclerView;
    AlbumAdapter albumAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_album, container, false);
        recyclerView = view.findViewById(R.id.recyclerView_Songs);
        recyclerView.setHasFixedSize(true);
        int size = songFiles== null ? 0 : songFiles.size();
        if(!(size < 1)){
            albumAdapter = new AlbumAdapter(getContext(), songFiles);
            recyclerView.setAdapter(albumAdapter);
            //recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        }
        return view;
    }
}