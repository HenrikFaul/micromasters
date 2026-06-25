# MicroMasters 🌍

**Hyper-casual · Idle · Strategy** — natív Android játék Kotlinban, a mellékelt
design (MICROMASTERS koncepció) alapján megvalósítva.

> Építsd, fejleszd és irányítsd apró egységeidet mikroszkopikus világokban.
> Gyűjts erőforrásokat, fejleszd egységeidet és hódíts meg új területeket –
> mindezt egyszerű, intuitív vezérléssel.

---

## 📲 Az APK letöltése

A játék **valódi, telepíthető `MicroMasters.apk`** fájlt épít a hivatalos Android
SDK-val, a **GitHub Actions** segítségével (a felhős környezet Google-tűzfala miatt
a build a CI-ban fut, nem itt).

Két helyről szerezheted meg a kész APK-t a `claude/beautiful-euler-8659ry` ágra
történő push után:

1. **Actions → „Build MicroMasters APK” futás → Artifacts → `MicroMasters-apk`**
2. **Releases → „MicroMasters — legfrissebb build” → `MicroMasters.apk`**

Telepítés: másold az APK-t a telefonodra, és engedélyezd az *ismeretlen forrásból*
való telepítést.

---

## 🎮 Mit tud a játék?

A mockup mind az 5 képernyője és a teljes játékmenet megvalósult:

| Képernyő | Tartalom |
|---|---|
| **Nyitókép** | MICRO MASTERS logó, lebegő bolygó, `JÁTSSZ` + `Bejelentkezés` |
| **Világválasztó** | Konyha, Fürdőszoba (nyitva), Kert, Űrhajó (gyémánttal feloldható), erőforrás-sáv, alsó menü |
| **Játékképernyő** | Animált mikrovilág (mászkáló egységek, erőforrás-csomópontok), élő termelés, `GYŰJTÉS` |
| **Fejlesztések** | `Egységek` / `Épületek` / `Boostok` fülek, szintezés, költségek |
| **Napi jutalom** | 7 napos jutalomtábla (500 → 5000 💎), sorozat (streak) |

### Játékmenet / mechanikák
- **Idle termelés:** 4 egységtípus (Bányász, Hordár, Őr, Tudós) folyamatosan termel
  érmét, valós időben felhalmozva (raktárkapacitásig).
- **Offline bevétel:** távollét alatt is termelsz (max. 8 óra), visszatéréskor üdvözlő üzenettel.
- **Fejlesztés:** egységek és épületek (Raktár, Műhely, Labor) szintezése egyre dráguló költséggel.
- **Boostok:** 2× termelés, azonnali raktár-feltöltés, „reklám” bónusz (szimulált).
- **4 világ:** mindegyik saját vizuállal, színvilággal és gyorsuló gazdasággal.
- **Hódítás (Térkép):** világonként 10 terület, mindegyik +5% termelés, teljes
  meghódításért gyémánt jutalom.
- **Napi jutalom + streak**, **bolt** (szimulált IAP/reklám), **mentés** és visszatöltés.

> A monetizáció (reklám, IAP, battle pass) a demóban **szimulált** – valós hirdetési/
> fizetési SDK nélkül, hogy az APK függőség- és engedélymentes maradjon.

---

## 🛠️ Technológia

- **Nyelv:** Kotlin · **UI:** Android View rendszer + ViewBinding + Material 3
- **Renderelés:** egyedi `GameView` (Canvas) az animált mikrovilághoz
- **Mentés:** `SharedPreferences` + JSON (`org.json`), nincs külső adatbázis
- **minSdk 26**, targetSdk/compileSdk 34, AGP 8.6.1, Gradle 8.9, JDK 17
- Internet-engedély nélkül fut, teljesen offline

## 🚀 Helyi build

```bash
./gradlew assembleDebug
# kimenet: app/build/outputs/apk/debug/app-debug.apk
```

Vagy nyisd meg a projektet **Android Studio**-ban és futtasd egy emulátoron/eszközön.

## 📁 Struktúra

```
app/src/main/java/com/micromasters/game/
  TitleActivity.kt        – nyitóképernyő
  WorldSelectActivity.kt  – világválasztó
  GameActivity.kt         – játékképernyő + idle ciklus
  GameView.kt             – animált mikrovilág (Canvas)
  Dialogs.kt              – fejlesztések, napi jutalom, bolt, térkép, beállítások
  GameState.kt            – állapot + szimuláció + perzisztencia
  Models.kt               – világok, egységek, épületek, boostok, napi jutalmak
  Storage.kt / Format.kt  – mentés / számformázás
app/src/main/res/         – layoutok, drawable-ek, ikon, témák (csak vektor, nincs PNG)
.github/workflows/build-apk.yml – APK build a hivatalos Android SDK-val
```
