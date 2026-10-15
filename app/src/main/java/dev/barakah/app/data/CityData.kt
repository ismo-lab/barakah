package dev.barakah.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream
import java.util.Locale

data class CitySuggestion(
    val name: String,
    val country: String,
    val countryAr: String,
    val nameAr: String,
    val lat: Double,
    val lng: Double,
    val timeZoneId: String
)

/** Offline place search backed by the GeoNames cities500 gazetteer. */
object CityData {
    private const val TAG = "CityData"
    private const val DATABASE_PREFIX = "geonames-cities500-"
    private val databaseMutex = Mutex()
    @Volatile private var database: SQLiteDatabase? = null

    suspend fun search(context: Context, query: String, limit: Int = 5): List<CitySuggestion> =
        withContext(Dispatchers.IO) {
            val term = query.trim()
            if (term.length < 2 || limit <= 0) return@withContext emptyList()

            try {
                val db = openDatabase(context.applicationContext)
                val prefixResults = queryDatabase(db, "${escapeLike(term)}%", limit)
                if (prefixResults.isNotEmpty()) prefixResults
                else queryDatabase(db, "%${escapeLike(term)}%", limit)
            } catch (error: Exception) {
                Log.e(TAG, "Unable to search the offline city catalog", error)
                emptyList()
            }
        }

    suspend fun nearestTimeZoneId(context: Context, latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            try {
                val db = openDatabase(context.applicationContext)
                db.rawQuery("SELECT latitude, longitude, timezone_id FROM city", null).use { cursor ->
                    var nearestDistance = Double.MAX_VALUE
                    var nearestTimeZone: String? = null
                    val originLatitudeRadians = Math.toRadians(latitude)
                    while (cursor.moveToNext()) {
                        val cityLatitude = cursor.getDouble(0)
                        val cityLongitude = cursor.getDouble(1)
                        val latitudeDelta = Math.toRadians(cityLatitude - latitude)
                        val longitudeDelta = Math.toRadians(cityLongitude - longitude)
                        val haversine = kotlin.math.sin(latitudeDelta / 2).let { it * it } +
                            kotlin.math.cos(originLatitudeRadians) * kotlin.math.cos(Math.toRadians(cityLatitude)) *
                            kotlin.math.sin(longitudeDelta / 2).let { it * it }
                        val distance = 2 * 6_371_000.0 * kotlin.math.asin(kotlin.math.sqrt(haversine.coerceIn(0.0, 1.0)))
                        if (distance < nearestDistance) {
                            nearestDistance = distance
                            nearestTimeZone = cursor.getString(2)
                        }
                    }
                    if (nearestDistance <= 100_000.0) nearestTimeZone else null
                }
            } catch (error: Exception) {
                Log.e(TAG, "Unable to resolve a timezone for the saved location", error)
                null
            }
        }

    private suspend fun openDatabase(context: Context): SQLiteDatabase = databaseMutex.withLock {
        database?.takeIf { it.isOpen }?.let { return@withLock it }

        val catalogVersion = context.assets.open("cities500.version").bufferedReader().use { it.readText().trim() }
        require(catalogVersion.matches(Regex("[0-9a-f]{64}"))) { "Invalid GeoNames catalog version" }
        val databaseFileName = "$DATABASE_PREFIX$catalogVersion.sqlite"
        val file = context.getDatabasePath(databaseFileName)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            val temporaryFile = File(file.parentFile, "$databaseFileName.tmp")
            try {
                GZIPInputStream(context.assets.open("cities500.sqlite.gz")).use { input ->
                    FileOutputStream(temporaryFile).use { output -> input.copyTo(output) }
                }
                if (!temporaryFile.renameTo(file)) {
                    throw IllegalStateException("Could not install the offline city catalog")
                }
            } finally {
                temporaryFile.delete()
            }
        }

        val openedDatabase = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        context.databaseList()
            .filter { it.startsWith(DATABASE_PREFIX) && it.endsWith(".sqlite") && it != databaseFileName }
            .forEach(context::deleteDatabase)
        openedDatabase.also { database = it }
    }

    private fun queryDatabase(db: SQLiteDatabase, pattern: String, limit: Int): List<CitySuggestion> {
        val sql = """
            SELECT c.name, c.country_code, c.arabic_name, c.latitude, c.longitude, c.timezone_id
            FROM city AS c
            WHERE c.name LIKE ? ESCAPE char(92) COLLATE NOCASE
               OR c.ascii_name LIKE ? ESCAPE char(92) COLLATE NOCASE
               OR c.arabic_name LIKE ? ESCAPE char(92) COLLATE NOCASE
               OR c.id IN (
                    SELECT a.city_id FROM city_alias AS a
                    WHERE a.name LIKE ? ESCAPE char(92) COLLATE NOCASE
               )
            ORDER BY c.population DESC, c.name COLLATE NOCASE ASC
            LIMIT ?
        """.trimIndent()
        val args = arrayOf(pattern, pattern, pattern, pattern, limit.toString())
        return db.rawQuery(sql, args).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0)
                    val countryCode = cursor.getString(1)
                    val arabicName = cursor.getString(2) ?: name
                    val country = Locale("", countryCode).getDisplayCountry(Locale.ENGLISH)
                    val countryAr = Locale("", countryCode).getDisplayCountry(Locale.forLanguageTag("ar"))
                    add(
                        CitySuggestion(
                            name = name,
                            country = country,
                            countryAr = countryAr.ifBlank { country },
                            nameAr = arabicName.ifBlank { name },
                            lat = cursor.getDouble(3),
                            lng = cursor.getDouble(4),
                            timeZoneId = cursor.getString(5)
                        )
                    )
                }
            }
        }
    }

    private fun escapeLike(value: String): String = value
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
}
