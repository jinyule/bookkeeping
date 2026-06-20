package com.github.bookkeeping.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CategoryDao {
    @Query("SELECT * FROM categories WHERE enabled = 1 ORDER BY direction, sortOrder")
    List<CategoryEntity> getAllEnabled();

    @Query("SELECT * FROM categories WHERE direction = :direction AND enabled = 1 ORDER BY sortOrder")
    List<CategoryEntity> getByDirection(String direction);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(CategoryEntity category);

    @Update
    int update(CategoryEntity category);
}
