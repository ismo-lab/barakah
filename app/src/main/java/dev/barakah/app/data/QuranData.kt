package dev.barakah.app.data

import android.content.Context
import dev.barakah.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Verse(
    val index: Int,
    val arabic: String,
    val english: String
)

data class Surah(
    val id: Int,
    val name: String,
    val arabic: String,
    val translation: String,
    val type: String,
    val versesCount: Int,
    val versesList: List<Verse> = emptyList()
)

object QuranData {
    var surahs: List<Surah> = listOf()
    
    private val _surahsFlow = MutableStateFlow<List<Surah>>(emptyList())
    val surahsFlow: StateFlow<List<Surah>> = _surahsFlow

    fun getSurahById(id: Int): Surah {
        return surahs.find { it.id == id } ?: if (surahs.isNotEmpty()) surahs[0] else Surah(1, "Loading", "جاري التحميل", "Loading", "Meccan", 0)
    }

    fun load(context: Context) {
        if (surahs.isNotEmpty()) return
        
        android.util.Log.d("QuranData", "Starting optimized Quran data load...")
        
        try {
            val resId = R.raw.quran
            if (resId == 0) {
                android.util.Log.e("QuranData", "R.raw.quran NOT FOUND!")
                return
            }
            
            val list = mutableListOf<Surah>()
            context.resources.openRawResource(resId).use { inputStream ->
                val size = inputStream.available()
                android.util.Log.d("QuranData", "Raw resource size: $size bytes")
                
                val reader = android.util.JsonReader(inputStream.bufferedReader(Charsets.UTF_8))
                reader.beginArray()
                while (reader.hasNext()) {
                    list.add(readSurah(reader))
                }
                reader.endArray()
                reader.close()
            }
            
            surahs = list.sortedBy { it.id }
            _surahsFlow.value = surahs
            android.util.Log.d("QuranData", "Successfully loaded ${surahs.size} surahs")
        } catch (e: Exception) {
            android.util.Log.e("QuranData", "Critical error parsing quran.json: ${e.message}", e)
        }
    }

    private fun readSurah(reader: android.util.JsonReader): Surah {
        var id = 0
        var nameAr = ""
        var nameTrans = ""
        var nameEn = ""
        var type = ""
        var versesCount = 0
        var versesList = mutableListOf<Verse>()

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "number" -> id = reader.nextInt()
                "name" -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "ar" -> nameAr = reader.nextString()
                            "en" -> nameEn = reader.nextString()
                            "transliteration" -> nameTrans = reader.nextString()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                }
                "revelation_place" -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        if (reader.nextName() == "en") type = reader.nextString()
                        else reader.skipValue()
                    }
                    reader.endObject()
                }
                "verses_count" -> versesCount = reader.nextInt()
                "verses" -> {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        versesList.add(readVerse(reader))
                    }
                    reader.endArray()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return Surah(
            id = id,
            name = nameTrans,
            arabic = nameAr,
            translation = nameEn,
            type = if (type.isNotEmpty()) {
                type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.US) else it.toString() }
            } else "Meccan",
            versesCount = versesCount,
            versesList = versesList.sortedBy { it.index }
        )
    }

    private fun readVerse(reader: android.util.JsonReader): Verse {
        var number = 0
        var ar = ""
        var en = ""

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "number" -> number = reader.nextInt()
                "text" -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "ar" -> ar = reader.nextString()
                            "en" -> en = reader.nextString()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return Verse(number, ar, en)
    }
}
