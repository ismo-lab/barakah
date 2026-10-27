package dev.barakah.app.data

import android.content.Context
import java.util.Calendar

/** The verse shown on the home screen and sent as the daily verse notification. */
data class DailyVerse(
    val surahId: Int,
    val ayah: Int,
    val arabic: String,
    val text: String,
    val reference: String
)

/** The hadith shown on the home screen and sent as the daily hadith notification. */
data class DailyHadith(
    val text: String,
    val source: String
)

/**
 * The single source for the "Today's Verse" and "Today's Hadith" cards and notifications, so the
 * two always show the same text. Selection is by day of the year, which keeps the content stable
 * for a whole day and rotates at midnight.
 */
object DailyContent {

    private data class VersePick(
        val surahId: Int,
        val ayah: Int,
        val nameEn: String,
        val nameAr: String
    )

    private val versePicks = listOf(
        VersePick(14, 7, "Ibrahim", "إبراهيم"),
        VersePick(2, 186, "Al-Baqarah", "البقرة"),
        VersePick(2, 155, "Al-Baqarah", "البقرة"),
        VersePick(65, 1, "At-Talaq", "الطلاق"),
        VersePick(65, 7, "At-Talaq", "الطلاق")
    )

    /**
     * The verse for [dayOfYear], translated into the language that follows [appLanguage]. Arabic
     * shows the source text; a language with no bundled edition shows English, which is the same
     * fallback the Quran reader announces on screen.
     */
    fun verse(context: Context, appLanguage: String, dayOfYear: Int = currentDayOfYear()): DailyVerse {
        val pick = versePicks[(dayOfYear - 1).mod(versePicks.size)]
        val tag = ReligiousTextLanguages.quranTagFor(appLanguage)
        val verse = QuranData.loadVersesForSurah(context, pick.surahId, tag)
            .verses.firstOrNull { it.index == pick.ayah }
        val arabic = verse?.arabic.orEmpty()
        val text = when {
            appLanguage == "ar" -> arabic
            verse != null -> verse.english
            else -> ""
        }
        val name = if (appLanguage == "ar") pick.nameAr else pick.nameEn
        return DailyVerse(
            surahId = pick.surahId,
            ayah = pick.ayah,
            arabic = arabic,
            text = text,
            reference = UiStrings.format("Surah %s, %s", name, pick.ayah)
        )
    }

    fun hadith(appLanguage: String, dayOfYear: Int = currentDayOfYear()): DailyHadith {
        val entries = hadiths[appLanguage] ?: hadiths.getValue("en")
        return entries[(dayOfYear - 1).mod(entries.size)]
    }

    private fun currentDayOfYear(): Int =
        Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

    // One citation per language; the wording of the hadith itself is the part that matters and is
    // kept in the reader's language.
    private val hadiths: Map<String, List<DailyHadith>> = mapOf(
        "ar" to listOf(
            DailyHadith(
                "«إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى»",
                "صحيح البخاري ومسلم • عن عمر بن الخطاب"
            ),
            DailyHadith(
                "«يَسِّرُوا وَلاَ تُعَسِّرُوا، وَبَشِّرُوا وَلاَ تُنَفِّرُوا»",
                "صحيح البخاري • عن أنس بن مالك"
            ),
            DailyHadith(
                "«مَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الآخِرِ فَلْيَقُلْ خَيْرًا أَوْ لِيَصْمُتْ»",
                "صحيح البخاري • عن أبي هريرة"
            )
        ),
        "en" to listOf(
            DailyHadith(
                "Actions are but by intentions, and every person shall have only that which they intended.",
                "Sahih al-Bukhari & Muslim • Umar ibn Al-Khattab"
            ),
            DailyHadith(
                "Make things easy and do not make them difficult, and cheer people up.",
                "Sahih al-Bukhari • Anas ibn Malik"
            ),
            DailyHadith(
                "Whoever believes in Allah and the Last Day should speak good or remain silent.",
                "Sahih al-Bukhari • Abu Hurayrah"
            )
        ),
        "id" to listOf(
            DailyHadith(
                "Amal itu bergantung pada niat, dan setiap orang mendapat sesuai niatnya.",
                "Sahih al-Bukhari & Muslim • Umar bin Al-Khattab"
            ),
            DailyHadith(
                "Permudahlah dan jangan mempersulit, berilah kabar gembira dan jangan membuat orang menjauh.",
                "Sahih al-Bukhari • Anas bin Malik"
            ),
            DailyHadith(
                "Siapa yang beriman kepada Allah dan hari akhir, hendaklah berkata baik atau diam.",
                "Sahih al-Bukhari • Abu Hurairah"
            )
        ),
        "tr" to listOf(
            DailyHadith(
                "Ameller niyetlere göredir ve herkese niyet ettiği vardır.",
                "Sahih al-Buhari & Müslim • Ömer bin Hattab"
            ),
            DailyHadith(
                "Kolaylaştırın, zorlaştırmayın; müjdeleyin, nefret ettirmeyin.",
                "Sahih al-Buhari • Enes bin Malik"
            ),
            DailyHadith(
                "Allah'a ve ahiret gününe inanan ya hayır söylesin ya da sussun.",
                "Sahih al-Buhari • Ebu Hureyre"
            )
        ),
        "ur" to listOf(
            DailyHadith(
                "اعمال کا دارومدار نیتوں پر ہے، اور ہر شخص کو وہی ملے گا جس کی اس نے نیت کی۔",
                "صحیح بخاری و مسلم • عمر بن خطاب"
            ),
            DailyHadith(
                "آسانی کرو، تنگی نہ کرو اور خوشخبری دو، نفرت نہ دلاؤ۔",
                "صحیح بخاری • انس بن مالک"
            ),
            DailyHadith(
                "جو اللہ اور آخرت کے دن پر ایمان رکھتا ہے وہ اچھی بات کہے یا خاموش رہے۔",
                "صحیح بخاری • ابو ہریرہ"
            )
        ),
        "fa" to listOf(
            DailyHadith(
                "اعمال به نیت‌ها بستگی دارند و هر کس همان چیزی را می‌یابد که نیت کرده است.",
                "صحیح بخاری و مسلم • عمر بن خطاب"
            ),
            DailyHadith(
                "آسان بگیرید و سخت نگیرید و بشارت دهید و مردم را بیزار نکنید.",
                "صحیح بخاری • انس بن مالک"
            ),
            DailyHadith(
                "هر کس به خدا و روز قیامت ایمان دارد، سخن نیک بگوید یا سکوت کند.",
                "صحیح بخاری • ابوهریره"
            )
        ),
        "bn" to listOf(
            DailyHadith(
                "কাজ নিয়তের ওপর নির্ভরশীল, প্রত্যেকে তাই পাবে যা সে নিয়ত করেছে।",
                "সহিহ বুখারি ও মুসলিম • উমর ইবনুল খাত্তাব"
            ),
            DailyHadith(
                "সহজ করো, কঠিন করো না; সুসংবাদ দাও, বিতাড়িত করো না।",
                "সহিহ বুখারি • আনাস ইবনু মালিক"
            ),
            DailyHadith(
                "যে আল্লাহ ও শেষ দিনে বিশ্বাস করে, সে ভালো কথা বলুক অথবা নীরব থাকুক।",
                "সহিহ বুখারি • আবু হুরাইরা"
            )
        ),
        "ru" to listOf(
            DailyHadith(
                "Поистине, дела оцениваются только по намерениям, и каждому воздастся лишь за то, что он намеревался совершить.",
                "Сахих аль-Бухари и Муслим • Умар ибн аль-Хаттаб"
            ),
            DailyHadith(
                "Облегчайте, а не затрудняйте; радуйте, а не отталкивайте.",
                "Сахих аль-Бухари • Анас ибн Малик"
            ),
            DailyHadith(
                "Пусть верующий в Аллаха и Судный день говорит благое или молчит.",
                "Сахих аль-Бухари • Абу Хурайра"
            )
        ),
        "ms" to listOf(
            DailyHadith(
                "Sesungguhnya amalan itu bergantung pada niat, dan setiap orang mendapat menurut niatnya.",
                "Sahih al-Bukhari & Muslim • Umar bin al-Khattab"
            ),
            DailyHadith(
                "Permudahkanlah dan jangan menyusahkan, berilah khabar gembira dan jangan menjauhkan orang.",
                "Sahih al-Bukhari • Anas bin Malik"
            ),
            DailyHadith(
                "Sesiapa beriman kepada Allah dan hari akhirat hendaklah berkata baik atau diam.",
                "Sahih al-Bukhari • Abu Hurairah"
            )
        ),
        "sw" to listOf(
            DailyHadith(
                "Hakika matendo hulipwa kwa nia, na kila mtu atapata alichokinuia.",
                "Sahih al-Bukhari na Muslim • Umar ibn al-Khattab"
            ),
            DailyHadith(
                "Rahisisheni wala msifanye mambo kuwa magumu; toeni habari njema wala msikatishe tamaa.",
                "Sahih al-Bukhari • Anas ibn Malik"
            ),
            DailyHadith(
                "Mwenye kumwamini Allah na Siku ya Mwisho aseme mema au anyamaze.",
                "Sahih al-Bukhari • Abu Hurayrah"
            )
        ),
        "ha" to listOf(
            DailyHadith(
                "Ayyuka suna dogara ne da niyya, kuma kowane mutum yana da abin da ya yi niyya.",
                "Sahih al-Bukhari da Muslim • Umar bn al-Khattab"
            ),
            DailyHadith(
                "Ku sauƙaƙa, kada ku tsananta; ku yi bushara, kada ku kore mutane.",
                "Sahih al-Bukhari • Anas bn Malik"
            ),
            DailyHadith(
                "Wanda ya yi imani da Allah da Ranar Lahira, ya faɗi alheri ko ya yi shiru.",
                "Sahih al-Bukhari • Abu Hurayra"
            )
        ),
        "ckb" to listOf(
            DailyHadith(
                "کردارەکان بە نیەتەکانن و هەر کەسێک بەوە دەگات کە نیەتی کردووە.",
                "سەحیحی بوخاری و موسلیم • عومەر"
            ),
            DailyHadith(
                "ئاسانکاری بکەن و سەختی مەکەن؛ مژدە بدەن و بێزار مەکەن.",
                "سەحیحی بوخاری • ئەنەس"
            ),
            DailyHadith(
                "هەرکەس باوەڕی بە خوا و ڕۆژی دوایی هەبێت، با قسەی چاک بکات یان بێدەنگ بێت.",
                "سەحیحی بوخاری • ئەبو هورەیرە"
            )
        ),
        "de" to listOf(
            DailyHadith(
                "Taten richten sich nach den Absichten, und jedem Menschen steht nur zu, was er beabsichtigt hat.",
                "Sahih al-Bukhari & Muslim • Umar ibn Al-Khattab"
            ),
            DailyHadith(
                "Erleichtert und erschwert nicht; verkündet frohe Botschaft und schreckt nicht ab.",
                "Sahih al-Bukhari • Anas ibn Malik"
            ),
            DailyHadith(
                "Wer an Allah und den Jüngsten Tag glaubt, soll Gutes sprechen oder schweigen.",
                "Sahih al-Bukhari • Abu Hurayra"
            )
        )
    )
}
