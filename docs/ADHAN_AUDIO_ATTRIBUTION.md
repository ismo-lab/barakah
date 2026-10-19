# Adhan audio attribution

The app bundles two recordings from the Internet Archive, both released by their uploader under
[CC0 1.0 Universal (Public Domain Dedication)](https://creativecommons.org/publicdomain/zero/1.0/),
which permits reuse and redistribution without conditions:

- Fajr: [28 May Fajr Adhan, Makkah](https://archive.org/details/28MayFajrAdhanMakkah) -
  `app/src/main/res/raw/adhan_fajr.mp3`
- Other obligatory prayers: [Makkah Azan - Ramadan](https://archive.org/details/MakkahAzan-ramadan) -
  `app/src/main/res/raw/adhan_regular.mp3`

These replaced two recordings from [Kiwifu/adhan-mp3](https://github.com/Kiwifu/adhan-mp3), which
were described as free for Islamic apps but carried no licence file, so the grant could not be
relied on for redistribution. Nothing else about the playback path changed: `AdhanPlaybackService`
picks `adhan_fajr` for Fajr and `adhan_regular` for every other prayer.
