package com.example.stereomusicplayer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.stereomusicplayer.R;
import com.example.stereomusicplayer.model.Songs;

import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<Songs> songsList;

    public SongAdapter(Context mContext, ArrayList<Songs> songsList) {
        this.mContext = mContext;
        this.songsList = songsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.song_item, parent, false);
        return new SongAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.songName.setText(songsList.get(position).getTitle());
        holder.artistName.setText(songsList.get(position).getArtist());
        Glide.with(mContext).load(songsList.get(position).getAlbum()).into(holder.album_art);
    }

    @Override
    public int getItemCount() {
        return songsList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        public TextView songName, artistName;
        public ImageView album_art;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            songName = itemView.findViewById(R.id.tv_song_name);
            artistName = itemView.findViewById(R.id.tv_artist_name);
            album_art = itemView.findViewById(R.id.song_img);
        }
    }
}
