package dev.barakah.app.data

import android.content.Context
import dev.barakah.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
        
        CoroutineScope(Dispatchers.IO).launch {
            android.util.Log.d("QuranData", "Loading Quran metadata from assets...")
            
            try {
                val metadataJson = context.assets.open("quran/metadata.json").bufferedReader().use { it.readText() }
                val reader = android.util.JsonReader(metadataJson.reader())
                val list = mutableListOf<Surah>()
                
                reader.beginArray()
                while (reader.hasNext()) {
                    var id = 0
                    var name = ""
                    var arabic = ""
                    var translation = ""
                    var type = ""
                    var versesCount = 0
                    
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "id" -> id = reader.nextInt()
                            "name" -> name = reader.nextString()
                            "arabic" -> arabic = reader.nextString()
                            "translation" -> translation = reader.nextString()
                            "type" -> type = reader.nextString()
                            "versesCount" -> versesCount = reader.nextInt()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                    
                    list.add(Surah(
                        id = id,
                        name = name,
                        arabic = arabic,
                        translation = translation,
                        type = if (type.isNotEmpty()) {
                            type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.US) else it.toString() }
                        } else "",
                        versesCount = versesCount,
                        versesList = emptyList()
                    ))
                }
                reader.endArray()
                reader.close()
                
                withContext(Dispatchers.Main) {
                    surahs = list.sortedBy { it.id }
                    _surahsFlow.value = surahs
                    android.util.Log.d("QuranData", "Successfully loaded metadata for ${surahs.size} surahs")
                }
            } catch (e: Exception) {
                android.util.Log.e("QuranData", "Error loading Quran metadata: ${e.message}", e)
            }
        }
    }

    fun loadVersesForSurah(context: Context, surahId: Int): List<Verse> {
        try {
            val surahJson = context.assets.open("quran/surah_$surahId.json").bufferedReader().use { it.readText() }
            val reader = android.util.JsonReader(surahJson.reader())
            val verses = mutableListOf<Verse>()
            
            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                if (name == "verses") {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        var index = 0
                        var arabic = ""
                        var english = ""
                        
                        reader.beginObject()
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "index" -> index = reader.nextInt()
                                "arabic" -> arabic = reader.nextString()
                                "english" -> english = reader.nextString()
                                else -> reader.skipValue()
                            }
                        }
                        reader.endObject()
                        verses.add(Verse(index, arabic, english))
                    }
                    reader.endArray()
                } else {
                    reader.skipValue()
                }
            }
            reader.endObject()
            reader.close()
            return verses.sortedBy { it.index }
        } catch (e: Exception) {
            android.util.Log.e("QuranData", "Error loading verses for Surah $surahId: ${e.message}")
            return emptyList()
        }
    }

    private var tafseerCache: Map<String, String>? = null
    private var tafseerEnCache: Map<String, String>? = null

    fun loadTafseer(context: Context, surahId: Int, ayahId: Int): String {
        if (tafseerCache == null) {
            loadAllTafseer(context)
        }
        val key = "${surahId}_$ayahId"
        return tafseerCache?.get(key) ?: "التفسير غير متوفر حالياً لهذه الآية."
    }

    fun loadEnglishTafseer(context: Context, surahId: Int, ayahId: Int): String {
        if (tafseerEnCache == null) {
            loadAllEnglishTafseer(context)
        }
        val key = "${surahId}_$ayahId"
        return tafseerEnCache?.get(key) ?: "Tafsir is not available for this verse currently."
    }

    @Synchronized
    private fun loadAllEnglishTafseer(context: Context) {
        if (tafseerEnCache != null) return
        val cacheMap = mutableMapOf<String, String>()
        try {
            android.util.Log.d("QuranData", "Loading English Tafseer into memory...")
            for (surahId in 1..114) {
                try {
                    val jsonText = context.assets.open("quran/en_tafseer/$surahId.json").bufferedReader().use { it.readText() }
                    val obj = org.json.JSONObject(jsonText)
                    if (obj.has("ayahs")) {
                        val ayahs = obj.getJSONArray("ayahs")
                        for (i in 0 until ayahs.length()) {
                            val a = ayahs.getJSONObject(i)
                            val ayah = a.getInt("ayah")
                            val text = a.getString("text")
                            cacheMap["${surahId}_$ayah"] = text
                        }
                    }
                } catch (e: Exception) {
                }
            }
            tafseerEnCache = cacheMap
        } catch (e: Exception) {
            android.util.Log.e("QuranData", "Error loading English tafseer: ${e.message}", e)
        }
    }

    @Synchronized
    private fun loadAllTafseer(context: Context) {
        if (tafseerCache != null) return
        val cacheMap = mutableMapOf<String, String>()
        try {
            android.util.Log.d("QuranData", "Loading Tafseer into memory from assets...")
            val jsonText = context.assets.open("quran/tafseer.json").bufferedReader().use { it.readText() }
            val reader = android.util.JsonReader(jsonText.reader())
            reader.beginArray()
            while (reader.hasNext()) {
                var numberStr = ""
                var ayaStr = ""
                var tafseerText = ""
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "number" -> numberStr = reader.nextString()
                        "aya" -> ayaStr = reader.nextString()
                        "text" -> tafseerText = reader.nextString()
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                if (numberStr.isNotEmpty() && ayaStr.isNotEmpty()) {
                    cacheMap["${numberStr}_$ayaStr"] = tafseerText
                }
            }
            reader.endArray()
            reader.close()
            tafseerCache = cacheMap
            android.util.Log.d("QuranData", "Loaded ${cacheMap.size} Tafseer entries into memory cache successfully")
        } catch (e: Exception) {
            android.util.Log.e("QuranData", "Error loading tafseer.json: ${e.message}", e)
        }
    }
}
