package com.example.data

import android.content.Context
import com.example.R
import org.json.JSONObject

data class Dua(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val categoryAr: String,
    val categoryEn: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val virtueAr: String,
    val virtueEn: String,
    val targetCount: Int
)

object DuaData {
    var duas: List<Dua> = emptyList()

    fun load(context: Context) {
        if (duas.isNotEmpty()) return
        try {
            val jsonStr = context.resources.openRawResource(R.raw.hisn).bufferedReader().use { it.readText() }
            val duasList = mutableListOf<Dua>()
            // Remove bom if exists
            val cleanJsonStr = if (jsonStr.startsWith("\uFEFF")) jsonStr.substring(1) else jsonStr
            
            val rootObj = JSONObject(cleanJsonStr)
            
            // Also load the english json
            val enJsonStr = context.resources.openRawResource(R.raw.hisn_en).bufferedReader().use { it.readText() }
            val cleanEnJsonStr = if (enJsonStr.startsWith("\uFEFF")) enJsonStr.substring(1) else enJsonStr
            val enArr = org.json.JSONArray(cleanEnJsonStr)
            val enMap = mutableMapOf<String, String>() // Map of arabic (normalized) to english
            val catMap = mutableMapOf<String, String>() // Map of category (arabic) to title (english)
            
            for (i in 0 until enArr.length()) {
                val item = enArr.getJSONObject(i)
                val arText = item.getString("arabic").replace("\n", "").replace("\r", "").trim()
                val enText = item.getString("english")
                enMap[arText] = enText
            }

            var idCounter = 1
            
            for (categoryStr in rootObj.keys()) {
                val catObj = rootObj.getJSONObject(categoryStr)
                val textArr = catObj.getJSONArray("Adhkar")
                
                var categoryEn = categoryStr
                
                for (j in 0 until textArr.length()) {
                    val item = textArr.getJSONObject(j)
                    val arabic = item.getString("Text").trim()
                    
                    // Try to find matching english text
                    var foundEnglish = ""
                    val normalizedArabic = arabic.replace("\n", "").replace("\r", "")
                    
                    // Direct match or partial match
                    for ((ar, en) in enMap) {
                        // Usually some small differences exist, we can use contains
                        if (ar.contains(normalizedArabic) || normalizedArabic.contains(ar)) {
                            foundEnglish = en
                            
                            // Find corresponding title for category if not set
                            val matchedItem = enArr.let { arr ->
                                for (k in 0 until arr.length()) {
                                    val e = arr.getJSONObject(k)
                                    if (e.getString("arabic").replace("\n", "").replace("\r", "").trim() == ar) {
                                        return@let e
                                    }
                                }
                                null
                            }
                            if (matchedItem != null && categoryEn == categoryStr) {
                                categoryEn = matchedItem.getString("title").replace("Chapter: ", "").trim()
                            }
                            break
                        }
                    }
                    
                    val targetCount = item.optInt("Count", 1)
                    val reference = item.optString("Reference", "")
                    
                    duasList.add(Dua(
                        id = idCounter.toString(),
                        titleAr = categoryStr,
                        titleEn = categoryEn, // will update later
                        categoryAr = categoryStr,
                        categoryEn = categoryEn, // will update later
                        arabic = arabic,
                        transliteration = "",
                        translation = foundEnglish,
                        virtueAr = "",
                        virtueEn = reference,
                        targetCount = if (targetCount > 0) targetCount else 1
                    ))
                    idCounter++
                }
                
                // second pass for categoryEn just in case
                if (categoryEn != categoryStr) {
                    val startIndex = duasList.size - textArr.length()
                    for (k in startIndex until duasList.size) {
                        duasList[k] = duasList[k].copy(titleEn = categoryEn, categoryEn = categoryEn)
                    }
                }
            }
            duas = duasList
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
