<p align="center">
  <img src="logo.svg" width="128" height="128" alt="Barakah App Icon" />
</p>

<h1 align="center">Barakah (بَرَكَة)</h1>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Privacy-100%25-4CAF50?style=flat-square&logo=shield-halved&logoColor=white" alt="Privacy First" />
  <img src="https://img.shields.io/badge/License-GPL--3.0-blue?style=flat-square" alt="License" />
</p>

**Barakah** is a privacy-respecting, fully Free and Open Source (FOSS) Islamic companion application crafted specifically for Android. By pairing Material 3 aesthetics with local offline-first reliability, **Barakah** serves as an eye-safe, advertisement-free spiritual toolkit for daily routines.

## 🌍 Supported Languages

The interface is English or Arabic, switchable in Settings. Widget labels come from Android
resources instead, so they follow the device language: Arabic and German are translated, and
everything else falls back to English.

---

## 🌟 Key Features

* **Home Dashboard**: View real-time prayer schedule trackers, choose voluntary (Nawafil) prayers to track, configure high-fidelity Mecca Al-Haram Adhan background play, and set flexible reminder notifications.
* **Quran Reader**: Access the complete Holy Quran text alongside Arabic Tafseer or English translations, adjustable font scaling, and an optional Uthmani-script face (Amiri Quran) applied to the Quran text alone.
* **Qiblah Compass**: Locate the Kaaba directions instantly using interactive, hardware-sensor calibrated alignment with physical haptic vibrations.
* **Haptic Tasbih**: Maintain focus during remembrance with a digital counter (misbaha) supporting custom targets and discrete haptic pulses.
* **Duas (Supplications)**: Read through compiled *Hisnul Muslim* (حصن المسلم) supplications organized by daily activities and scenarios.

---

## 🛠 Building & Running the Tests

You need JDK 17 and an Android SDK with API 36 installed. The tests additionally need JDK 21,
because Robolectric sandboxes Android 36 only on 21 and above. There are no API keys to obtain
and no Google Play Services dependency.

```bash
./gradlew assembleDebug     # debug APK:   app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease   # release APK: app/build/outputs/apk/release/app-release.apk
./gradlew test              # unit and screenshot tests, on JDK 21
./gradlew lintDebug         # Android Lint
```

The offline city catalog is not committed. Run `python tools/build_city_database.py` once (Python
3.12) before building, otherwise city search quietly returns nothing; passing `--source` and
`--alternate-names-source` uses saved GeoNames exports instead of downloading them. CI builds the
same catalog as a cached step.

Signing: the build expects `app/keystore.jks`. Locally it is materialised from
`debug.keystore.base64` or `debug.keystore`, and a throwaway key is generated when neither
exists, so an APK built on one machine will not install over one built on another. Release builds
are signed from repository secrets.

The screenshot tests compare against the reference images in `app/src/test/screenshots`, which
were recorded on CI. They can fail on another machine, because the rendered image depends on the
runner's fonts and SDK image; dispatch the workflow with `record_screenshots` to make new ones.

---

## 🎨 Design Spec & Aesthetics

* **Material 3** with a light and a dark scheme, Material You dynamic colour on Android 12 and
  above, and an optional pure-black (AMOLED) dark theme.
* **The theme is the app's choice, not the phone's**: light, dark or follow-system is set in
  Settings, and the status and navigation bar icons follow whatever is chosen.
* **Arabic-first typography**: the Amiri Quran face is applied to Quran text alone, so the rest of
  the interface stays on the system font and stays legible at small sizes.
* **Arabic-Indic numerals** by default in Arabic, with a setting to use Western digits instead.

---

## 🔒 Privacy Guarantee

* **Zero Telemetry**: No user tracking, analytics nodes, or telemetry collection processes.
* **Offline Processing**: Real-time astronomical coordinates and physical tracking parameters remain completely local on-device.
* **Forever Ad-Free & Free**: Absolutely zero ads, monetization models, or dark UI design patterns.

---

## 📚 Attributions & Dataset Sources

Barakah relies gratefully on the efforts of the global FOSS Islamic developer community for its offline-ready datasets, reference scripts, and audio assets:

- **Hadith Compilation**: [hadith.json](https://github.com/4thel00z/hadith.json)
- **Hisnul Muslim Supplications**: [HisnElMuslim](https://github.com/asellam/HisnElMuslim)
- **Holy Quran Database**: [Quran-Data](https://github.com/rn0x/Quran-Data)
- **Holy Quran Database & Tafseer**: [Quran-Data](https://github.com/rn0x/Quran-Data), [QuranTafseer-ar-json](https://github.com/00AhmedMokhtar00/QuranTafseer-ar-json), and [tafsir_api](https://github.com/spa5k/tafsir_api) by spa5k
- **Quran Script Font**: [Amiri Quran](https://github.com/aliftype/amiri) © Khaled Hosny and the Amiri Project Authors, licensed under the [SIL Open Font License 1.1](https://openfontlicense.org). The bundled licence text is in [docs/AmiriQuran-OFL.txt](docs/AmiriQuran-OFL.txt).
- **Astronomical Mathematics**: [Adhan SDK](https://github.com/batoulapps/adhan-java)
- **Adhan Audio Assets**: [adhan-mp3](https://github.com/Kiwifu/adhan-mp3) by Kiwifu
- **Offline City Search, Localized Names, and City Timezones**: [GeoNames cities500](https://download.geonames.org/export/dump/cities500.zip) and [GeoNames alternateNamesV2](https://download.geonames.org/export/dump/alternateNamesV2.zip), © GeoNames, licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). The bundled catalog keeps canonical city records separate from language-tagged alternate names. Arabic labels use GeoNames entries tagged as Arabic; when no localized name exists, the app shows the canonical city name instead of guessing from its script. The catalog contains populated places with a population above 500 or designated capitals. It is broad global coverage, but does not claim to include every named village or settlement. GPS mode uses the device's configured timezone; manual city selections use the IANA timezone ID supplied with each city record.

GitHub Actions downloads the GeoNames city and localized-name exports and creates the catalog before building each APK, so lookup works without a network connection after installation. For a local build, install Python 3.12 and run `python tools/build_city_database.py` before Gradle; pass `--source path/to/cities500.zip` and `--alternate-names-source path/to/alternateNamesV2.zip` to use saved GeoNames exports. Checksums of both sources version the app's local database cache, so a newer APK replaces an older cached catalog on its next search.

---

## 📄 License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

---

Made as a humble contribution to the Muslim Ummah. May Allah grant Barakah in our lives. 🤲
