package com.example.stereomusicplayer.fragments;

import static com.example.stereomusicplayer.MainActivity.songFiles;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.stereomusicplayer.R;
import com.example.stereomusicplayer.adapters.SongAdapter;
import com.example.stereomusicplayer.model.Songs;

import java.util.ArrayList;
import java.util.List;

public class SongFragment extends Fragment {

    public static SongAdapter songAdapter;
    private RecyclerView recyclerView;

    public SongFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_song, container, false);
        recyclerView = view.findViewById(R.id.recyclerView_Songs);
        recyclerView.setHasFixedSize(true);
        int size = songFiles== null ? 0 : songFiles.size();
        if(!(size < 1)){
            songAdapter = new SongAdapter(getContext(), songFiles);
            recyclerView.setAdapter(songAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        }
        return view;
    }
}