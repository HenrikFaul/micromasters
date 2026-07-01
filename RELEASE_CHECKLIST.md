# Play Store kiadási checklist — MicroMasters

Gyakorlati lépéssor a `com.micromasters.game` publikálásához. Az anyagok készen
állnak: `STORE_LISTING.md` (listázás), `DATA_SAFETY.md` (adatbiztonság),
`PRIVACY.md` (adatvédelem).

## 0. Build artefaktumok (a CI állítja elő)
- [x] Aláírt, **nem debuggolható** release **APK** (oldalról telepítéshez/teszthez)
- [x] Aláírt release **AAB** (`MicroMasters.aab`) — ezt kell a Play Console-ba tölteni
- A CI a `latest-build` release-be publikálja mindkettőt push után.

> ⚠️ **Aláírókulcs:** a CI jelenleg **efemer** kulcsot generál buildenként (teszthez).
> Éles kiadáshoz **stabil** kulcs kell. Két lehetőség:
> 1. **Play App Signing** (ajánlott): a Play kezeli az app-aláírást; te egy
>    feltöltési (upload) kulcsot használsz. Egyszer generálj egyet, tedd GitHub
>    Secret-be (`KEYSTORE_BASE64`, `KEYSTORE_PASS`, `KEY_ALIAS`, `KEY_PASS`), és a
>    CI keystore-generáló lépését cseréld erre.
> 2. Saját stabil keystore GitHub Secret-ből.

## 1. Google Play Console — alkalmazás létrehozása
- [ ] Fejlesztői fiók (egyszeri 25 USD reg.díj), ha még nincs
- [ ] „Create app" → Név: **MicroMasters** · Nyelv: Magyar · Típus: **Game** · Ingyenes
- [ ] Alap-nyilatkozatok elfogadása

## 2. Store listing (a `STORE_LISTING.md`-ből)
- [ ] Rövid leírás (≤80) és Teljes leírás (≤4000) beillesztése
- [ ] **Grafikai elemek:**
  - [ ] App ikon 512×512 (a meglévő adaptív ikonból exportálva)
  - [ ] Feature graphic 1024×500
  - [ ] Legalább 2–8 **telefon-képernyőkép** (a játékból: felfedező-tábla, Kódex,
        Master-párbaj, Világok, Hősök)
- [ ] Kategória: **Casual** · Címkék a listázásból

## 3. Adatbiztonság (a `DATA_SAFETY.md`-ből)
- [ ] „Data safety" szakasz: **nem gyűjt és nem oszt meg adatot**
- [ ] Adattörlés: helyi, uninstall/adattörléssel
- [ ] Adatvédelmi szabályzat URL (a `PRIVACY.md` publikálva, pl. GitHub Pages)

## 4. Tartalmi besorolás
- [ ] IARC kérdőív kitöltése (várható: **Mindenki / PEGI 3** — absztrakt
      sebzésszám-alapú „csaták", nincs vér, nincs vásárlás, nincs chat)

## 5. Célközönség és tartalom
- [ ] Célközönség kora (nem kizárólag gyerekeknek pozicionálva ajánlott)
- [ ] Reklám: **Nincs** (nem tartalmaz hirdetést) — jelöld be

## 6. App content / megfelelőség
- [ ] Reklámazonosító (GAID): **nem használ** → jelöld be
- [ ] Kormányzati app: nem · Pénzügyi: nem
- [ ] Célzott hirdetés: nem

## 7. Kiadás
- [ ] **Internal testing** track: töltsd fel a `MicroMasters.aab`-t, adj hozzá
      tesztelőket, ellenőrizd a telepítést és a játékmenetet valós eszközön
- [ ] Ha rendben: **Production** → fokozatos kiadás (pl. 20%)
- [ ] Release notes a `STORE_LISTING.md` „Mi az új?" szövegéből

## 8. Kiadás utáni ellenőrzés
- [ ] Play Console → **Pre-launch report** (Robo teszt) — crash/ANR/akadálymentesség
- [ ] **Android vitals** figyelése (ANR/crash ráta) az első napokban

---

### Verzió most
`versionCode 14 · versionName 2.4` (a `app/build.gradle.kts`-ben; új kiadáshoz
mindig növeld a `versionCode`-ot).
