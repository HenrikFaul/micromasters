# Adatvédelmi tájékoztató — MicroMasters

**Hatályos: 2026-06-30 · `com.micromasters.game`**

## Rövid összefoglaló

A MicroMasters **nem gyűjt, nem továbbít és nem oszt meg semmilyen személyes
adatot.** A játék teljesen **offline** működik, és **nincs internet-hozzáférése**
(az alkalmazás nem kér `INTERNET` engedélyt).

## Milyen adatot kezel az alkalmazás?

Kizárólag a **játékmenethez szükséges állapotot**, ami **csak a te eszközödön**
tárolódik, és soha nem hagyja el azt:

- felfedezett találmányok (a Kódex tartalma),
- napi széria és a legutóbbi játéknap,
- legyőzött Masterek, összegyűjtött szilánkok, futó expedíciók,
- a csatába állított hős, valamint a hang ki/be beállítás,
- esetleges összeomlási hibanapló (stack trace) — kizárólag helyben, a hibaképernyő
  megjelenítéséhez; ez sem kerül elküldésre sehová.

Ezeket az adatokat az Android `SharedPreferences` és a beágyazott játékmotor
`localStorage` tárolója őrzi, **a te eszközödön**.

## Amit az alkalmazás NEM tesz

- ❌ Nem gyűjt személyes adatot (név, e-mail, hely, eszközazonosító).
- ❌ Nem használ analitikát, hirdetést vagy harmadik féltől származó követőt
  (nincs beépítve ad-, analytics- vagy crash-reporting SDK).
- ❌ Nem fér hozzá a kamerához, mikrofonhoz, névjegyekhez, fájlokhoz.
- ❌ Nem küld adatot szerverre — nincs hálózati kommunikáció.

## Engedélyek

Az alkalmazás **egyetlen veszélyes engedélyt sem** kér. Hálózati engedélye nincs,
a cleartext (titkosítatlan HTTP) forgalom kifejezetten **tiltva** van.

## Az adataid törlése

Mivel minden adat helyben tárolódik, az **alkalmazás adatainak törlésével**
(Beállítások → Alkalmazások → MicroMasters → Tárhely → Adatok törlése) vagy az
alkalmazás eltávolításával minden mentett állapot véglegesen törlődik.

## Gyermekek

Az alkalmazás nem gyűjt adatot, így gyermekek számára is biztonságosan használható.

## Kapcsolat

Kérdés esetén: a projekt repójában nyitható issue
(`github.com/HenrikFaul/micromasters`).

---

*Ez a tájékoztató az alkalmazás tényleges működését írja le: a build nem tartalmaz
hálózati, analitikai vagy hirdetési komponenst, és ezt a forráskód, valamint a
hiányzó `INTERNET` engedély igazolja.*
