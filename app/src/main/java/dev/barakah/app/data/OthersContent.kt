package dev.barakah.app.data

import android.content.Context
import org.json.JSONObject

data class OccasionCopy(val name: String, val desc: String)

/**
 * Localized occasion copy for the Others screen. English stays in code; this asset only carries
 * the other interface languages, and every lookup falls back to English when a language is missing.
 */
class OthersContent private constructor(
    private val occasions: Map<Int, Map<String, OccasionCopy>>
) {
    fun occasionName(id: Int, language: String, fallback: String): String =
        occasions[id]?.get(language)?.name ?: fallback

    fun occasionDescription(id: Int, language: String, fallback: String): String =
        occasions[id]?.get(language)?.desc ?: fallback

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
            val occasionJson = json.getJSONObject("occasions")
            occasionJson.keys().forEach { language ->
                val byId = occasionJson.getJSONObject(language)
                byId.keys().forEach { id ->
                    val item = byId.getJSONObject(id)
                    occasionMap.getOrPut(id.toInt()) { mutableMapOf() }[language] =
                        OccasionCopy(item.getString("name"), item.getString("desc"))
                }
            }

            return OthersContent(occasionMap)
        }
    }
}
