package dev.barakah.app.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val database: AppDatabase) {

    // Bookmarks
    val allBookmarks: Flow<List<SurahBookmark>> = database.bookmarkDao().getAllBookmarks()

    suspend fun getBookmarkBySurah(surahId: Int): SurahBookmark? {
        return database.bookmarkDao().getBookmarkBySurah(surahId)
    }

    suspend fun saveBookmark(bookmark: SurahBookmark) {
        database.bookmarkDao().insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(surahId: Int) {
        database.bookmarkDao().deleteBookmark(surahId)
    }

    // Tasbih
    val allTasbihStates: Flow<List<TasbihState>> = database.tasbihDao().getAllTasbihStates()

    suspend fun getTasbihState(dhikrId: String): TasbihState? {
        return database.tasbihDao().getTasbihState(dhikrId)
    }

    suspend fun saveTasbihState(state: TasbihState) {
        database.tasbihDao().saveTasbihState(state)
    }

    // Prayer Alerts
    val allAlertSettings: Flow<List<PrayerAlertSetting>> = database.prayerAlertDao().getAllAlertSettings()

    suspend fun saveAlertSetting(setting: PrayerAlertSetting) {
        database.prayerAlertDao().saveAlertSetting(setting)
    }

    suspend fun initDefaultAlertSettings() {
        val prayers = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")
        for (prayer in prayers) {
            if (database.prayerAlertDao().getAlertSetting(prayer) == null) {
                database.prayerAlertDao().saveAlertSetting(PrayerAlertSetting(prayer, true))
            }
        }
    }

    // Dua Bookmarks
    val allDuaBookmarks: Flow<List<DuaBookmark>> = database.duaBookmarkDao().getAllDuaBookmarks()

    suspend fun saveDuaBookmark(bookmark: DuaBookmark) {
        database.duaBookmarkDao().insertDuaBookmark(bookmark)
    }

    suspend fun deleteDuaBookmark(duaId: String) {
        database.duaBookmarkDao().deleteDuaBookmark(duaId)
    }

    // Quran Resume
    suspend fun getLastReadingState(): LastReadingState? {
        return database.quranResumeDao().getLastReadingState()
    }

    suspend fun saveLastReadingState(state: LastReadingState) {
        database.quranResumeDao().saveLastReadingState(state)
    }
}
