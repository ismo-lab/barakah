# Religious text sources

Arabic scripture is always shown unchanged. Scripture translations and tafsir are human-authored
interpretations, labelled as such in the app, never machine translations. When a chosen language
has no bundled edition for a text, the app shows English and states on screen that the requested
language is unavailable. It never silently substitutes a different language without saying so.

## Quran translations

Bundled from the editions published by [fawazahmed0/quran-api](https://github.com/fawazahmed0/quran-api)
(Unlicense). Each edition keeps its upstream attribution:

| Language | Edition / translator | Upstream source |
| --- | --- | --- |
| Indonesian | Kementerian Agama Republik Indonesia | [QuranEnc](https://quranenc.com/en/browse/indonesian_affairs) |
| Urdu | Muhammad Junagarhi | [Tanzil](https://tanzil.net) |
| Bengali | Abu Bakr Zakaria | King Fahd Complex, via QuranEnc |
| Turkish | Muslim Shahin | QuranEnc |
| Persian | Islamhouse.com Persian Team | QuranEnc |
| Hausa | Abubakar Mahmood Jummi | QuranEnc |
| Malay | Abdullah Muhammad Basmeih | Tanzil |
| Swahili | Ali Muhsin Al Barwani | Tanzil |

The bundled English translation is the one already shipped in the app's surah assets; its
translator attribution is not changed here.

Before publishing a build, confirm the redistribution terms of each upstream edition. The
Unlicense covers the collection repository, not necessarily every edition's underlying text.

## Tafsir

Bundled from [Tarteel QUL](https://qul.tarteel.ai/resources/tafsir), the [Tafsir Center for Quranic Studies](https://tafsir.net), and [QuranEnc](https://quranenc.com). [spa5k/tafsir_api](https://github.com/spa5k/tafsir_api) is MIT-licensed packaging code; that licence does not cover the bundled tafsir text.

| Language | Edition | Upstream |
| --- | --- | --- |
| Arabic | Tafsir al-Jalalayn | [QUL / Tarteel](https://qul.tarteel.ai/resources/tafsir/523) |
| English | Al-Mukhtasar (CC BY 4.0) | Tafsir Center for Quranic Studies, via [spa5k/tafsir_api](https://github.com/spa5k/tafsir_api) |
| Indonesian | Al-Mukhtasar | QUL / Tarteel |
| Bengali | Al-Mukhtasar | QUL / Tarteel |
| Turkish | Al-Mukhtasar | QUL / Tarteel |
| Persian | Al-Mukhtasar | QUL / Tarteel |
| Urdu | As-Saadi — Urdu edition V1.0.0 (QuranEnc conditional republication terms) | [QuranEnc](https://quranenc.com/en/browse/urdu_saadi) |
| Russian | Al-Mukhtasar | QUL / Tarteel |
| Sorani Kurdish | Al-Mukhtasar | QUL / Tarteel |

The tafsir picker lists only languages with a bundled edition. An old saved selection that no longer
has one falls back to English and says so on screen.

## Others screen copy

Occasion names/descriptions and the 99 Allah-name meanings/details are machine-assisted
translations in `app/src/main/assets/others_i18n.json`. They are not scripture and do not replace
the Arabic names. Native-speaker review is required before treating them as final.

## Supplication translations

Russian, Persian, and Sorani Kurdish translations come from
[my-prayers/muslim-data-ios](https://github.com/my-prayers/muslim-data-ios), Apache License 2.0.
Items are matched to the bundled Arabic duas by normalized Arabic text; unmatched items fall back
to the bundled English translation with an explicit notice.

## Data layout

- `app/src/main/assets/quran/translations/<language>/surah_<number>.json` — verse translation
- `app/src/main/assets/quran/tafsir/<language>/<surah>.json` — tafsir
- `app/src/main/res/raw/duas_<language>.json` — supplication translations keyed by normalized Arabic
