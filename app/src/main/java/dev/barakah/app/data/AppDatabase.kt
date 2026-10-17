package dev.barakah.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SurahBookmark::class, TasbihState::class, PrayerAlertSetting::class, DuaBookmark::class, LastReadingState::class],
    version = 2,
    // The schema is checked in under app/schemas, so the next version can be given a real migration
    // and an entity change without a version bump fails the build rather than the database.
    exportSchema = true
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
                // There are no migrations yet, so any version bump would recreate the database. That
                // is the last resort rather than the plan: the exported schema in app/schemas is what
                // lets a future bump ship a migration instead, and this stays so that a missing one
                // cannot leave the app unable to start at all.
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
