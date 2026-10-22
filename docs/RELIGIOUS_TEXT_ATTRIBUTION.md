# Religious text sources

Arabic scripture is always shown unchanged. Scripture translations and tafsir are human-authored
interpretations, labelled as such in the app, never machine translations. When a chosen language
has no bundled edition for a text, the app shows English and states on screen that the requested
language is unavailable. It never silently substitutes a different language without saying so.

## F-Droid status

The source code is GPL-3.0-only. Some bundled translations and tafsir are non-free content used
with attribution under non-commercial terms. F-Droid metadata therefore declares
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

English is the Sahih International translation, attributed to Saheeh International and bundled
through `rn0x/Quran-Data`. The repository's MIT licence covers its packaging; the translation
remains attributed to its publisher. The English, Urdu, Malay and Swahili editions are hosted by
[Tanzil](https://tanzil.net) and are used under Tanzil's non-commercial redistribution terms;
Barakah is free and non-commercial. Tanzil asks apps that use more than three of its translations
to link back to [tanzil.net/trans](https://tanzil.net/trans/); Barakah bundles four and provides
that link in Settings > About. The collection repositories' licences cover packaging, not
necessarily every edition's underlying text.

## Tafsir

Bundled from [Tarteel QUL](https://qul.tarteel.ai/resources/tafsir), the [Tafsir Center for Quranic Studies](https://tafsir.net), and [QuranEnc](https://quranenc.com). [spa5k/tafsir_api](https://github.com/spa5k/tafsir_api) is MIT-licensed packaging code; that licence does not cover the bundled tafsir text.

| Language | Edition | Upstream |
| --- | --- | --- |
| Arabic | Tafsir al-Jalalayn | [QUL / Tarteel](https://qul.tarteel.ai/resources/tafsir/523) |
| English | Al-Mukhtasar (CC BY 4.0) | Tafsir Center for Quranic Studies, via [spa5k/tafsir_api](https://github.com/spa5k/tafsir_api) |
| Indonesian | Al-Mukhtasar | QUL / Tarteel |
| Bengali | Tafsir Abu Bakr Zakaria | QUL / Tarteel |
| Turkish | Al-Mukhtasar | QUL / Tarteel |
| Persian | Al-Mukhtasar | QUL / Tarteel |
| Urdu | As-Saadi — Urdu edition V1.0.0 (QuranEnc conditional republication terms) | [QuranEnc](https://quranenc.com/en/browse/urdu_saadi) |
| Russian | Al-Mukhtasar | QUL / Tarteel |
| Sorani Kurdish | Al-Mukhtasar | QUL / Tarteel |

QUL's FAQ states that resource licensing varies by edition and asks projects to review each
author's terms. Barakah does not relicense QUL text; the table records the editions and attribution.
The English Al-Mukhtasar is CC BY 4.0, and the Urdu As-Saadi edition is used under QuranEnc's
conditional republication terms. Bengali uses Tafsir Abu Bakr Zakaria because the QUL Bengali
Al-Mukhtasar copy contains legacy font glyphs; the bundled text is the upstream edition, unedited.

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
