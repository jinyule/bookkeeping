package com.github.bookkeeping.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SourceSettingDao {
    @Query("SELECT * FROM source_settings ORDER BY sourceKey")
    List<SourceSettingEntity> getAll();

    @Query("SELECT * FROM source_settings WHERE sourceKey = :sourceKey LIMIT 1")
    SourceSettingEntity get(String sourceKey);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(SourceSettingEntity setting);

    @Update
    int update(SourceSettingEntity setting);
}
