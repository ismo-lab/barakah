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

    private var cachedSurahId: Int = -1
    private var cachedSurahTafseer: Map<Int, String> = emptyMap()

    fun loadTafseer(context: Context, surahId: Int, ayahId: Int): String {
        synchronized(this) {
            if (cachedSurahId == surahId && cachedSurahTafseer.isNotEmpty()) {
                val value = cachedSurahTafseer[ayahId]
                if (value != null) return value
            }
        }
        
        val surahMap = mutableMapOf<Int, String>()
        try {
            context.assets.open("quran/tafseer_ar.json").use { inputStream ->
                val reader = android.util.JsonReader(inputStream.reader())
                reader.beginArray()
                while (reader.hasNext()) {
                    reader.beginObject()
                    var sId = -1
                    var aId = -1
                    var text = ""
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "number" -> sId = reader.nextString().toIntOrNull() ?: -1
                            "aya" -> aId = reader.nextString().toIntOrNull() ?: -1
                            "text" -> text = reader.nextString()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                    
                    if (sId == surahId && aId != -1) {
                        surahMap[aId] = text
                    } else if (sId > surahId) {
                        break
                    }
                }
                try { reader.close() } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            android.util.Log.e("QuranData", "Error loading tafseer for surah $surahId: ${e.message}", e)
        }
        
        synchronized(this) {
            cachedSurahId = surahId
            cachedSurahTafseer = surahMap
            return cachedSurahTafseer[ayahId] ?: "التفسير غير متوفر حالياً لهذه الآية."
        }
    }

    private var cachedEnSurahId: Int = -1
    private var cachedEnSurahTafseer: Map<Int, String> = emptyMap()

    fun loadEnglishTafseer(context: Context, surahId: Int, ayahId: Int): String {
        synchronized(this) {
            if (cachedEnSurahId == surahId && cachedEnSurahTafseer.isNotEmpty()) {
                val value = cachedEnSurahTafseer[ayahId]
                if (value != null) return value
            }
        }

        var jsonText = ""
        try {
            context.assets.open("quran/en_tafseer/$surahId.json").use { inputStream ->
                jsonText = inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            android.util.Log.e("QuranData", "Error opening local English tafseer asset for Surah $surahId: ${e.message}")
        }

        val enMap = mutableMapOf<Int, String>()
        if (jsonText.isNotEmpty()) {
            try {
                val obj = org.json.JSONObject(jsonText)
                if (obj.has("ayahs")) {
                    val ayahsArr = obj.getJSONArray("ayahs")
                    for (i in 0 until ayahsArr.length()) {
                        val item = ayahsArr.getJSONObject(i)
                        val aId = item.optInt("ayah", -1)
                        val text = item.optString("text", "")
                        if (aId != -1 && text.isNotEmpty()) {
                            enMap[aId] = text
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("QuranData", "Error parsing en tafseer json: ${e.message}", e)
            }
        }

        synchronized(this) {
            if (enMap.isNotEmpty()) {
                cachedEnSurahId = surahId
                cachedEnSurahTafseer = enMap
                val value = cachedEnSurahTafseer[ayahId]
                if (value != null) return value
            }
        }

        // Fallback: If local file load fails or is missing, fall back to Arabic translation or error message
        val arTafseer = loadTafseer(context, surahId, ayahId)
        if (arTafseer != "التفسير غير متوفر حالياً لهذه الآية.") {
            return "English Tafseer (Tafsir al-Jalalayn) is not available currently. Here is the Arabic Tafseer:\n\n$arTafseer"
        }
        return "Tafsir is not available for this verse currently."
    }
}
