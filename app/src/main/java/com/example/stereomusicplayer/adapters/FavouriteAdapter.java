package com.example.stereomusicplayer.adapters;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.stereomusicplayer.R;
import com.example.stereomusicplayer.model.Songs;

import java.io.IOException;
import java.util.List;

public class FavouriteAdapter extends RecyclerView.Adapter<FavouriteAdapter.FavouriteViewHolder> {

    private Context mContext;
    private List<Songs> favouriteFiles;

    public FavouriteAdapter(Context context, List<Songs> favouriteFiles) {
        mContext = context;
        this.favouriteFiles = favouriteFiles;
    }

    @NonNull
    @Override
    public FavouriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        /*View view = LayoutInflater.from(mContext).inflate(R.layout.song_item, parent, false);
        return new FavouriteViewHolder(view);*/
        return FavouriteViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull FavouriteViewHolder holder, int position) {
        Songs current = favouriteFiles.get(position);
        holder.bind(current.getTitle(), current.getArtist(), current.getPath(), mContext);
    }

    @Override
    public int getItemCount() {
        return favouriteFiles.size();
    }

    public void setSongs(List<Songs> favouriteFiles) {
        this.favouriteFiles = favouriteFiles;
        notifyDataSetChanged();
    }

    private static byte[] getAlbumArt(String uri) {
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


    public static class FavouriteViewHolder extends RecyclerView.ViewHolder {

        public TextView songName, artistName;
        public ImageView album_art, moreMenu;
        RelativeLayout parentLayout;

        public FavouriteViewHolder(@NonNull View itemView) {
            super(itemView);
            songName = itemView.findViewById(R.id.tv_song_name);
            artistName = itemView.findViewById(R.id.tv_artist_name);
            album_art = itemView.findViewById(R.id.song_img);
            parentLayout = itemView.findViewById(R.id.song_item);
            moreMenu = itemView.findViewById(R.id.menu_more);
        }

        static FavouriteViewHolder create(ViewGroup parent) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.song_item, parent, false);
            return new FavouriteViewHolder(view);
        }

        void bind(String songTitle, String songArtist, String songPath, Context mContext) {
            songName.setText(songTitle);
            artistName.setText(songArtist);
            byte[] image = getAlbumArt(songPath);
            if (image != null)
                Glide.with(mContext).asBitmap().load(image).into(album_art);
            else
                Glide.with(mContext).load(R.drawable.ic_album_art).into(album_art);

        }
    }

}
