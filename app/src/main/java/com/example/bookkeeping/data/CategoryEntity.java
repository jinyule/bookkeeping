package com.example.bookkeeping.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "categories",
        indices = {@Index(value = {"categoryKey", "direction"}, unique = true)}
)
public class CategoryEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String categoryKey = "";

    @NonNull
    public String direction = "EXPENSE";

    @NonNull
    public String labelResName = "";

    public int sortOrder;

    public boolean builtIn;

    public boolean enabled = true;
}
