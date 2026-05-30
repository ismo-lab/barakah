package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surah_bookmarks")
data class SurahBookmark(
    @PrimaryKey val surahId: Int,
    val surahName: String,
    val transliteration: String,
    val lastReadAyah: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasbih_state")
data class TasbihState(
    @PrimaryKey val dhikrId: String,
    val count: Int,
    val target: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "dua_bookmarks")
data class DuaBookmark(
    @PrimaryKey val duaId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "last_reading_state")
data class LastReadingState(
    @PrimaryKey val id: Int = 1,
    val surahId: Int,
    val ayahNumber: Int,
    val surahName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "prayer_alert_settings")
data class PrayerAlertSetting(
    @PrimaryKey val prayerName: String,
    val isEnabled: Boolean = true
)
