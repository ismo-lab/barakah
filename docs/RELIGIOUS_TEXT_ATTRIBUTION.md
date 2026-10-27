# Religious text sources

Arabic scripture is always shown unchanged. Scripture translations and tafsir are human-authored
interpretations, labelled as such in the app, never machine translations. When a chosen language
has no bundled edition for a text, the app shows English and states on screen that the requested
language is unavailable. It never silently substitutes a different language without saying so.

## F-Droid status

The source code is GPL-3.0-only. Some bundled translations and tafsir are non-free content used
with attribution under source-specific terms. F-Droid metadata therefore declares
`AntiFeatures: NonFreeAssets`; Barakah does not relicense those texts. Editions whose upstream
terms do not permit redistribution must be removed or replaced before submission.

## Quran translations

Bundled from the editions published by [fawazahmed0/quran-api](https://github.com/fawazahmed0/quran-api)
(Unlicense). Each edition keeps its upstream attribution:

| Language | Edition / translator | Upstream source |
| --- | --- | --- |
| English | Sahih International (Saheeh International) | [rn0x/Quran-Data](https://github.com/rn0x/Quran-Data) |
| Indonesian | Kementerian Agama Republik Indonesia | [QuranEnc](https://quranenc.com/en/browse/indonesian_affairs) |
| Urdu | Muhammad Junagarhi | [Tanzil](https://tanzil.net) |
| Bengali | Abu Bakr Zakaria | King Fahd Complex, via QuranEnc |
| Turkish | Muslim Shahin | QuranEnc |
| Persian | Islamhouse.com Persian Team | QuranEnc |
| Hausa | Abubakar Mahmood Jummi | QuranEnc |
| Malay | Abdullah Muhammad Basmeih | Tanzil |
| Swahili | Ali Muhsin Al Barwani | Tanzil |
| Russian | Elmir Kuliev | Tanzil |
| German | A. S. F. Bubenheim and N. Elyas | [QuranEnc](https://quranenc.com/en/browse/german_bubenheim) |
| Sorani Kurdish | Muhammad Saleh Bamoki | [QuranEnc](https://quranenc.com/en/browse/kurdish_bamoki) |

English is the Sahih International translation, attributed to Saheeh International and bundled
through `rn0x/Quran-Data`. The repository's MIT licence covers its packaging; the translation
remains attributed to its publisher. The English, Urdu, Malay, Swahili and Russian editions are
hosted by [Tanzil](https://tanzil.net) and are used under Tanzil's non-commercial redistribution
terms; Barakah is free and non-commercial. Tanzil asks apps that use more than three of its
translations to link back to [tanzil.net/trans](https://tanzil.net/trans/); Barakah bundles five
and provides that link in Settings > About. The collection repositories' licences cover packaging,
not necessarily every edition's underlying text.

## Tafsir

Bundled from [Tarteel QUL](https://qul.tarteel.ai/resources/tafsir), the [Tafsir Center for Quranic Studies](https://tafsir.net), and [QuranEnc](https://quranenc.com). [spa5k/tafsir_api](https://github.com/spa5k/tafsir_api) is MIT-licensed packaging code; that licence does not cover the bundled tafsir text.

| Language | Edition | Upstream |
| --- | --- | --- |
| Arabic | Tafsir al-Jalalayn | [QUL / Tarteel](https://qul.tarteel.ai/resources/tafsir/523) |
| English | Tafsir al-Jalalayn (English translation; non-free) | [altafsir.com](https://www.altafsir.com/), via [spa5k/tafsir_api](https://github.com/spa5k/tafsir_api) |
| Indonesian | Al-Mukhtasar (QuranEnc conditional republication terms) | QUL / Tarteel |
| Bengali | Tafsir Abu Bakr Zakaria (QuranEnc conditional republication terms) | [QUL / Tarteel](https://qul.tarteel.ai/resources/tafsir/166), King Fahd Complex |
| Turkish | Al-Mukhtasar (QuranEnc conditional republication terms) | QUL / Tarteel |
| Persian | Al-Mukhtasar (QuranEnc conditional republication terms) | QUL / Tarteel |
| Urdu | As-Saadi — Urdu edition V1.0.0 (QuranEnc conditional republication terms) | [QuranEnc](https://quranenc.com/en/browse/urdu_saadi) |
| Russian | Al-Mukhtasar (QuranEnc conditional republication terms) | QUL / Tarteel |
| Sorani Kurdish | Al-Mukhtasar (QuranEnc conditional republication terms) | QUL / Tarteel |

QUL's FAQ states that resource licensing varies by edition and asks projects to review each
author's terms. Barakah does not relicense QUL text; the table records the editions and attribution.
The English tafsir is the Jalalayn English translation published by altafsir.com; it is non-free
and covered by `NonFreeAssets`. The Urdu As-Saadi edition is used under QuranEnc's conditional
republication terms. Bengali is Tafsir Abu Bakr Zakaria (QUL resource 166), not Al-Mukhtasar; it is
also non-free and covered by `NonFreeAssets`. Its legacy glyphs were normalized for display, with
no change to the wording.

The Tafsir Center's official `tafsir-mcp-data` dataset declares CC BY 4.0, but its `quran.db`
contains only `Mukhtasaren`, `Mukhtasarar`, and `Mukhtasarbn` — Arabic, English, and Bengali
Al-Mukhtasar. It does not include the Indonesian, Turkish, Persian, Russian, or Sorani Kurdish
Al-Mukhtasar editions bundled here; those remain under QuranEnc's conditional republication terms.
The dataset's `DATA_SOURCES.md` also says commercial redistribution of tafsir texts requires
permission, which conflicts with its CC BY 4.0 file. Treat the CC BY 4.0 grant as limited to the
exact three-language dataset and do not extend it to other editions without written confirmation.

The tafsir picker lists only languages with a bundled edition. An old saved selection that no longer
has one falls back to English and says so on screen.

## Others screen copy

Occasion names/descriptions are machine-assisted translations in
`app/src/main/assets/others_i18n.json`. The 99 Allah-name meanings/details use the English source in
`AllahNamesData.kt` until native-speaker review; the unreviewed localized copies were removed. These
are not scripture and do not replace the Arabic names.

## Hisn al-Muslim

The Arabic supplications in `hisn.json` come from
[asellam/HisnElMuslim](https://github.com/asellam/HisnElMuslim), MIT License; the licence text is in
[docs/HisnElMuslim-MIT.txt](HisnElMuslim-MIT.txt). The English text in `hisn_en.json` comes from
[YousefAsalya/Islamic-Pro-azkar-API](https://github.com/YousefAsalya/Islamic-Pro-azkar-API), MIT
License; the licence text is in [docs/IslamicProAzkar-MIT.txt](IslamicProAzkar-MIT.txt). Hadith
references from the previous bundled edition are retained where the Arabic text matches exactly;
the remaining entries show no reference.

## Supplication translations

Russian, Persian, and Sorani Kurdish translations come from
[my-prayers/muslim-data-ios](https://github.com/my-prayers/muslim-data-ios), Apache License 2.0.
Items are matched to the bundled Arabic duas by normalized Arabic text; unmatched items fall back
to the bundled English translation with an explicit notice.

Indonesian supplication translations were removed because their provenance and licence could not be
verified. Indonesian now falls back to the bundled English duas with the same explicit notice.

## Data layout

- `app/src/main/assets/quran/translations/<language>/surah_<number>.json` — verse translation
- `app/src/main/assets/quran/tafsir/<language>/<surah>.json` — tafsir
- `app/src/main/res/raw/duas_<language>.json` — supplication translations keyed by normalized Arabic
