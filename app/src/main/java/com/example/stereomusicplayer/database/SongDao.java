package com.example.stereomusicplayer.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.stereomusicplayer.model.Songs;

import java.util.List;

@Dao
public interface SongDao {

    @Query("SELECT * FROM songs")
    LiveData<List<Songs>> getAllSongs();

    @Query("SELECT COUNT(*) FROM songs")
    int rowCount();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Songs song);

    @Query("DELETE FROM songs")
    void deleteAll();

    @Delete
    void delete(Songs... songs);
}
