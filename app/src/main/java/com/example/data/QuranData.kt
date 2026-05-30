package com.example.data

import android.content.Context
import com.example.R
import org.json.JSONArray

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

    fun getSurahById(id: Int): Surah {
        return surahs.find { it.id == id } ?: if (surahs.isNotEmpty()) surahs[0] else Surah(1, "Loading", "جاري التحميل", "Loading", "Meccan", 0)
    }

    fun load(context: Context) {
        if (surahs.isNotEmpty()) return
        try {
            val jsonStr = context.resources.openRawResource(R.raw.quran).bufferedReader().use { it.readText() }
            val list = mutableListOf<Surah>()
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getInt("number")
                val nameObj = obj.getJSONObject("name")
                val nameAr = nameObj.getString("ar")
                val trans = nameObj.getString("transliteration")
                val translation = nameObj.getString("en")
                val revPlaceObj = obj.getJSONObject("revelation_place")
                val type = revPlaceObj.getString("en")
                val versesCount = obj.getInt("verses_count")
                
                val versesList = mutableListOf<Verse>()
                val versesArr = obj.optJSONArray("verses")
                if (versesArr != null) {
                    for (j in 0 until versesArr.length()) {
                        val vObj = versesArr.getJSONObject(j)
                        val textObj = vObj.getJSONObject("text")
                        versesList.add(Verse(
                            index = vObj.getInt("number"),
                            arabic = textObj.getString("ar"),
                            english = textObj.optString("en", "")
                        ))
                    }
                }
                list.add(Surah(
                    id = id,
                    name = trans,
                    arabic = nameAr,
                    translation = translation,
                    type = type.replaceFirstChar { it.uppercase() },
                    versesCount = versesCount,
                    versesList = versesList
                ))
            }
            surahs = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
