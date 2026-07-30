package com.zzy.ksongfloat.history;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface InteractionRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(InteractionRecord record);

    @Update
    void update(InteractionRecord record);

    @Delete
    void delete(InteractionRecord record);

    @Query("DELETE FROM interaction_records")
    void clearAll();

    @Query("SELECT * FROM interaction_records ORDER BY lastSeenAt DESC")
    List<InteractionRecord> all();

    @Query("SELECT * FROM interaction_records WHERE interactionStatus = :status ORDER BY lastSeenAt DESC")
    List<InteractionRecord> byStatus(String status);

    @Query("SELECT * FROM interaction_records WHERE nickname LIKE '%' || :keyword || '%' ORDER BY lastSeenAt DESC")
    List<InteractionRecord> searchNickname(String keyword);

    @Query("SELECT * FROM interaction_records WHERE id = :id LIMIT 1")
    InteractionRecord byId(long id);

    @Query("SELECT * FROM interaction_records WHERE profileFingerprint = :fingerprint LIMIT 1")
    InteractionRecord byFingerprint(String fingerprint);

    @Query("UPDATE interaction_records SET interactionStatus = :status, lastSeenAt = :time WHERE id = :id")
    void updateStatus(long id, String status, long time);

    @Query("UPDATE interaction_records SET userNotes = :notes, lastSeenAt = :time WHERE id = :id")
    void updateNotes(long id, String notes, long time);
}
