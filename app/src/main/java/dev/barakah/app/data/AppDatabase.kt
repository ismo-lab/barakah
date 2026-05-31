package dev.barakah.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SurahBookmark::class, TasbihState::class, PrayerAlertSetting::class, DuaBookmark::class, LastReadingState::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun tasbihDao(): TasbihDao
    abstract fun prayerAlertDao(): PrayerAlertDao
    abstract fun duaBookmarkDao(): DuaBookmarkDao
    abstract fun quranResumeDao(): QuranResumeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "barakah_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
