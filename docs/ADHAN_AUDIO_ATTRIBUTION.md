# Adhan audio attribution

The app bundles two freely redistributable recordings:

- **Other obligatory prayers:** [Beautiful adhan](https://commons.wikimedia.org/wiki/File:Beautiful_adhan.ogg)
  by Adam-synagda, released under [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/).
  Shipped as `app/src/main/res/raw/adhan_regular.ogg`.
- **Fajr:** [Eid al-Fitr Fajr azan at Malmö Mosque – 19 August 2012](https://commons.wikimedia.org/wiki/File:Eid_al-Fitr_Fajr_azan_at_Malm%C3%B6_Mosque_-_19_August_2012.webm)
  by Islamic Center Malmö / Besim Azemi, released under
  [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/).
  Shipped as `app/src/main/res/raw/adhan_fajr.ogg`.

Required attribution for the Fajr recording:

> "Eid al-Fitr Fajr azan at Malmö Mosque – 19 August 2012", Islamic Center Malmö / Besim Azemi,
> CC BY 3.0, https://commons.wikimedia.org/wiki/File:Eid_al-Fitr_Fajr_azan_at_Malm%C3%B6_Mosque_-_19_August_2012.webm.
> Trimmed of silence, faded in and out, re-encoded from stereo Opus to mono Opus.

Both files were resampled to 48 kHz mono Opus at 64 kbps and faded at the edges. Total size is
3.4 MB, down from 8.8 MB for the previous pair. `AdhanPlaybackService` is unchanged: it selects
`adhan_fajr` for Fajr and `adhan_regular` for every other prayer, and `R.raw` names are unchanged.

These replaced two Makkah recordings from the Internet Archive that were also CC0. The change was
made for a genuine Fajr variant (the Malmö recording includes the Fajr-specific *as-salatu
khayrun min an-nawm* phrase) and smaller files, not because the previous licence was invalid.
A prior pair from Kiwifu/adhan-mp3 was rejected because it carried no licence file.

The `NonFreeAssets` AntiFeature in the F-Droid recipe is unrelated to audio: it covers the Tafsir
al-Jalalayn and Tanzil translations.
