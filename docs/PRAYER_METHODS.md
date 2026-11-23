# Prayer calculation methods

The app computes times from solar geometry, not from a bundled timetable. A *method* is a set of
twilight angles (or, for Umm al-Qura, an interval after Maghrib) chosen by an authority. Two apps
using the same geometry can differ by minutes because they follow different authorities; that is a
choice, not a measurement error.

## Defaults

- Explicit choice in Settings always wins.
- Otherwise the saved location country selects a method where one is documented below.
- Otherwise, and for the initial install with no saved country, **Muslim World League (Fajr 18°,
  Isha 17°)**.

`PrayerMethodResolver` is the single reader for the UI, widgets and the alarm scheduler, so all
three agree on the method in force.

| ISO | Method | Fajr | Isha |
|---|---|---:|---:|
| MA | Morocco | 19° | 17° |
| DZ | Algeria | 18° | 17° |
| TN | Tunisia | 18° | 18° |
| EG | Egypt | 19.5° | 17.5° |
| TR | Turkey (Diyanet approximation) | 18° | 17° |
| IR | Tehran | 17.7° | 14° |
| ID | Kemenag (Indonesia) | 20° | 18° |
| MY, BN | JAKIM | 20° | 18° |
| SG | Singapore (MUIS) | 20° | 18° |
| PK | Karachi | 18° | 18° |
| SA | Umm al-Qura | 18.5° | Maghrib + 90 min (+120 in Ramadan) |
| KW | Kuwait | 18° | 17.5° |
| AE | Dubai | 18.2° | 18.2° |
| RU | Russia | 16° | 15° |
| FR | UOIF | 12° | 12° |
| US, CA | ISNA | 15° | 15° |

Angles from [AlAdhan's method list](https://api.aladhan.com/v1/methods) and its
[calculation-method documentation](https://aladhan.com/calculation-methods). Morocco is also
described as using the Muslim World League family by the Moroccan Ministry of Habous; AlAdhan gives
its own 19°/17° entry, which is what the app uses. Morocco also shifts every clock time 60 minutes
earlier on top of the angles, matching the country's published timetable. The shift lands in
`PrayerCalculator.calculate()`, so Home, the widgets, the alarms and the nawafil derived from those
times (Duha, Witr, Tahajjud, Qiyam-ul-Layl) all move with it.

Only angle-based methods are mapped. Authorities that publish fixed monthly tables or add minutes
to Maghrib (for example Qatar, Jordan and Portugal) are deliberately left out rather than
approximated. An unlisted country keeps the MWL fallback, and the picker lets anyone override it.

## Other entries in the picker

`EGYPT_BIS` is the revised Egyptian 20°/18° set. `TURKEY` and `SINGAPORE` approximate the published
Diyanet and MUIS tables with angles; those authorities also issue fixed tables that can differ by
a few minutes.
