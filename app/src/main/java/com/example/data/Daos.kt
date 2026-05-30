package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM surah_bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<SurahBookmark>>

    @Query("SELECT * FROM surah_bookmarks WHERE surahId = :surahId LIMIT 1")
    suspend fun getBookmarkBySurah(surahId: Int): SurahBookmark?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: SurahBookmark)

    @Query("DELETE FROM surah_bookmarks WHERE surahId = :surahId")
    suspend fun deleteBookmark(surahId: Int)
}

@Dao
interface TasbihDao {
    @Query("SELECT * FROM tasbih_state ORDER BY lastUpdated DESC")
    fun getAllTasbihStates(): Flow<List<TasbihState>>

    @Query("SELECT * FROM tasbih_state WHERE dhikrId = :dhikrId LIMIT 1")
    suspend fun getTasbihState(dhikrId: String): TasbihState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTasbihState(state: TasbihState)
}

@Dao
interface PrayerAlertDao {
    @Query("SELECT * FROM prayer_alert_settings")
    fun getAllAlertSettings(): Flow<List<PrayerAlertSetting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAlertSetting(setting: PrayerAlertSetting)
    
    @Query("SELECT * FROM prayer_alert_settings WHERE prayerName = :name LIMIT 1")
    suspend fun getAlertSetting(name: String): PrayerAlertSetting?
}

@Dao
interface DuaBookmarkDao {
    @Query("SELECT * FROM dua_bookmarks ORDER BY timestamp DESC")
    fun getAllDuaBookmarks(): Flow<List<DuaBookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuaBookmark(bookmark: DuaBookmark)

    @Query("DELETE FROM dua_bookmarks WHERE duaId = :duaId")
    suspend fun deleteDuaBookmark(duaId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM dua_bookmarks WHERE duaId = :duaId)")
    suspend fun isDuaBookmarked(duaId: String): Boolean
}

@Dao
interface QuranResumeDao {
    @Query("SELECT * FROM last_reading_state WHERE id = 1 LIMIT 1")
    suspend fun getLastReadingState(): LastReadingState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLastReadingState(state: LastReadingState)
}
