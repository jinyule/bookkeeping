package com.github.bookkeeping.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import kotlinx.coroutines.flow.Flow;

@Dao
public interface BillDao {
    @Query("SELECT * FROM bills ORDER BY transactionTime DESC, id DESC")
    Flow<List<BillEntity>> observeAll();

    @Query("SELECT * FROM bills ORDER BY transactionTime DESC, id DESC")
    List<BillEntity> getAll();

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    BillEntity getById(long id);

    @Query("SELECT * FROM bills WHERE reviewStatus = 'REVIEW' ORDER BY transactionTime DESC")
    Flow<List<BillEntity>> observeReviewQueue();

    @Query("SELECT * FROM bills WHERE merchant LIKE :pattern ESCAPE '\\' OR note LIKE :pattern ESCAPE '\\' OR categoryKey LIKE :pattern ESCAPE '\\' OR sourceChannel LIKE :pattern ESCAPE '\\' ORDER BY transactionTime DESC, id DESC LIMIT 500")
    List<BillEntity> search(String pattern);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(BillEntity bill);

    @Update
    int update(BillEntity bill);

    @Query("DELETE FROM bills WHERE id = :id")
    int deleteById(long id);

    @Query("SELECT COUNT(*) FROM bills WHERE fingerprint = :fingerprint")
    int countByFingerprint(String fingerprint);
}
