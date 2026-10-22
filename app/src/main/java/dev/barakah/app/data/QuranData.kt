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

/** Verses plus whether the requested translation was missing and English had to stand in. */
data class VerseText(
    val verses: List<Verse>,
    val usedEnglishFallback: Boolean
)

/** Tafsir text plus whether the requested language was missing and English had to stand in. */
data class TafsirResult(
    val text: String,
    val usedEnglishFallback: Boolean
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

    private val versesCache = java.util.concurrent.ConcurrentHashMap<Int, List<Verse>>()

    fun loadVersesForSurah(context: Context, surahId: Int): List<Verse> {
        versesCache[surahId]?.let { return it }
        try {
            context.assets.open("quran/surah_$surahId.json").use { inputStream ->
                val reader = android.util.JsonReader(inputStream.bufferedReader())
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
                val sorted = verses.sortedBy { it.index }
                versesCache[surahId] = sorted
                return sorted
            }
        } catch (e: Exception) {
            android.util.Log.e("QuranData", "Error loading verses for Surah $surahId: ${e.message}")
            return emptyList()
        }
    }

    private var cachedSurahId: Int = -1
    private var cachedSurahTafseer: Map<Int, String> = emptyMap()

    /** Reads the bundled tafseer for one surah. Blocking: call it from a background dispatcher. */
    private fun readTafseerAsset(context: Context, surahId: Int): Map<Int, String> {
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
        return surahMap
    }

    /**
     * The tafseer is one bundled asset for the whole Quran, so reading it walks the file up to this
     * surah. That is real work, and it does not belong on the main thread while a verse is being
     * held down.
     */
    suspend fun loadTafseer(context: Context, surahId: Int, ayahId: Int): String {
        synchronized(this) {
            if (cachedSurahId == surahId && cachedSurahTafseer.isNotEmpty()) {
                val value = cachedSurahTafseer[ayahId]
                if (value != null) return value
            }
        }

        val surahMap = withContext(Dispatchers.IO) { readTafseerAsset(context, surahId) }

        synchronized(this) {
            cachedSurahId = surahId
            cachedSurahTafseer = surahMap
            return cachedSurahTafseer[ayahId] ?: "التفسير غير متوفر حالياً لهذه الآية."
        }
    }

    private var cachedEnSurahId: Int = -1
    private var cachedEnSurahTafseer: Map<Int, String> = emptyMap()

    /** Reads the bundled English tafseer for one surah. Blocking: call it off the main thread. */
    private fun readEnglishTafseerAsset(context: Context, surahId: Int): Map<Int, String> {
        val enMap = mutableMapOf<Int, String>()
        try {
            context.assets.open("quran/en_tafseer/$surahId.json").use { inputStream ->
                val reader = android.util.JsonReader(inputStream.bufferedReader())
                reader.beginObject()
                while (reader.hasNext()) {
                    if (reader.nextName() == "ayahs") {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            var aId = -1
                            var text = ""
                            reader.beginObject()
                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "ayah" -> aId = reader.nextInt()
                                    "text" -> text = reader.nextString()
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()
                            if (aId != -1 && text.isNotEmpty()) {
                                enMap[aId] = text
                            }
                        }
                        reader.endArray()
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
                reader.close()
            }
        } catch (e: Exception) {
            android.util.Log.e("QuranData", "Error opening local English tafseer asset for Surah $surahId: ${e.message}")
        }
        return enMap
    }

    /** Reads the English tafseer, which is bundled as one file per surah, off the main thread. */
    suspend fun loadEnglishTafseer(context: Context, surahId: Int, ayahId: Int): String {
        synchronized(this) {
            if (cachedEnSurahId == surahId && cachedEnSurahTafseer.isNotEmpty()) {
                val value = cachedEnSurahTafseer[ayahId]
                if (value != null) return value
            }
        }

        val enMap = withContext(Dispatchers.IO) { readEnglishTafseerAsset(context, surahId) }

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
            return "English Tafsir (Jalalayn) is not available currently. Here is the Arabic Tafsir:\n\n$arTafseer"
        }
        return "Tafsir is not available for this verse currently."
    }

    private val translationCache =
        java.util.concurrent.ConcurrentHashMap<String, VerseText>()

    /**
     * Verses with [language]'s translation in [Verse.english], merged onto the bundled Arabic.
     * A missing asset falls back to English and reports it so the screen can say so.
     */
    fun loadVersesForSurah(context: Context, surahId: Int, language: String): VerseText {
        val base = loadVersesForSurah(context, surahId)
        if (language.isEmpty() || language == "en") return VerseText(base, false)

        val key = "$language:$surahId"
        translationCache[key]?.let { return it }

        val translated = readTranslationAsset(context, language, surahId)
        val result = if (translated.isEmpty()) {
            VerseText(base, true)
        } else {
            VerseText(
                base.map { verse ->
                    verse.copy(english = translated[verse.index] ?: verse.english)
                },
                false
            )
        }
        translationCache[key] = result
        return result
    }

    private fun readTranslationAsset(
        context: Context,
        language: String,
        surahId: Int
    ): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        try {
            context.assets.open("quran/translations/$language/surah_$surahId.json").use { input ->
                val reader = android.util.JsonReader(input.bufferedReader())
                reader.beginObject()
                while (reader.hasNext()) {
                    if (reader.nextName() == "verses") {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            var index = -1
                            var text = ""
                            reader.beginObject()
                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "index" -> index = reader.nextInt()
                                    "text" -> text = reader.nextString()
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()
                            if (index != -1 && text.isNotBlank()) map[index] = text
                        }
                        reader.endArray()
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
                try { reader.close() } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            android.util.Log.d("QuranData", "No $language translation for surah $surahId")
        }
        return map
    }

    private val tafsirCache =
        java.util.concurrent.ConcurrentHashMap<String, Map<Int, String>>()

    /**
     * Reads tafsir in [language]. Arabic and English use the existing assets; other languages
     * live one file per surah under quran/tafsir/. Missing text falls back to English.
     */
    suspend fun loadTafsir(
        context: Context,
        surahId: Int,
        ayahId: Int,
        language: String
    ): TafsirResult {
        if (language.isEmpty() || language == "ar") {
            return TafsirResult(loadTafseer(context, surahId, ayahId), false)
        }
        if (language == "en") {
            return TafsirResult(loadEnglishTafseer(context, surahId, ayahId), false)
        }

        val key = "$language:$surahId"
        val surahMap = tafsirCache[key] ?: withContext(Dispatchers.IO) {
            readNumberedTafsir(context, language, surahId)
        }.also { if (it.isNotEmpty()) tafsirCache[key] = it }

        val text = surahMap[ayahId]
        if (!text.isNullOrBlank()) return TafsirResult(text, false)
        return TafsirResult(loadEnglishTafseer(context, surahId, ayahId), true)
    }

    private fun readNumberedTafsir(
        context: Context,
        language: String,
        surahId: Int
    ): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        try {
            context.assets.open("quran/tafsir/$language/$surahId.json").use { input ->
                val reader = android.util.JsonReader(input.bufferedReader())
                reader.beginObject()
                while (reader.hasNext()) {
                    if (reader.nextName() == "ayahs") {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            var ayah = -1
                            var text = ""
                            reader.beginObject()
                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "ayah" -> ayah = reader.nextInt()
                                    "text" -> text = reader.nextString()
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()
                            if (ayah != -1 && text.isNotBlank()) map[ayah] = text
                        }
                        reader.endArray()
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
                try { reader.close() } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            android.util.Log.d("QuranData", "No $language tafsir for surah $surahId")
        }
        return map
    }
}
