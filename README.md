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
  <a href="https://github.com/ismo-lab/barakah/actions/workflows/android.yml"><img src="https://github.com/ismo-lab/barakah/actions/workflows/android.yml/badge.svg" alt="Android CI" /></a>
</p>

**Barakah** is a privacy-respecting, fully Free and Open Source (FOSS) Islamic companion application crafted specifically for Android. By pairing Material 3 aesthetics with local offline-first reliability, **Barakah** serves as an eye-safe, advertisement-free spiritual toolkit for daily routines.

**Get the app:** the latest signed APK is on the [Releases](https://github.com/ismo-lab/barakah/releases) page as `Barakah-v<version>.apk`.

## 🌟 Key Features

* **Home Dashboard**: View real-time prayer schedule trackers, choose voluntary (Nawafil) prayers to track, configure high-fidelity Mecca Al-Haram Adhan background play, and set flexible reminder notifications.
* **Hijri Calendar**: The home screen shows today's Hijri date, and the occasions list is dated from it. A ±2-day shift in Settings lines the calendar up with the moon sighting your community follows.
* **Quran Reader**: Access the complete Holy Quran text alongside Arabic Tafseer or English translations, adjustable font scaling, and an optional Uthmani-script face (Amiri Quran) applied to the Quran text alone.
* **Qiblah Compass**: Locate the Kaaba directions instantly using interactive, hardware-sensor calibrated alignment with physical haptic vibrations.
* **Haptic Tasbih**: Maintain focus during remembrance with a digital counter (misbaha) supporting custom targets and discrete haptic pulses.
* **Duas (Supplications)**: Read through compiled *Hisnul Muslim* (حصن المسلم) supplications organized by daily activities and scenarios.
* **Home-Screen Widgets**: Three of them - the daily prayer times, a live countdown to the next prayer, and the voluntary (Nawafil) times.
* **Any Window Size**: The layout follows the window it is given rather than a fixed orientation. A phone keeps the bottom bar, a tablet or unfolded foldable gets a navigation rail, and a list with a detail view shows both side by side once there is room for them, so nothing is stretched across a wide screen or squeezed into a narrow one.

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

The offline city catalog is committed as `app/src/main/assets/cities500.sqlite`, so building needs no
network. `python tools/build_city_database.py` (Python 3.12+) regenerates it from the GeoNames
exports; pass `--source` and `--alternate-names-source` to reuse saved exports instead of downloading
them. Checksums of the two exports are stored beside it in `cities500.version` and version the
on-device cache, so a newer APK replaces an older cached catalog on its next search.

Signing: the build expects `app/keystore.jks`. Locally it is materialised from
`debug.keystore.base64` or `debug.keystore`, and a throwaway key is generated when neither
exists, so an APK built on one machine will not install over one built on another. Release builds
are signed from repository secrets.

The screenshot tests compare against the reference images in `app/src/test/screenshots`, which
were recorded on CI. They can fail on another machine, because the rendered image depends on the
runner's fonts and SDK image; dispatch the workflow with `record_screenshots` to make new ones.

---

## 🔒 Privacy Guarantee

* **No Network Permission**: Barakah does not declare `android.permission.INTERNET`, so it has no way to send anything anywhere, even in principle. The Quran, tafseer, supplications and the city catalog are all bundled in the APK. Android cloud backup and device-to-device transfer are disabled, so app data stays on the device.
* **Zero Telemetry**: No user tracking, analytics nodes, or telemetry collection processes.
* **Offline Processing**: Real-time astronomical coordinates and physical tracking parameters remain completely local on-device.
* **Forever Ad-Free & Free**: Absolutely zero ads, monetization models, or dark UI design patterns.

---

## 📚 Attributions & Dataset Sources

Barakah relies gratefully on the efforts of the global FOSS Islamic developer community for its offline-ready datasets, reference scripts, and audio assets:

- **Hadith Compilation**: [hadith.json](https://github.com/4thel00z/hadith.json)
- **Hisnul Muslim Supplications**: [HisnElMuslim](https://github.com/asellam/HisnElMuslim)
- **Holy Quran Database**: [Quran-Data](https://github.com/rn0x/Quran-Data)
- **Holy Quran Database & Tafseer**: [Quran-Data](https://github.com/rn0x/Quran-Data) (MIT) for the text, and [tafsir_api](https://github.com/spa5k/tafsir_api) by spa5k (MIT) for the Arabic and English *Tafsir al-Jalalayn*, a classical work in the public domain
- **Quran Script Font**: [Amiri Quran](https://github.com/aliftype/amiri) © Khaled Hosny and the Amiri Project Authors, licensed under the [SIL Open Font License 1.1](https://openfontlicense.org). The bundled licence text is in [docs/AmiriQuran-OFL.txt](docs/AmiriQuran-OFL.txt).
- **Astronomical Mathematics**: [Adhan SDK](https://github.com/batoulapps/adhan-java)
- **Adhan Audio Assets**: two Makkah recordings released under [CC0](https://creativecommons.org/publicdomain/zero/1.0/) on the Internet Archive - [Fajr adhan](https://archive.org/details/28MayFajrAdhanMakkah) and [regular adhan](https://archive.org/details/MakkahAzan-ramadan). See [docs/ADHAN_AUDIO_ATTRIBUTION.md](docs/ADHAN_AUDIO_ATTRIBUTION.md).
- **Offline City Search, Localized Names, and City Timezones**: [GeoNames cities500](https://download.geonames.org/export/dump/cities500.zip) and [GeoNames alternateNamesV2](https://download.geonames.org/export/dump/alternateNamesV2.zip), © GeoNames, licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). The bundled catalog keeps canonical city records separate from language-tagged alternate names. Arabic labels use GeoNames entries tagged as Arabic; when no localized name exists, the app shows the canonical city name instead of guessing from its script. The catalog contains populated places with a population above 500 or designated capitals. It is broad global coverage, but does not claim to include every named village or settlement. GPS mode uses the device's configured timezone; manual city selections use the IANA timezone ID supplied with each city record.

---

## 📄 License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

---

Made as a humble contribution to the Muslim Ummah. May Allah grant Barakah in our lives. 🤲

---

**A note on Google AI Studio:** Barakah started as a small personal app built with Google AI Studio. As it grew into a full application, development moved away from that platform. Any Google AI Studio note or reference left in the repository is historical, not a description of how the project is built today.
