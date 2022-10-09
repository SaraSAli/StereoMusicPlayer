package com.example.stereomusicplayer.fragments;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stereomusicplayer.PlayerActivity;
import com.example.stereomusicplayer.R;
import com.example.stereomusicplayer.adapters.FavouriteAdapter;
import com.example.stereomusicplayer.database.SongsViewModel;
import com.example.stereomusicplayer.model.Songs;

import java.util.ArrayList;

public class FavouriteFragment extends Fragment {

    RecyclerView recyclerView;
    FavouriteAdapter favouriteAdapter;
    SongsViewModel viewModel;

    public static final int NEW_SONG_REQUEST_CODE = 1;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(this).get(SongsViewModel.class);
        //viewModel.getSongs().observe(this, favouriteAdapter::setSongs);
        favouriteAdapter = new FavouriteAdapter(getContext(), new ArrayList<>());

        viewModel.getSongs().observe(this, songs -> {
            favouriteAdapter.setSongs(songs);
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_favourite, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(favouriteAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(getContext(), "onActivityResult called", Toast.LENGTH_SHORT).show();
        if (requestCode == NEW_SONG_REQUEST_CODE && resultCode == RESULT_OK) {
            //Songs song = data.getStringExtra(PlayerActivity.EXTRA_SONG);
            Songs myObject = (Songs) data.getSerializableExtra(PlayerActivity.EXTRA_SONG);
            viewModel.insert(myObject);
            Toast.makeText(
                    getContext(),
                    "FavouriteFragment: onActivityResult",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(
                    getContext(),
                    "Empty",
                    Toast.LENGTH_LONG).show();
        }
    }
}