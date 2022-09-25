package com.example.stereomusicplayer.adapters;

import static android.content.Context.BIND_AUTO_CREATE;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.stereomusicplayer.MainActivity;
import com.example.stereomusicplayer.PlayerActivity;
import com.example.stereomusicplayer.R;
import com.example.stereomusicplayer.model.Songs;
import com.example.stereomusicplayer.services.MusicService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {

    private static final String TAG = "Song Adapter";
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
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.songName.setText(songsList.get(position).getTitle());
        holder.artistName.setText(songsList.get(position).getArtist());
        byte[] image = getAlbumArt(songsList.get(position).getPath());
        if (image != null)
            Glide.with(mContext).asBitmap().load(image).into(holder.album_art);
        else
            Glide.with(mContext).load(R.drawable.ic_album_art).into(holder.album_art);

        holder.parentLayout.setOnClickListener(view -> {
            Log.d(TAG, "onClick: clicked on: " + songsList.get(position));
            Toast.makeText(mContext, songsList.get(position).toString(), Toast.LENGTH_SHORT).show();

            Intent i = new Intent(mContext, PlayerActivity.class);
            i.putExtra("position", position);
            mContext.startActivity(i);
        });

        holder.moreMenu.setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(mContext,view);
            popupMenu.getMenuInflater().inflate(R.menu.popup, popupMenu.getMenu());
            popupMenu.show();
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                switch (menuItem.getItemId()){
                    case R.id.delete:
                        Toast.makeText(mContext, "Delete Clicked", Toast.LENGTH_SHORT).show();
                        //deleteFile(holder.getAdapterPosition(), view);
                        deleteFile(position, view);
                        break;
                }
                return true;
            });
        });
    }

    private void deleteFile(final int position, View view){
        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                Long.parseLong(songsList.get(position).getId()));

        File file = new File(songsList.get(position).getPath());
        boolean deleted = file.delete();
        if(deleted) {
            mContext.getContentResolver().delete(contentUri, null, null);
            songsList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, songsList.size());
            Toast.makeText(mContext, "Song Deleted", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(mContext, "Song can't be deleted", Toast.LENGTH_SHORT).show();
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

    public void updateList(ArrayList<Songs> songsArrayList){
        songsList = new ArrayList<>();
        songsList.addAll(songsArrayList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return songsList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView songName, artistName;
        public ImageView album_art, moreMenu;
        RelativeLayout parentLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            songName = itemView.findViewById(R.id.tv_song_name);
            artistName = itemView.findViewById(R.id.tv_artist_name);
            album_art = itemView.findViewById(R.id.song_img);
            parentLayout = itemView.findViewById(R.id.song_item);
            moreMenu = itemView.findViewById(R.id.menu_more);
        }
    }
}