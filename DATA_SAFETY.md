# Google Play – Data Safety (Adatbiztonsági űrlap válaszai)

> Kész válaszok a Play Console **Data safety** szakaszához a `com.micromasters.game`
> alkalmazáshoz. Az alkalmazás működése igazolja ezeket: nincs `INTERNET`
> engedély, nincs analytics/ad/crash-reporting SDK, minden adat helyben marad.

---

## 1. Gyűjt vagy megoszt az alkalmazás felhasználói adatot?

**Adatgyűjtés: NEM.**
**Adatmegosztás: NEM.**

Az alkalmazás semmilyen felhasználói adatot nem gyűjt és nem továbbít szerverre
vagy harmadik félnek. Nincs hálózati kommunikációja (nincs `INTERNET` engedély).

## 2. Adattípusok (mind: NEM gyűjtött)

| Kategória | Gyűjtve? | Megosztva? |
|---|---|---|
| Helyadat | ❌ | ❌ |
| Személyes adat (név, e-mail) | ❌ | ❌ |
| Pénzügyi adat | ❌ | ❌ |
| Egészségügyi/fitnesz | ❌ | ❌ |
| Üzenetek | ❌ | ❌ |
| Fényképek/videók | ❌ | ❌ |
| Hang | ❌ | ❌ |
| Fájlok/dokumentumok | ❌ | ❌ |
| Naptár/névjegyek | ❌ | ❌ |
| Alkalmazástevékenység | ❌ | ❌ |
| Böngészési előzmény | ❌ | ❌ |
| Eszköz- vagy más azonosító (pl. GAID) | ❌ | ❌ |
| Összeomlási naplók | ❌ (csak helyben, nem küldve) | ❌ |

## 3. Biztonsági gyakorlatok

- **Titkosítás továbbítás közben:** nem alkalmazható — **nincs hálózati forgalom**.
  A cleartext forgalom kifejezetten tiltva (`usesCleartextTraffic=false` +
  network-security-config).
- **Adattörlés kérése:** minden adat helyben tárolódik; az alkalmazás adatainak
  törlésével vagy eltávolításával véglegesen törlődik. Külön kérési mechanizmus
  nem szükséges, mert nincs szerveroldali adat.

## 4. A játékállapot tárolása (nem „gyűjtött adat" a Play definíciója szerint)

A játék kizárólag a **helyi eszközön** tárol játékmenet-állapotot (felfedezett
elemek, széria, győzelmek, szilánkok, expedíciók, beállítások) az Android
`SharedPreferences` és a beágyazott WebView `localStorage` tárolójában. Ez soha
nem hagyja el az eszközt, így a Play Data Safety szempontjából **nem minősül
adatgyűjtésnek**.

## 5. Értesítések (ha a re-engagement funkció aktív)

Ha a napi széria-emlékeztető engedélyezve van, az alkalmazás **helyi
értesítést** küld (kizárólag az eszközön ütemezve, hálózat nélkül). Ehhez a
`POST_NOTIFICATIONS` futásidejű engedély szükséges (Android 13+), amelyet a
felhasználó bármikor megtagadhat vagy visszavonhat. Ez **nem jár adatgyűjtéssel**.

---

*Ezek a válaszok a build tényleges képességeit tükrözik; a forráskód és a kért
engedélyek halmaza igazolja őket.*
