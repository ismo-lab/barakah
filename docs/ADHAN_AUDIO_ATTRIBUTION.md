# Adhan audio attribution

The app bundles two freely redistributable recordings:

- **Other obligatory prayers:** [Islamic call to prayer from the Prophet's Mosque](https://commons.wikimedia.org/wiki/File:33937_ejaz215_call-to-prayer-from-the-prophet-s-mo.ogg)
  by ejaz215, originally uploaded to [Freesound](https://freesound.org/people/ejaz215/sounds/33937/),
  released under [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/).
  Shipped as `app/src/main/res/raw/adhan_regular.ogg`.
- **Fajr:** [Eid al-Fitr Fajr azan at Malmö Mosque – 19 August 2012](https://commons.wikimedia.org/wiki/File:Eid_al-Fitr_Fajr_azan_at_Malm%C3%B6_Mosque_-_19_August_2012.webm)
  by Islamic Center Malmö / Besim Azemi, released under
  [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/).
  Shipped as `app/src/main/res/raw/adhan_fajr.ogg`.

Required attribution:

> **Regular:** "Islamic call to prayer from the Prophet's Mosque" by ejaz215, CC BY 3.0,
> https://commons.wikimedia.org/wiki/File:33937_ejaz215_call-to-prayer-from-the-prophet-s-mo.ogg,
> originally from https://freesound.org/people/ejaz215/sounds/33937/. Shipped unmodified.
>
> **Fajr:** "Eid al-Fitr Fajr azan at Malmö Mosque – 19 August 2012", Islamic Center Malmö / Besim Azemi,
> CC BY 3.0, https://commons.wikimedia.org/wiki/File:Eid_al-Fitr_Fajr_azan_at_Malm%C3%B6_Mosque_-_19_August_2012.webm.
> Trimmed of silence, faded in and out, re-encoded from stereo Opus to mono Opus.

The regular file is the original Ogg Vorbis recording (186 s). The Fajr file was resampled to 48 kHz
mono Opus at 64 kbps and faded at the edges. Together they are about 5 MB. `AdhanPlaybackService`
selects `adhan_fajr` for Fajr and `adhan_regular` for every other prayer, and `R.raw` names are
unchanged.

The regular recording replaced the previous CC0 "Beautiful adhan". The Fajr recording was chosen for a
genuine Fajr variant (it includes the Fajr-specific *as-salatu khayrun min an-nawm* phrase). Both
earlier Makkah recordings from the Internet Archive were also CC0; they were replaced for the smaller
files and the Fajr variant, not because the licence was invalid. A prior pair from Kiwifu/adhan-mp3
was rejected because it carried no licence file.

The `NonFreeAssets` AntiFeature in the F-Droid recipe is unrelated to audio: it covers the Tafsir
al-Jalalayn and Tanzil translations.
