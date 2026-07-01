# MicroMasters — Játékleírás (v2.0)

> Nagyon részletes működési leírás: minden képernyő, minden mechanika, a teljes
> progresszió és a konkrét számértékek. Ez a dokumentum a **ténylegesen megépített**
> játékot írja le (nem ígéretet) — a végén külön szakasz szól arról, mi van még
> hátra a teljes vízióból.

---

## 0. Egymondatos összefoglaló

A MicroMasters egy **merge-felfedezős** játék: két elemet egyesítve **új találmányokat fedezel
fel**, kitöltöd az emberi tudás **Kódexét** (276 elem), felépíted a **technológiai fát** a kőtől a
III. típusú civilizációig, közben **Hősöket** toborzol, **Expedíciókra** küldöd őket a **12 Világba**
(AFK jutalom), és **valódi körökre osztott párbajokban** legyőzöd a korszakok **Mastereit**.

---

## 1. A világ és a végcél

**2158.** Az emberiség eltűnt — nem háború, nem vírus, csak *eltűntek*. Egyetlen dolog maradt: a
**Master Core (Mestermag)**, az emberi tudás őrzője. De a Mag összetört, és emlékezete ezernyi
szilánkra hullott szét.

- **A te szereped:** a Mag élő akarata, aki **újraépíti a teljes emberi civilizációt** — a kőtől a
  csillagokig.
- **A végső cél:** fedezd fel mind a **276 találmányt**, győzd le mind a **10 Mastert**, és érd el a
  **III. Típusú Civilizációt** (`🌌`) — ekkor a Mag teljesen összeáll, és az emberiség újjászületik a
  galaxisban.

A haladásod **vizuálisan is látszik**: a 3D Mestermag kezdetben szétrobbant szilánkfelhő; ahogy nő a
felfedezett elemek aránya, a szilánkok **összehúzódnak és fényesebben izzanak**.

---

## 2. A központi játékhurok

```
  FELFEDEZÉS                 FELKÉSZÜLÉS              KIHÍVÁS                 JUTALOM
  ──────────                 ───────────              ───────                 ───────
  Egyesíts 2 elemet   →   Töltsd a Kódexet    →   Vidd a tech-fát      →   Új korszak nyílik
  új találmányért         és a tech-ágakat        a Master req-jéig         (+ erősebb Mag)
        │                       │                        │                         │
        │                       ▼                        ▼                         │
        │                 Oldj fel HŐSÖKET   →   Állíts hőst CSATÁBA   →   +10 állandó Mag-ÉP
        │                  (mérföldkövek)         (aktív képesség)          + trófea
        ▼                       │                                                  │
  EXPEDÍCIÓK  ◄─────────────────┘                                                  │
  Küldj hőst a Világokba → idővel 💠 szilánk + automatikusan felfedezett elem  ────┘
```

Minden rendszer a felfedezést táplálja, és a felfedezés minden rendszert kinyit.

---

## 3. Képernyők egyenként

### 3.1 Címképernyő (natív)

Az alkalmazás indításakor ez fogad.

| Elem | Mit csinál |
|---|---|
| **JÁTSSZ** gomb | Egyenesen a Felfedező-táblába visz (a játék lényege azonnal elérhető). |
| **JÁTSSZ** *hosszú nyomás* | A régi (örökölt) világ-hub menü — power-user gyorsgomb, normál játékhoz nem kell. |
| **Bejelentkezés** | Kozmetikai (visszajelző üzenet) — felhődmentés még nincs. |
| **⚙️ Beállítások** | Hang ki/be és egyéb kapcsolók. |
| Bolygó animáció | A háttér-bolygó lassan „lélegzik" (fel-le mozog). |

### 3.2 Bevezető történet (csak az első indításkor)

Teljes képernyős intro: **2158**, az eltűnt emberiség, a Mestermag, a feladat. Egy **KEZDÉS**
gombbal zárod. Ezután **soha többé nem jelenik meg** — visszatérő játékos egyből a táblán van.
(Elmentve: `introSeen`.)

### 3.3 Fő játéktér — a Felfedező-tábla ⭐ (ez a játék szíve)

Ez egy WebGL (Three.js) 3D jelenet, fölötte a kezelőfelülettel.

**Felső sáv (vízszintesen görgethető):**

| Ikon | Jelentés / kattintásra |
|---|---|
| `◀` | Vissza a címképernyőre. |
| `📜 X/276` | **Kódex** — eddig felfedezett / összes elem. Kattintásra megnyílik a Kódex-rács. |
| `⚔️` | **Masterek** listája (korszak-bossok). |
| `🗺️` | **Világok & Expedíciók** térkép. Zöld szám = ennyi expedíció gyűjthető be. |
| `🦸` | **Hősök** képernyő. |
| `🔥 N` | **Napi széria** (hány egymást követő nap játszottál). Kattintásra: rekord + mai felfedezések. |
| `💠 N` | **Szilánkok** (expedíciós valuta). |
| `🧩 N%` | A **Mestermag** összeállásának aránya (= felfedezett/összes). |

**A 3D jelenet:**
- Középen a **Mestermag** (10 szilánkból álló, lebegő, izzó kristály) — összeáll, ahogy haladsz.
- Két **posztamens** (bal/jobb) — ide kerül a kiválasztott két elem.
- Csillagmező háttér, és minden egyesítéskor **részecske-robbanás**.

**Az alsó „polc" (`#inv`) — a leltárad:**
- A felfedezett elemek **csempéi** (emoji + név), vízszintesen görgethetők, **technológiai sorrendben**.
- Kezdéskor csak a **4 őselem** van itt: `🔥 Tűz`, `💧 Víz`, `🌍 Föld`, `💨 Levegő`.

**Hogyan egyesítesz (a fő művelet):**
1. **Koppints egy csempére** → felkerül a **bal** posztamensre (a kombó-kijelző mutatja: „🔥 + ?").
2. **Koppints egy másikra** → a **jobb** posztamensre kerül, és ~0,26 mp múlva **automatikusan egyesül**.
3. Háromféle eredmény:
   - ✅ **Új találmány** → felvillan a **Felfedezés-banner** (emoji, név, korszak, idézet/lore), a Mag
     felvillan, a részecskék szétrepülnek, és **elmentődik**.
   - 🔁 **Már megvan** → „(már megvan)" — nem hiba, csak nincs új.
   - ✗ **Nincs kombináció** → „✗ nincs kombináció" (ez a pár nem ad semmit).

**💡 Tipp gomb (jobb alul):**
- Megkeresi a **legkorábbi még fel nem fedezett receptet, amelynek mindkét hozzávalója már megvan**,
  ráteszi a két elemet a posztamensekre, **felvillantja** a hozzájuk tartozó csempéket a polcon, és
  kiír egy „Próbáld ki: A + B" buborékot.
- Ez a merge-játékok **#1 lemorzsolódási okát** (elakadás) oldja meg. Ha már mindent felfedeztél, ami
  most elérhető, ezt is megírja.

**🔥 Napi széria:**
- Helyi éjfél szerint számolja a napokat. Egymást követő napokon **nő** a széria; ha kihagysz egy
  napot, **újraindul**. Visszatéréskor „Üdv újra! 🔥 N napos szériában vagy" buborék.

### 3.4 📜 Kódex képernyő

Az **összes 276 elem** rácsa.
- **Felfedezett** elem: emoji + név, élénk.
- **Zárt** elem: `❓` / „???" (homályos) — láthatod, hány találmány vár még rád, de nem tudod, mik.
- A tetején: „X / 276 felfedezve".
- Ez a **Pokédex-élmény**: a gyűjtés önmagában cél.

### 3.5 ⚔️ Masterek lista

A **10 korszak-boss** kártyái, korszak szerint sorrendben. Minden kártyán:
- Emoji, név, **korszak**, a Master **egyedi mechanikájának leírása** (lore).
- A **kért technológiák** emojijai (kivilágítva, ha már felfedezted).
- **Állapot** + gomb:
  - 🔒 **Zárva** — még hiányzik a kért tech (nem hívható ki).
  - **⚔️ CSATA** gomb — ha minden kért techet felfedeztél, **kihívható**.
  - ✅ **LEGYŐZVE** + `👑` és egy **↻** gomb — már legyőzted, de **újrajátszható**.
- Alul: „⚔️ X legyőzve · Y kihívható · 10 korszak".

**Fontos kapuzás:** egy Mastert csak akkor hívhatsz ki, ha **az összes kért technológiáját
felfedezted**. Ezért a felfedezés ad értelmet — a tudás a fegyvered.

### 3.6 Boss-párbaj (a Csata-képernyő) ⚔️

Ez egy **körökre osztott kártyacsata**, ahol a **felfedezett találmányaid a fegyvereid**.

**Elrendezés:**
- **Fent:** a boss emojija, neve, korszaka + a **mechanika-címke**, és a **Boss-ÉP sávja** (100% → 0%).
- A mechanika **lore-szövege** `【…】` keretben.
- **Középen:** a **harci napló** (mit tettél, mit válaszolt a boss) — mögötte a **3D Mag bosszá
  alakul** (vörösen izzik, feldagad, pulzál, találatkor felvillan és megrázza a kamerát).
- **Lent:** a **🧩 Mag-integritás** sávod (a te „életed"), opcionálisan a **hős-képesség gomb**, majd a
  **4 lapos kéz**.

**A kör menete:**
1. Minden körben **4 lapot húzol** a felfedezett arzenálodból.
   - A boss **kért technológiái = gyengepontok**: ezek a lapok **arany ⚔️ keretet** kapnak és
     **kritikus sebzést** adnak.
   - A többi lap kisebbet sebez.
   - **Ugyanazt a gyengepontot nem játszhatod ki kétszer egymás után** — forgatnod kell az arzenálodat.
2. **Koppints egy lapra** → sebzés a bossnak.
3. A boss **visszaüt** a Mag-integritásodra. Minden **3. kör** **feltöltött csapás** (×1,5, a napló
   előre jelzi). Egyes Masterek **minden 2. körben** dühöngenek (lásd mechanikák).
4. **Győzelem:** a Boss-ÉP 0 → 🏆 **MASTER LEGYŐZVE**. **+10 állandó Mag-integritás**, trófea
   elmentve, a győzelmek száma nő, a Mag „erősebb lett". A 3D boss **szétrobban**.
5. **Vereség:** a Mag-integritásod 0 → 💥 **A MAG MEGINGOTT**. Újrapróbálhatod (a felfedezéseid
   közben erősítik a fegyvertáradat).

**Master-mechanikák (minden boss máshogy harcol):**

| Mechanika | Hatás | Mely Masterek |
|---|---|---|
| 🛡️ **Páncél** | A **nem-kritikus** sebzésből −2 (a gyengepontokat nem érinti → használj kritikust!). | Kovakirály, Pályaőr |
| ♻️ **Regeneráció** | A boss **+2 ÉP-t gyógyul** minden körben → kitartásra kényszerít. | Tudásfaló, Kórdémon, Csillagéhes, Magtörő |
| ⚡ **Gyakori düh** | Feltöltött csapás **minden 2. körben** (a szokásos 3. helyett). | Gőzkolosszus, Üregelme |
| 🌀 **Sebzéscsökkentés** | Időnként a **következő lapod sebzése feleződik**. | Glitchúr |

**Hős-képesség a csatában (ha van csatába állított hősöd):**
- Megjelenik egy **arany képesség-gomb**, ami **csatánként egyszer** vethető be:

| Típus | Hatás |
|---|---|
| 💚 **Gyógyítás** | +30 / +40 Mag-integritás. |
| 🛡️ **Pajzs** | A következő boss-csapást **teljesen elnyeli**. |
| ⚡ **Túltöltés** | A **következő lapod dupla sebzés**. |
| 💥 **Szikracsapás / Csillagtűz** | Azonnali **28 / 34 sebzés** a bossnak. |
| 🔄 **Felderítés / Hálózat** | **Új kéz**, és a következő lap **nem vált ki ellentámadást** (ingyen ütés). |

**Egyensúly (Monte-Carlo szimulációval hangolva):** felkészülten **minden szinten nyerhetsz** (a
maradék Mag ~53%-ról a tier-0-n ~24%-ra csökken a végső bossnál — egyre feszültebb), de ha
**felkészületlenül ugrasz előre**, a kései regeneráló/dühöngő bossok **legyőznek**. Tehát számít a
sorrend és a felfedezettség.

### 3.7 🗺️ Világok & Expedíciók

A **12 Világ** mint **expedíciós térkép** — itt zajlik az **AFK (idle) haladás**.

**Egy világkártya állapotai:**
- 🔒 **Zárva** — a feloldási feltétel (felfedezésszám vagy legyőzött Masterek) még nem teljesült;
  látod a mechanika-leírást és a feltételt.
- 🟢 **Szabad (feloldva, nincs futó expedíció)** — „Válassz hőst" + a **szabad hősök zsetonjai**;
  koppints egyre → **elindul az expedíció**.
- ⏳ **Fut** — a hozzárendelt hős, egy **folyamatjelző sáv**, **visszaszámláló**, és egy
  **⏩ Siettetés (💠N)** gomb (szilánkért azonnal befejezed).
- ✅ **Kész** — **🎁 Begyűjt** gomb.

**Begyűjtéskor mit kapsz:**
- **💠 Szilánkok** (a világ alapértéke × a hős szorzója × a perk).
- **Gyakran egy automatikusan felfedezett találmány** a Kódexbe — mindig egy **most elérhető**
  (mindkét hozzávalója megvan) elem. Ez AFK-haladás és **segítség az elakadt játékosnak**.

**Világ-perkek (mindegyik világ mechanikailag más):**

| Perk | Hatás |
|---|---|
| `plain` | Alap zsákmány. |
| `safe` (garantált lelet) | Mindig hoz elemet, ha van elérhető. |
| `double_frag` (dupla 💠) | Kétszeres szilánk. |
| `risky` (kockázatos) | 25% eséllyel fél zsákmány és nincs elem; egyébként +60% szilánk. |
| `rare` (ritka lelet) | A **legmélyebb** elérhető elemet adja, nagyobb eséllyel. |
| `fast` (gyors) | Rövid futás, szerényebb hozam. |
| `jackpot` (💠💠) | Háromszoros szilánk. |
| `double_elem` (2× lelet) | Akár **két** elemet is hozhat. |

- A **🗺️ gombon zöld szám** jelzi a begyűjthető expedíciókat; **buborék-értesítést** kapsz, ha egy
  expedíció visszatér, **akkor is, ha zárva van a térkép**.
- Alul: „💠 N szilánk · X aktív expedíció · Y szabad hős".

**Több expedíció egyszerre is futhat** — világonként egy, de mindegyikhez **külön szabad hős** kell.
Ez ösztönöz több hős feloldására.

### 3.8 🦸 Hősök

A **8 hős** kártyái. Minden kártyán:
- Emoji, név, a **csata-képessége** (név + leírás), az **expedíciós tempója** (×szorzó), és a hős
  egyedi **lore-képessége**.
- **Állapot:** 🔒 *zárt* (feloldási feltétellel), 🟢 *Szabad*, vagy 🔵 *Expedíción: (világ)*.
- **⚔️ Csatába** kapcsoló — egy hőst a Master-párbajokhoz rendelsz (a kártya kiemelve mutatja).
- Alul: „🦸 X / 8 hős feloldva".

Új hős feloldásakor **buborék-értesítést** kapsz.

---

## 4. A teljes progresszió — hogyan megy előre a játék

### 4.1 A technológiai fa (korszakok)

A felfedezések korszakokba rendeződnek, nagyjából ebben a sorrendben:

```
Őselemek → Ősidők → Kőkor → Bronzkor → Tudás kora → Ipari kor →
Információs kor → Modern kor → Űrkor → Csillagközi kor → Végcél (III. típusú civ.)
```

Néhány mérföldkő-recept (a több száz közül):
- `🔥 + 💧 = ♨️ Gőz`, `🌍 + 🌍 = 🪨 Kő`, `🪨 + 🌳 = 🔨 Kalapács`
- `🔨 + 🔥 = ⚒️ Kohó`, `⚒️ + ⛏️ = 🥉 Bronz`, `🥉 + 🌳 = ⚙️ Kerék`
- `✨ + ⛏️ = ⚡ Elektromosság`, `🔌 + 🔌 = 💻 Számítógép`, `💻 + 🌐 = 🤖 MI`
- `⚛️ + ⚛️ = 🔆 Fúzió`, `🔆 + 🛰️ = 🌞 Dyson-gömb`, `🌞 + 🤖 = 🌌 III. típusú civ.`

### 4.2 A két „valuta", ami kaput nyit

Szinte minden feloldás e kettőből számít:
- **Felfedezett elemek száma** (`disc`) — a Kódex mérete.
- **Legyőzött Masterek száma** (`wins`).

### 4.3 Master-feloldások (kapu = a kért tech felfedezése)

| # | Master | Korszak | Kért technológiák | Mechanika |
|---|---|---|---|---|
| 0 | 🗿 Kovakirály | Kőkor | 🔨 Kalapács, 🪨 Kő, 🔥 Tűz | 🛡️ Páncél |
| 1 | ⚒️ Olvasztár | Bronzkor | 🥉 Bronz, ⚒️ Kohó, ⚙️ Kerék, 🧱 Tégla | — |
| 2 | 📖 Tudásfaló | Tudás kora | 📚 Könyv, 📜 Írás, 🖨️ Nyomtatás, 🔬 Tudomány | ♻️ Regen |
| 3 | 🏭 Gőzkolosszus | Ipari kor | 🚂 Gőzgép, 🏭 Gyár, vas, szén | ⚡ Gyakori düh |
| 4 | 👾 Glitchúr | Információs kor | 💻 Számítógép, 🌐 Internet, 🔌 Áramkör, 🤖 MI | 🌀 Sebzéscsökkentés |
| 5 | 🦠 Kórdémon | Modern kor | 💊 Orvostudomány, ⚛️ Atom, 🦾 Robot, 🔬 Tudomány | ♻️ Regen |
| 6 | 🛰️ Pályaőr | Űrkor | 🚀 Rakéta, 🛰️ Műhold, 🔆 Fúzió, titán | 🛡️ Páncél |
| 7 | 🌞 Csillagéhes | Csillagközi kor | 🌞 Dyson, 🔆 Fúzió, 🛰️ Műhold, napelem | ♻️ Regen |
| 8 | 🕳️ Üregelme | Csillagközi kor | 🌌 III. típus, 🤖 MI, 🌐 Internet, hálózat | ⚡ Gyakori düh |
| 9 | 🌌 Magtörő | Végcél | 🌌 III. típus, 🌞 Dyson, 🔆 Fúzió, 🤖 MI | ♻️ Regen |

### 4.4 Hős-feloldások

| Hős | Feltétel | Csata-képesség | Exp. tempó |
|---|---|---|---|
| 🜂 Emberke | **ingyen** | Szikracsapás (💥 28) | ×1.05 |
| 📜 Öreg Krónikás | 18 elem | Kódex-emlék (💚 30) | ×1.0 |
| ⚒️ Vasanya | 35 elem | Vaspajzs (🛡️) | ×1.1 |
| ⚡ Áramvölgyi | 1 Master | Túltöltés (⚡ dupla) | ×1.25 |
| 🧭 Szellőcsillag | 70 elem | Felderítés (🔄) | **×1.67** (expedíció-specialista) |
| 🌐 Adatlány | 2 Master | Hálózat (🔄) | ×1.18 |
| 🔬 Fénymag | 130 elem | Gyógyítás (💚 40) | ×1.18 |
| 🌌 Csillagkovács | 5 Master | Csillagtűz (💥 34) | ×1.11 |

### 4.5 Világ-feloldások

| Világ | Feltétel | Idő | Alap 💠 | Perk |
|---|---|---|---|---|
| 🪨 Kővilág | ingyen | 30s | 5 | plain |
| ❄️ Jégvilág | 15 elem | 50s | 7 | safe |
| 🌴 Dzsungel | 28 elem | 60s | 9 | double_frag |
| 🌊 Óceán | 45 elem | 75s | 11 | plain |
| 🌋 Vulkán | 1 Master | 70s | 15 | risky |
| 🏰 Égi Királyság | 75 elem | 90s | 13 | rare |
| 🌙 Hold | 2 Master | 45s | 9 | fast |
| 🔴 Mars | 105 elem | 110s | 17 | plain |
| 🧊 Európa | 3 Master | 130s | 22 | rare |
| ☀️ Dyson-gyűrű | 150 elem | 100s | 26 | double_frag |
| 🕳️ Fekete Lyuk | 5 Master | 160s | 36 | jackpot |
| 🌀 Multiverzum | 7 Master | 150s | 30 | double_elem |

---

## 5. A párbaj pontos számai

| Paraméter | Érték |
|---|---|
| Boss-ÉP | 100 (százalékos) |
| Gyengepont (kritikus) lap sebzése | 10 |
| Sima lap sebzése | 4 |
| Boss ellentámadás | 4,5 + 0,65 × tier (±15% szórás) |
| Feltöltött csapás (düh) | ×1,5 (minden 3. kör; „enrage2"-nél minden 2.) |
| A te Mag-integritásod | 100 + 10 × (legyőzött Masterek) |
| Páncél | nem-kritikus sebzés −2 |
| Regeneráció | +2 boss-ÉP/kör |
| Expedíció-siettetés ára | `⌈hátralévő mp / 15⌉` 💠 |

Egy átlagos párbaj ~9–12 kör. A „kéz" mindig kínál **legalább egy gyengepontot**, így mindig van
értelmes lépésed, de a kockázat valódi: figyelned kell a Mag-integritásodra, és okosan időzítened a
hős-képességet.

---

## 6. Mit kell teljesíteni? (célok)

**Rövid táv (első ülés):**
1. Fedezd fel a 4 őselemből a `♨️ Gőz`, `🪨 Kő`, `🌳 Fa`, `🔨 Kalapács` láncot (a 💡 Tipp segít).
2. Old fel **Öreg Krónikást** (18 elem) és **Vasanyát** (35 elem).
3. Indíts **expedíciót** a Kővilágba; gyűjts 💠-t és egy ingyen felfedezést.
4. Fedezd fel a Kovakirály kért techét (🔨/🪨/🔥), és **nyerd meg az első csatát** → +10 Mag-ÉP, és
   feloldódik 🌋 Vulkán + ⚡ Áramvölgyi.

**Közép táv:**
- Vidd a tech-fát a Bronzkoron át a Tudás koráig; győzd le a 2–4. Mastert.
- Állíts **csatába hőst** (pl. 🔬 Fénymag gyógyítás a regeneráló bossok ellen).
- Tarts több expedíciót párhuzamosan (több szabad hős = több AFK-hozam).
- Old fel a közepes világokat (Óceán, Égi Királyság, Mars).

**Hosszú táv (végjáték):**
- Érd el az Űr- és Csillagközi kort; győzd le a 6–9. Mastert (regen/düh — kell a felkészültség).
- Old fel 🕳️ Fekete Lyukat és 🌀 Multiverzumot (5, illetve 7 Master-győzelem).
- **Végcél:** fedezd fel a `🌌 III. Típusú Civilizációt`, töltsd ki a teljes Kódexet (276/276), és
  győzd le a **Magtörőt** — ekkor a Mestermag teljesen összeáll.

---

## 7. Mentés, napi ritmus, offline

- **Minden elmentődik** a készüléken (localStorage): felfedezett elemek, napi széria, legyőzött
  Masterek, szilánkok, futó expedíciók, csatába állított hős.
- **Napi széria:** helyi éjfélkor vált; egymást követő napok növelik.
- **Offline expedíciók:** ha bezárod a játékot egy futó expedícióval, az **a valós idő múlásával
  készül el** — visszatérve értesítést kapsz és begyűjtheted.
- Két mentési kulcs: `mm_codex_v1` (felfedezések) és `mm_meta_v1` (széria, győzelmek, szilánkok,
  expedíciók, hős).

---

## 8. Jelenlegi korlátok és a következő lépések (őszintén)

A **vízió** néhány eleme még **nem épült meg** — ezek a tervezett következő lépések:

- **Hős-szintlépés:** a szilánkoknak (💠) jelenleg egy fő felhasználása van (expedíció-siettetés);
  tervben: szilánkból **hősök erősítése** (erősebb képesség / gyorsabb expedíció).
- **Több hős egy csatában** (jelenleg 1 csatába állított hős).
- **Világok saját tábla-mechanikája:** a 12 világ leírt egyedi szabálya (fagyás, áradás, kitörés…)
  jelenleg **expedíciós perként** jelenik meg, nem külön játszható táblaként.
- **Guild / globális események / ranglista** (a régi idle-rétegből) — a felfedezős magba még nincs
  beépítve.
- **Felhődmentés / fiók** (a Bejelentkezés ma kozmetikai).

A jelenlegi v2.0 ezektől függetlenül **teljes, játszható hurok**: felfedezés → Kódex → tech-fa →
Masterek (valódi csaták) → Hősök → Expedíciók → erősödés → következő korszak.

---

*MicroMasters v2.0 — natív Android (Kotlin) + WebGL (Three.js) felfedezős mag, teljesen offline.*
