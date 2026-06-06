# ✨ Barakah (بَرَكَة) ✨

[![Android Build Status](https://img.shields.io/badge/Android-Build--Passing-success?style=for-the-badge&logo=android&logoColor=white&color=3DDC84)](https://github.com/ismo-lab/barakah)
[![FOSS](https://img.shields.io/badge/Open--Source-FOSS-blue?style=for-the-badge&logo=open-source&logoColor=white&color=007ACC)](https://github.com/ismo-lab/barakah)
[![Privacy First](https://img.shields.io/badge/Privacy-100%25-green?style=for-the-badge&logo=shield&logoColor=white&color=4CAF50)](#privacy-first)

**Barakah** is a privacy-respecting, fully Free and Open Source (FOSS) Islamic companion application crafted specifically for Android. By pairing state-of-the-art Material 3 aesthetics with local offline-first reliability, **Barakah** serves as an eye-safe, advertisement-free spiritual toolkit for daily routines.

## 🌍 Supported Languages / اللغات المدعومة

This companion app exclusively supports the following languages:
- **العربية (Arabic)**: Native RTL layout rendering, complete support for Quranic scripts, offline Arabic Tafseer, and day-to-day adhkar.
- **English**: Fully localized interfaces, English Quran translations, and local Tafsir al-Jalalayn (English) companion texts.

No other languages are actively targeted, ensuring a highly optimized, lightweight, and focused offline bundle size.

---

## 🌟 Key Pillars & Features

### 🕋 Real-time Qibla Companion
- An interactive, hardware-sensor calibrated compass indicating the precise direction of the Kaaba (Makkah).
- Smooth vector rendering with direct sensory vibrational tactile feedback.

### 🕌 Precision Prayer Coordinates & Synced Adhan Alerts
- Dynamic latitude/longitude calculations using the official robust **Adhan** algorithm.
- Location auto-detection via secure GPS or lightweight, offline global city index searching.
- Custom adjustments per prayer time and custom audio alert notification presets for Adhkar and Adhan timings.

### 📖 Pure Reading Experience (Quran & Hadith)
- High-contrast typography optimized for long-form reading with custom font sizing.
- Curated index of **Hisnul Muslim** (حصن المسلم) supplications organized by daily context (morning, evening, travel).
- Live-updated, responsive Arabic text accompanied by verified English translations.

### 📿 Digital Tasbih Counter
- Persistent, offline digital misbaha with customizable target numbers.
- Gentle dynamic haptic ripples on completion for eyes-free invocation tracking.

---

## 🎨 Design Spec & Aesthetics

Built from the ground up for Material Design 3 (M3):
- **🌌 Cosmic Obsidian Palette**: Rich midnight dark layouts that prioritize night-time legibility and save battery on AMOLED displays.
- **✨ Fluid Layout Transitions**: High-frequency animated state transitions for uninterrupted page switching.
- **📱 Dynamic Edge-to-Edge**: Seamless integration with Android navigation bars and status regions.

---

## 🛠️ Stack & Modern Underpinnings

- **Kotlin & Coroutines**: Reactive state streams driven via `StateFlow` and structured concurrency.
- **Room Database**: Optimized local relational persistence for bookmark indexing.
- **Glance**: Custom home screen widgets allowing quick glance-ability directly on the Android launcher.
- **Secrets Gradle Plugin**: Secure build configurations injecting system bindings.

---

## 🔬 Data Attributions & Sources

This app aggregates verified, authenticated Islamic scholarly datasets:
- **Hadith Compilation**: [hadith.json](https://github.com/4thel00z/hadith.json)
- **Hisnul Muslim Supplications**: [HisnElMuslim](https://github.com/asellam/HisnElMuslim)
- **Holy Quran Database & Tafseer**: [Quran-Data](https://github.com/rn0x/Quran-Data), [QuranTafseer-ar-json](https://github.com/00AhmedMokhtar00/QuranTafseer-ar-json), and [tafsir_api](https://github.com/spa5k/tafsir_api) by spa5k
- **Astronomical Mathematics**: [Adhan SDK](https://github.com/batoulapps/adhan-java)
- **Adhan Audio Assets**: [adhan-mp3](https://github.com/Kiwifu/adhan-mp3) by Kiwifu

---

## 🔒 Privacy Guarantee

- **Zero Telemetry**: No tracking codes, analytics nodes, or background error beacons.
- **Offline Integrity**: Your precise physical location stays on-client and is never transmitted to remote services.
- **Forever Free & Open**: No paywalls, premium plans, or dark patterns.

Made as a humble contribution to the Muslim Ummah. May Allah grant Barakah in our lives. 🤲
