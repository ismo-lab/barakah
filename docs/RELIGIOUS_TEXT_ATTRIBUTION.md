# Religious text sources

Arabic scripture is always shown unchanged. Translations and tafsir are human-authored
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

Bundled from [spa5k/tafsir_api](https://github.com/spa5k/tafsir_api) (MIT):

| Language | Edition | Upstream |
| --- | --- | --- |
| Arabic | Tafsir al-Jalalayn | [QUL / Tarteel](https://qul.tarteel.ai/resources/tafsir/523) |
| English | Tafsir al-Jalalayn (pre-existing asset) | [Altafsir](https://www.altafsir.com) |
| Indonesian | Al-Mukhtasar | QUL / Tarteel |
| Bengali | Al-Mukhtasar | QUL / Tarteel |
| Turkish | Al-Mukhtasar | QUL / Tarteel |
| Persian | Al-Mukhtasar | QUL / Tarteel |

Languages without a bundled tafsir show the English tafsir with an explicit notice.

## Supplication translations

Russian, Persian, and Sorani Kurdish translations come from
[my-prayers/muslim-data-ios](https://github.com/my-prayers/muslim-data-ios), Apache License 2.0.
Items are matched to the bundled Arabic duas by normalized Arabic text; unmatched items fall back
to the bundled English translation with an explicit notice.

## Data layout

- `app/src/main/assets/quran/translations/<language>/surah_<number>.json` — verse translation
- `app/src/main/assets/quran/tafsir/<language>/<surah>.json` — tafsir
- `app/src/main/res/raw/duas_<language>.json` — supplication translations keyed by normalized Arabic
