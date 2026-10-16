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

Only English and Arabic are supported.

---

## 🌟 Key Features

* **Home Dashboard**: View real-time prayer schedule trackers, choose voluntary (Nawafil) prayers to track, configure high-fidelity Mecca Al-Haram Adhan background play, and set flexible reminder notifications.
* **Quran Reader**: Access the complete Holy Quran text alongside Arabic Tafseer or English translations, adjustable font scaling, and an optional Uthmani-script face (Amiri Quran) applied to the Quran text alone.
* **Qiblah Compass**: Locate the Kaaba directions instantly using interactive, hardware-sensor calibrated alignment with physical haptic vibrations.
* **Haptic Tasbih**: Maintain focus during remembrance with a digital counter (misbaha) supporting custom targets and discrete haptic pulses.
* **Duas (Supplications)**: Read through compiled *Hisnul Muslim* (حصن المسلم) supplications organized by daily activities and scenarios.

---

## 🎨 Design Spec & Aesthetics

Built from the ground up for Material Design 3

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
