package dev.barakah.app.data

import android.content.Context
import dev.barakah.app.R
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
    var duasAr: List<Dua> = emptyList()
    var duasEn: List<Dua> = emptyList()

    // For backwards compatibility
    val duas: List<Dua>
        get() = duasAr

    fun load(context: Context) {
        if (duasAr.isNotEmpty() && duasEn.isNotEmpty()) return
        
        // 1. Load Arabic Duas from hisn.json
        if (duasAr.isEmpty()) {
            try {
                val jsonStr = context.resources.openRawResource(R.raw.hisn).bufferedReader().use { it.readText() }
                val cleanJsonStr = if (jsonStr.startsWith("\uFEFF")) jsonStr.substring(1) else jsonStr
                val rootObj = JSONObject(cleanJsonStr)
                val arList = mutableListOf<Dua>()
                var idCounter = 1
                
                for (categoryStr in rootObj.keys()) {
                    val catObj = rootObj.getJSONObject(categoryStr)
                    val textArr = catObj.getJSONArray("Adhkar")
                    
                    for (j in 0 until textArr.length()) {
                        val item = textArr.getJSONObject(j)
                        val arabic = item.getString("Text").trim()
                        val targetCount = item.optInt("Count", 1)
                        val reference = item.optString("Reference", "")
                        
                        arList.add(Dua(
                            id = "ar_$idCounter",
                            titleAr = categoryStr,
                            titleEn = categoryStr,
                            categoryAr = categoryStr,
                            categoryEn = categoryStr,
                            arabic = arabic,
                            transliteration = "",
                            translation = "",
                            virtueAr = reference,
                            virtueEn = "",
                            targetCount = if (targetCount > 0) targetCount else 1
                        ))
                        idCounter++
                    }
                }
                duasAr = arList
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Load English Duas from hisn_en.json
        if (duasEn.isEmpty()) {
            try {
                val enJsonStr = context.resources.openRawResource(R.raw.hisn_en).bufferedReader().use { it.readText() }
                val cleanEnJsonStr = if (enJsonStr.startsWith("\uFEFF")) enJsonStr.substring(1) else enJsonStr
                val enArr = org.json.JSONArray(cleanEnJsonStr)
                val enList = mutableListOf<Dua>()
                
                for (i in 0 until enArr.length()) {
                    val item = enArr.getJSONObject(i)
                    val title = item.optString("title", "").replace("Chapter: ", "").trim()
                    val arabic = item.optString("arabic", "").trim()
                    val english = item.optString("english", "").trim()
                    val reference = item.optString("reference", "").trim()
                    
                    enList.add(Dua(
                        id = "en_${i + 1}",
                        titleAr = "",
                        titleEn = title,
                        categoryAr = "",
                        categoryEn = title,
                        arabic = arabic,
                        transliteration = "",
                        translation = english,
                        virtueAr = "",
                        virtueEn = reference,
                        targetCount = 1
                    ))
                }

                // Match counts from Arabic dataset to keep rep counts aligned when using English
                fun normalizeAr(text: String): String {
                    val sb = java.lang.StringBuilder()
                    for (char in text) {
                        if (char in '\u064B'..'\u065F' || char == '\u0670' || char == '\u0640') {
                            continue
                        }
                        val normalizedChar = when (char) {
                            'أ', 'إ', 'آ' -> 'ا'
                            'ى' -> 'ي'
                            'ة' -> 'ه'
                            else -> char
                        }
                        if (normalizedChar in '\u0621'..'\u064A') {
                            sb.append(normalizedChar)
                        }
                    }
                    return sb.toString()
                }

                val arNormalizedList = duasAr.map { it to normalizeAr(it.arabic) }

                val finalEnList = enList.map { enDua ->
                    val enNorm = normalizeAr(enDua.arabic)
                    if (enNorm.length < 5) return@map enDua

                    // Try to find matching Arabic dua
                    var match: Dua? = arNormalizedList.firstOrNull { it.second == enNorm }?.first
                    if (match == null) {
                        match = arNormalizedList.firstOrNull { 
                            it.second.isNotEmpty() && (enNorm.contains(it.second) || it.second.contains(enNorm))
                        }?.first
                    }
                    if (match == null) {
                        match = arNormalizedList.firstOrNull {
                            val arNorm = it.second
                            val len = kotlin.math.min(enNorm.length, arNorm.length)
                            len >= 12 && enNorm.substring(0, 12) == arNorm.substring(0, 12)
                        }?.first
                    }

                    if (match != null && match.targetCount > 1) {
                        enDua.copy(targetCount = match.targetCount)
                    } else {
                        enDua
                    }
                }

                duasEn = finalEnList
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
