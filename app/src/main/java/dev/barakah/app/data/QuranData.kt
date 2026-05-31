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
            val list = mutableListOf<Surah>()
            context.resources.openRawResource(R.raw.quran).use { inputStream ->
                val reader = android.util.JsonReader(inputStream.bufferedReader())
                reader.beginObject()
                while (reader.hasNext()) {
                    if (reader.nextName() == "chapters") {
                        reader.beginObject()
                        while (reader.hasNext()) {
                            reader.nextName() // Chapter index string
                            list.add(readSurah(reader))
                        }
                        reader.endObject()
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
                reader.close()
            }
            
            surahs = list.sortedBy { it.id }
            _surahsFlow.value = surahs
            android.util.Log.d("QuranData", "Successfully loaded ${surahs.size} surahs")
        } catch (e: Exception) {
            android.util.Log.e("QuranData", "Critical error parsing quran.json with JsonReader", e)
            e.printStackTrace()
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
                "id" -> id = reader.nextInt()
                "surah_name" -> nameTrans = reader.nextString()
                "surah_name_ar" -> nameAr = reader.nextString()
                "translation" -> nameEn = reader.nextString()
                "type" -> type = reader.nextString()
                "total_verses" -> versesCount = reader.nextInt()
                "verses" -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        val verseIndex = reader.nextName().toIntOrNull() ?: 0
                        versesList.add(readVerse(reader, verseIndex))
                    }
                    reader.endObject()
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

    private fun readVerse(reader: android.util.JsonReader, index: Int): Verse {
        var ar = ""
        var en = ""

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "content" -> ar = reader.nextString()
                "translation_eng" -> en = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return Verse(index, ar, en)
    }
}
