package dev.barakah.app.data

import android.content.Context
import org.json.JSONObject

data class OccasionCopy(val name: String, val desc: String)
data class NameCopy(val meaning: String, val detail: String)

/**
 * Localized copy for the Others screen. The Arabic and English originals stay in code; this asset
 * only carries the other interface languages, and every lookup falls back to the caller's English
 * text when a language or id is missing.
 */
class OthersContent private constructor(
    private val occasions: Map<Int, Map<String, OccasionCopy>>,
    private val names: Map<Int, Map<String, NameCopy>>
) {
    fun occasionName(id: Int, language: String, fallback: String): String =
        occasions[id]?.get(language)?.name ?: fallback

    fun occasionDescription(id: Int, language: String, fallback: String): String =
        occasions[id]?.get(language)?.desc ?: fallback

    fun nameMeaning(id: Int, language: String, fallback: String): String =
        names[id]?.get(language)?.meaning ?: fallback

    fun nameDetail(id: Int, language: String, fallback: String): String =
        names[id]?.get(language)?.detail ?: fallback

    companion object {
        @Volatile
        private var cached: OthersContent? = null

        fun load(context: Context): OthersContent =
            cached ?: synchronized(this) {
                cached ?: parse(context).also { cached = it }
            }

        private fun parse(context: Context): OthersContent {
            val root = context.assets.open("others_i18n.json").bufferedReader().use { it.readText() }
            val json = JSONObject(root)

            val occasionMap = mutableMapOf<Int, MutableMap<String, OccasionCopy>>()
            val nameMap = mutableMapOf<Int, MutableMap<String, NameCopy>>()

            val occasionJson = json.getJSONObject("occasions")
            occasionJson.keys().forEach { language ->
                val byId = occasionJson.getJSONObject(language)
                byId.keys().forEach { id ->
                    val item = byId.getJSONObject(id)
                    occasionMap.getOrPut(id.toInt()) { mutableMapOf() }[language] =
                        OccasionCopy(item.getString("name"), item.getString("desc"))
                }
            }

            val nameJson = json.getJSONObject("allahNames")
            nameJson.keys().forEach { language ->
                val byId = nameJson.getJSONObject(language)
                byId.keys().forEach { id ->
                    val item = byId.getJSONObject(id)
                    nameMap.getOrPut(id.toInt()) { mutableMapOf() }[language] =
                        NameCopy(item.getString("meaning"), item.getString("detail"))
                }
            }

            return OthersContent(occasionMap, nameMap)
        }
    }
}
