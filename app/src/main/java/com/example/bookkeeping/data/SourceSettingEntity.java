package com.example.bookkeeping.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "source_settings")
public class SourceSettingEntity {
    @PrimaryKey
    @NonNull
    public String sourceKey = "";

    public boolean notificationEnabled = true;

    public boolean accessibilityEnabled = true;

    public boolean importEnabled = true;

    public boolean paused = false;

    public long updatedAt;
}
