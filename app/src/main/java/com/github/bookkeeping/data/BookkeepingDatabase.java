package com.github.bookkeeping.data;

import androidx.room.Database;
import androidx.room.migration.Migration;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {BillEntity.class, CategoryEntity.class, SourceSettingEntity.class},
        version = 2,
        exportSchema = true
)
public abstract class BookkeepingDatabase extends RoomDatabase {
    public abstract BillDao billDao();

    public abstract CategoryDao categoryDao();

    public abstract SourceSettingDao sourceSettingDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("DROP INDEX IF EXISTS index_categories_categoryKey");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_categoryKey_direction ON categories(categoryKey, direction)");
        }
    };
}
