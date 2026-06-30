/* MicroMasters — Codex of Humanity.
   A merge-DISCOVERY game: combine two elements to invent a new technology, climb the
   tech tree from Stone to a Type III civilization, and rebuild the shattered Master Core.
   Three.js r148, fully offline in a WebView. */
(function () {
  'use strict';
  const boot = document.getElementById('boot');
  const fail = (m) => { if (boot) { boot.textContent = m; boot.style.display = 'flex'; } };
  if (typeof THREE === 'undefined') { fail('A 3D motor nem töltött be.'); return; }

  // ---------- content: elements + tech-tree recipes ----------
  const E = {
    fire:{n:'Tűz',e:'🔥',era:'Őselemek',lore:'Minden átalakulás kezdete.'},
    water:{n:'Víz',e:'💧',era:'Őselemek',lore:'Az élet forrása.'},
    earth:{n:'Föld',e:'🌍',era:'Őselemek',lore:'A szilárd alap.'},
    air:{n:'Levegő',e:'💨',era:'Őselemek',lore:'A láthatatlan közeg.'},
    steam:{n:'Gőz',e:'♨️',era:'Ősidők',lore:'Tűz és víz tánca.'},
    lava:{n:'Láva',e:'🌋',era:'Ősidők',lore:'A Föld olvadt szíve.'},
    stone:{n:'Kő',e:'🪨',era:'Kőkor',lore:'Az első szerszámok anyaga.'},
    rain:{n:'Eső',e:'🌧️',era:'Ősidők',lore:'Az ég megöntözi a földet.'},
    spark:{n:'Szikra',e:'✨',era:'Ősidők',lore:'Minden energia magja.'},
    mud:{n:'Sár',e:'🟤',era:'Ősidők',lore:'Föld és víz találkozása.'},
    plant:{n:'Növény',e:'🌱',era:'Ősidők',lore:'Az élet első hajtása.'},
    wood:{n:'Fa',e:'🌳',era:'Kőkor',lore:'Az erdő ajándéka.'},
    hammer:{n:'Kalapács',e:'🔨',era:'Kőkor',lore:'Az emberiség első találmánya.'},
    forge:{n:'Kohó',e:'⚒️',era:'Bronzkor',lore:'Itt születik a fém.'},
    metal:{n:'Fém',e:'⛏️',era:'Bronzkor',lore:'A föld mélyének kincse.'},
    bronze:{n:'Bronz',e:'🥉',era:'Bronzkor',lore:'Egy egész korszak névadója.'},
    wheel:{n:'Kerék',e:'⚙️',era:'Bronzkor',lore:'Mindent mozgásba hozott.'},
    clay:{n:'Agyag',e:'🧱',era:'Kőkor',lore:'Az első edények és falak.'},
    paper:{n:'Papír',e:'📄',era:'Tudás kora',lore:'A tudás hordozója.'},
    writing:{n:'Írás',e:'📜',era:'Tudás kora',lore:'A civilizáció emlékezete.'},
    book:{n:'Könyv',e:'📚',era:'Tudás kora',lore:'Összegyűjtött bölcsesség.'},
    printing:{n:'Nyomtatás',e:'🖨️',era:'Tudás kora',lore:'A tudás mindenkié lett.'},
    electricity:{n:'Elektromosság',e:'⚡',era:'Ipari kor',lore:'Megszelídített energia.'},
    steamengine:{n:'Gőzgép',e:'🚂',era:'Ipari kor',lore:'Az ipari forradalom szíve.'},
    factory:{n:'Gyár',e:'🏭',era:'Ipari kor',lore:'A tömeggyártás hajnala.'},
    bulb:{n:'Villanykörte',e:'💡',era:'Ipari kor',lore:'Fény az éjszakában.'},
    circuit:{n:'Áramkör',e:'🔌',era:'Információs kor',lore:'Az elektronika alapja.'},
    radio:{n:'Rádió',e:'📻',era:'Információs kor',lore:'Hangok a levegőben.'},
    computer:{n:'Számítógép',e:'💻',era:'Információs kor',lore:'A gondolkodó gép.'},
    internet:{n:'Internet',e:'🌐',era:'Információs kor',lore:'A világ összekapcsolódott.'},
    phone:{n:'Okostelefon',e:'📱',era:'Információs kor',lore:'A világ a zsebedben.'},
    ai:{n:'Mesterséges Intelligencia',e:'🤖',era:'Információs kor',lore:'A gép megtanult tanulni.'},
    science:{n:'Tudomány',e:'🔬',era:'Tudás kora',lore:'A megértés módszere.'},
    medicine:{n:'Orvostudomány',e:'💊',era:'Modern kor',lore:'Legyőztük a kórokat.'},
    atom:{n:'Atomenergia',e:'⚛️',era:'Modern kor',lore:'Az anyag rejtett ereje.'},
    fusion:{n:'Fúzió',e:'🔆',era:'Űrkor',lore:'A csillagok ereje a Földön.'},
    rocket:{n:'Rakéta',e:'🚀',era:'Űrkor',lore:'Elhagytuk a Földet.'},
    satellite:{n:'Műhold',e:'🛰️',era:'Űrkor',lore:'Szemek az ég felett.'},
    robot:{n:'Robot',e:'🦾',era:'Modern kor',lore:'Fáradhatatlan munkáskéz.'},
    dyson:{n:'Dyson-gömb',e:'🌞',era:'Csillagközi kor',lore:'Egy egész csillag energiája.'},
    type3:{n:'III. Típusú Civilizáció',e:'🌌',era:'Végcél',lore:'Az emberiség újjászületett a galaxisban.'},
  };
  const BASES = ['fire','water','earth','air'];
  const R = {};
  [
    ['steam','fire','water'],['lava','fire','earth'],['stone','earth','earth'],['rain','air','water'],
    ['spark','fire','air'],['mud','earth','water'],['plant','rain','earth'],['wood','plant','earth'],
    ['hammer','stone','wood'],['forge','hammer','fire'],['metal','stone','fire'],['bronze','forge','metal'],
    ['wheel','bronze','wood'],['clay','mud','fire'],['paper','wood','water'],['writing','stone','paper'],
    ['book','writing','paper'],['printing','book','metal'],['electricity','spark','metal'],
    ['steamengine','steam','metal'],['factory','steamengine','wheel'],['bulb','electricity','spark'],
    ['circuit','electricity','metal'],['radio','circuit','air'],['computer','circuit','circuit'],
    ['internet','computer','radio'],['phone','internet','circuit'],['ai','computer','internet'],
    ['science','book','bulb'],['medicine','science','plant'],['atom','science','spark'],['fusion','atom','atom'],
    ['rocket','factory','fire'],['satellite','rocket','circuit'],['robot','ai','metal'],
    ['dyson','fusion','satellite'],['type3','dyson','ai'],
  ].forEach(([r,a,b]) => { R[[a,b].sort().join('+')] = r; });

  // merge the expanded Codex (generated data.js): hundreds more inventions + Masters
  let MASTERS = [];
  if (window.MM_DATA) {
    Object.assign(E, MM_DATA.elements || {});
    (MM_DATA.recipes || []).forEach(([res, a, b]) => { if (res && a && b && E[res]) R[[a, b].sort().join('+')] = res; });
    MASTERS = MM_DATA.masters || [];
  }
  const TOTAL = Object.keys(E).length;

  // ---------- state ----------
  const KEY = 'mm_codex_v1';
  let disc = new Set(BASES);
  try { const j = JSON.parse(localStorage.getItem(KEY) || 'null'); if (Array.isArray(j)) disc = new Set(j.filter(id => E[id])); } catch (e) {}
  BASES.forEach(b => disc.add(b));
  const save = () => { try { localStorage.setItem(KEY, JSON.stringify([...disc])); } catch (e) {} };
  let sel = [];           // selected element ids (max 2)

  // ---------- meta: daily streak, intro-seen, lifetime best (retention) ----------
  const MKEY = 'mm_meta_v1';
  let meta = { introSeen: false, streak: 0, bestStreak: 0, lastDayN: -1, foundToday: 0, defeated: [], wins: 0,
    fragments: 0, expeds: {}, heroSeen: [], equipHero: null };
  try { const m = JSON.parse(localStorage.getItem(MKEY) || 'null'); if (m && typeof m === 'object') meta = Object.assign(meta, m); } catch (e) {}
  if (!Array.isArray(meta.defeated)) meta.defeated = [];
  if (!meta.expeds || typeof meta.expeds !== 'object') meta.expeds = {};
  if (typeof meta.fragments !== 'number') meta.fragments = 0;
  if (!Array.isArray(meta.heroSeen)) meta.heroSeen = [];
  const isDefeated = (id) => meta.defeated.indexOf(id) >= 0;
  const saveMeta = () => { try { localStorage.setItem(MKEY, JSON.stringify(meta)); } catch (e) {} };
  // local-day index (offset so the day rolls over at the player's local midnight)
  function localDayN() { const d = new Date(); return Math.floor((d.getTime() - d.getTimezoneOffset() * 60000) / 86400000); }
  let streakAdvanced = false;
  (function rollStreak() {
    const today = localDayN();
    if (meta.lastDayN === today) { /* already counted today */ }
    else if (meta.lastDayN === today - 1) { meta.streak = (meta.streak || 0) + 1; meta.foundToday = 0; streakAdvanced = true; }
    else { meta.streak = 1; meta.foundToday = 0; streakAdvanced = (meta.lastDayN >= 0); }
    meta.lastDayN = today;
    if (meta.streak > (meta.bestStreak || 0)) meta.bestStreak = meta.streak;
    saveMeta();
  })();

  // ---------- three.js ----------
  let renderer, scene, camera, clock;
  try {
    renderer = new THREE.WebGLRenderer({ canvas: document.getElementById('c'), antialias: true, alpha: true });
    renderer.setClearColor(0x05070f, 1);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    renderer.setSize(innerWidth, innerHeight);
    renderer.outputEncoding = THREE.sRGBEncoding;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
  } catch (e) { fail('WebGL nem érhető el.'); return; }
  scene = new THREE.Scene();
  camera = new THREE.PerspectiveCamera(48, innerWidth / innerHeight, 0.1, 100);
  camera.position.set(0, 0.4, 7.2);
  clock = new THREE.Clock();
  scene.add(new THREE.HemisphereLight(0x9fb8ff, 0x202840, 1.0));
  const key = new THREE.PointLight(0x9fd0ff, 1.4, 40); key.position.set(3, 4, 6); scene.add(key);
  const warm = new THREE.PointLight(0xffd24d, 0.8, 40); warm.position.set(-4, -2, 4); scene.add(warm);

  // starfield
  (function () {
    const g = new THREE.BufferGeometry(); const n = 420; const p = new Float32Array(n * 3);
    for (let i = 0; i < n; i++) { const r = 18 + Math.random() * 24; const a = Math.random() * 6.28, b = Math.acos(2 * Math.random() - 1);
      p[i*3] = r*Math.sin(b)*Math.cos(a); p[i*3+1] = r*Math.sin(b)*Math.sin(a); p[i*3+2] = r*Math.cos(b) - 10; }
    g.setAttribute('position', new THREE.BufferAttribute(p, 3));
    scene.add(new THREE.Points(g, new THREE.PointsMaterial({ color: 0xaaccff, size: 0.08, sizeAttenuation: true, transparent: true, opacity: 0.8 })));
  })();

  // Master Core: fractured glowing crystal that reassembles as you progress
  const core = new THREE.Group(); core.position.set(0, 1.35, 0); scene.add(core);
  const coreMat = new THREE.MeshStandardMaterial({ color: 0x6f9bff, emissive: 0x2a4cff, emissiveIntensity: 0.4, roughness: 0.25, metalness: 0.5, flatShading: true });
  const shards = [];
  for (let i = 0; i < 10; i++) {
    const s = new THREE.Mesh(new THREE.TetrahedronGeometry(0.34 + Math.random() * 0.2, 0), coreMat);
    const a = Math.random() * 6.28, b = Math.acos(2 * Math.random() - 1);
    s.userData.dir = new THREE.Vector3(Math.sin(b)*Math.cos(a), Math.sin(b)*Math.sin(a), Math.cos(b));
    s.userData.spin = new THREE.Vector3(Math.random(), Math.random(), Math.random());
    core.add(s); shards.push(s);
  }

  // two pedestals + a result anchor
  function pedestal(x) {
    const g = new THREE.Group(); g.position.set(x, -1.2, 0.4);
    const base = new THREE.Mesh(new THREE.CylinderGeometry(0.5, 0.62, 0.34, 8), new THREE.MeshStandardMaterial({ color: 0x26314e, roughness: 0.7, metalness: 0.3, flatShading: true }));
    g.add(base);
    const ring = new THREE.Mesh(new THREE.TorusGeometry(0.42, 0.04, 8, 24), new THREE.MeshStandardMaterial({ color: 0x6f9bff, emissive: 0x3050ff, emissiveIntensity: 0.5 }));
    ring.rotation.x = Math.PI / 2; ring.position.y = 0.2; g.add(ring);
    scene.add(g); return g;
  }
  const padL = pedestal(-1.5), padR = pedestal(1.5);

  // emoji -> sprite (cached textures)
  const texCache = {};
  function emojiTex(em) {
    if (texCache[em]) return texCache[em];
    const cv = document.createElement('canvas'); cv.width = cv.height = 128;
    const cx = cv.getContext('2d'); cx.textAlign = 'center'; cx.textBaseline = 'middle';
    cx.font = '96px serif'; cx.fillText(em, 64, 72);
    const t = new THREE.CanvasTexture(cv); t.anisotropy = 2; texCache[em] = t; return t;
  }
  function makeSprite(em) {
    const sp = new THREE.Sprite(new THREE.SpriteMaterial({ map: emojiTex(em), transparent: true }));
    sp.scale.set(0.9, 0.9, 0.9); return sp;
  }
  let spriteL = null, spriteR = null;
  function showOnPad(pad, em, hold) {
    if (pad.userData.sp) { pad.remove(pad.userData.sp); pad.userData.sp = null; }
    if (em) { const s = makeSprite(em); s.position.y = 0.8; pad.add(s); pad.userData.sp = s; }
    return pad.userData.sp;
  }

  // floating particles on merge
  const fx = [];
  function burst(pos, color, n) {
    for (let i = 0; i < (n || 14); i++) {
      const m = new THREE.Mesh(new THREE.TetrahedronGeometry(0.07, 0), new THREE.MeshStandardMaterial({ color: color, emissive: color, emissiveIntensity: 0.8, flatShading: true }));
      m.position.copy(pos);
      const a = Math.random() * 6.28, b = Math.acos(2 * Math.random() - 1), sp = 1.5 + Math.random() * 3;
      m.userData = { v: new THREE.Vector3(Math.sin(b)*Math.cos(a)*sp, Math.sin(b)*Math.sin(a)*sp, Math.cos(b)*sp), life: 0.9 };
      scene.add(m); fx.push(m);
    }
  }

  // ---------- DOM ----------
  const el = (id) => document.getElementById(id);
  function eraOrder(id) { return Object.keys(E).indexOf(id); }

  // transient toast (streak / hint feedback)
  let toastT = null;
  function toast(html, ms) {
    const t = el('toast'); if (!t) return;
    t.innerHTML = '<span class="tbox">' + html + '</span>';
    t.classList.add('show');
    if (toastT) clearTimeout(toastT);
    toastT = setTimeout(() => t.classList.remove('show'), ms || 2600);
  }

  // build a reverse map (result -> [a,b]) once, for hints and progress
  const RBYRES = {};
  for (const k in R) { const res = R[k]; if (!RBYRES[res]) RBYRES[res] = k.split('+'); }

  // Hint: find a not-yet-discovered invention whose BOTH ingredients are already owned.
  // Prefer the earliest in tech-tree order so hints nudge along the natural progression.
  function findHint() {
    let best = null, bestOrd = Infinity;
    for (const k in R) {
      const res = R[k];
      if (disc.has(res)) continue;
      const [a, b] = k.split('+');
      if (disc.has(a) && disc.has(b)) {
        const ord = eraOrder(res);
        if (ord < bestOrd) { bestOrd = ord; best = { a, b, res }; }
      }
    }
    return best;
  }
  function giveHint() {
    if (el('reveal').style.display === 'flex' || el('codex').style.display === 'block' || el('masters').style.display === 'block') return;
    const h = findHint();
    if (!h) { toast('Mindent felfedeztél, ami most elérhető! 🎉', 3200); return; }
    // auto-stage the two ingredients on the pedestals and flash them in the inventory
    sel = [h.a];
    showOnPad(padL, E[h.a].e); showOnPad(padR, E[h.b].e);
    el('combo').textContent = E[h.a].e + ' + ' + E[h.b].e + ' = ?';
    toast('Próbáld ki: ' + E[h.a].e + ' + ' + E[h.b].e, 3200);
    renderInv();
    // pre-select the first; one more tap on the 2nd ingredient completes it
    flashChips([h.a, h.b]);
  }
  function flashChips(ids) {
    const inv = el('inv'); if (!inv) return;
    inv.querySelectorAll('.chip').forEach(c => {
      if (ids.indexOf(c.dataset.id) >= 0) { c.classList.add('hintglow'); setTimeout(() => c.classList.remove('hintglow'), 2400); }
    });
    const first = inv.querySelector('.chip[data-id="' + ids[0] + '"]');
    if (first && first.scrollIntoView) first.scrollIntoView({ inline: 'center', block: 'nearest' });
  }
  function renderInv() {
    const inv = el('inv'); inv.innerHTML = '';
    const ids = [...disc].sort((a, b) => eraOrder(a) - eraOrder(b));
    for (const id of ids) {
      const d = E[id];
      const c = document.createElement('div'); c.className = 'chip' + (sel.includes(id) ? ' sel' : '');
      c.dataset.id = id;
      c.innerHTML = '<div class="e">' + d.e + '</div><div class="n">' + d.n + '</div>';
      c.onclick = () => pick(id);
      inv.appendChild(c);
    }
  }
  function updateTop() {
    el('cx').textContent = disc.size + '/' + TOTAL;
    el('core').textContent = Math.round(disc.size / TOTAL * 100) + '%';
    el('streak').textContent = meta.streak || 0;
    const fr = el('frag'); if (fr) fr.textContent = meta.fragments || 0;
    if (typeof checkNewHeroes === 'function') checkNewHeroes();
  }

  function pick(id) {
    if (el('reveal').style.display === 'flex') return;
    sel.push(id);
    if (sel.length === 1) {
      showOnPad(padL, E[id].e); showOnPad(padR, null);
      el('combo').textContent = E[id].e + ' + ?';
    } else if (sel.length === 2) {
      showOnPad(padR, E[sel[1]].e);
      el('combo').textContent = E[sel[0]].e + ' + ' + E[sel[1]].e;
      setTimeout(combine, 260);
    }
    renderInv();
  }

  function combine() {
    const [a, b] = sel;
    const r = R[[a, b].sort().join('+')];
    const mid = new THREE.Vector3(0, 0.1, 0.6);
    if (!r) {
      burst(mid, 0x8090b0, 8);
      el('combo').textContent = '✗ nincs kombináció';
      reset(420); return;
    }
    burst(core.position.clone(), 0xffd24d, 18);
    coreFlash = 1;
    if (!disc.has(r)) {
      disc.add(r); save();
      meta.foundToday = (meta.foundToday || 0) + 1; saveMeta();
      updateTop();
      showReveal(r, true);
    } else {
      el('combo').textContent = E[r].e + ' (már megvan)';
    }
    reset(r && !disc.has(r) ? 0 : 360);
  }
  function reset(delay) {
    setTimeout(() => { sel = []; showOnPad(padL, null); showOnPad(padR, null);
      if (el('reveal').style.display !== 'flex') el('combo').textContent = 'Válassz két elemet';
      renderInv(); }, delay || 0);
  }

  function showReveal(id, isNew) {
    const d = E[id];
    el('revKicker').textContent = id === 'type3' ? '🏆 A CIVILIZÁCIÓ ÚJJÁÉPÜLT' : 'ÚJ FELFEDEZÉS';
    el('revEmoji').textContent = d.e;
    el('revName').textContent = d.n;
    el('revEra').textContent = d.era.toUpperCase();
    el('revLore').textContent = '„' + d.lore + '”';
    el('reveal').style.display = 'flex';
  }
  el('revBtn').onclick = () => { el('reveal').style.display = 'none'; sel = []; el('combo').textContent = 'Válassz két elemet'; renderInv(); };

  // codex
  function renderCodex() {
    el('codexSub').textContent = disc.size + ' / ' + TOTAL + ' felfedezve';
    const grid = el('grid'); grid.innerHTML = '';
    for (const id of Object.keys(E)) {
      const d = E[id], have = disc.has(id);
      const cell = document.createElement('div'); cell.className = 'cell' + (have ? '' : ' lock');
      cell.innerHTML = '<div class="e">' + (have ? d.e : '❓') + '</div><div class="n">' + (have ? d.n : '???') + '</div>';
      grid.appendChild(cell);
    }
  }
  el('codexBtn').onclick = () => { renderCodex(); el('codex').style.display = 'block'; };
  el('codexClose').onclick = () => { el('codex').style.display = 'none'; };

  // Masters (era bosses): real duels. The list shows who you can challenge.
  function bossReady(m) { const req = m.req || []; return req.length > 0 && req.every(id => disc.has(id)); }
  function renderMasters() {
    const g = el('mastersGrid'); g.innerHTML = '';
    let ready = 0, won = 0;
    for (const m of MASTERS) {
      const req = m.req || [];
      const ok = bossReady(m), done = isDefeated(m.id);
      if (ok) ready++; if (done) won++;
      const reqHtml = req.map(id => '<span style="opacity:' + (disc.has(id) ? 1 : .3) + '">' + ((E[id] && E[id].e) || '❓') + '</span>').join(' ');
      const status = done ? '<b style="color:#7fe08a">LEGYŐZVE ✅</b>' : ok ? '' : '<span style="color:#9fb0d8">zárva 🔒</span>';
      const card = document.createElement('div'); card.className = 'mcard' + (done ? ' done' : ok ? ' ready' : '');
      card.innerHTML = '<div class="me">' + m.emoji + '</div><div class="mb"><div class="mn">' + m.name + (done ? ' 👑' : '') + '</div>' +
        '<div class="mera">' + m.era + '</div><div class="mmech">' + (m.mechanic || '') + '</div>' +
        '<div class="mreq">' + reqHtml + (status ? ' &nbsp; ' + status : '') + '</div></div>';
      if (ok) {
        const btn = document.createElement('button');
        btn.className = 'mfight' + (done ? ' again' : '');
        btn.textContent = done ? '↻' : '⚔️ CSATA';
        btn.onclick = () => startBattle(m);
        card.appendChild(btn);
      }
      g.appendChild(card);
    }
    el('mastersSub').innerHTML = '⚔️ ' + won + ' legyőzve · ' + Math.max(0, ready - won) + ' kihívható · ' + MASTERS.length + ' korszak';
  }
  el('mastersBtn').onclick = () => { renderMasters(); el('masters').style.display = 'block'; };
  el('mastersClose').onclick = () => { el('masters').style.display = 'none'; };

  // ---------- boss duel engine ----------
  // Your discovered tech is your arsenal. Each turn you draw a hand; the boss's required
  // tech are its weaknesses (crit damage). The boss counters and enrages every 3rd turn.
  const bEl = (id) => document.getElementById(id);
  let battle = null;
  let shake = 0, bossHit = 0, bossMode = false, bossExplode = 0;
  // each Master fights differently (kept mild so duels stay winnable when prepared)
  const MMECH = {
    master_stone: 'armor', master_knowledge: 'regen', master_industrial: 'enrage2',
    master_information: 'drain', master_modern: 'regen', master_space: 'armor',
    master_interstellar: 'regen', master_galactic: 'enrage2', master_type3: 'regen',
  };
  const MECHLABEL = { armor: '🛡️ Páncél (–2 a nem-kritikus sebzésből)', regen: '♻️ Regeneráció (+2 ÉP/kör)', enrage2: '⚡ Gyakori düh (minden 2. kör)', drain: '🌀 Sebzéscsökkentés', none: '' };

  function masterTier(m) { const i = MASTERS.indexOf(m); return i < 0 ? 0 : i; }
  function shuffle(a) { for (let i = a.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); const t = a[i]; a[i] = a[j]; a[j] = t; } return a; }

  function startBattle(m) {
    if (!bossReady(m)) { toast('Előbb fedezd fel a Master technológiáit 🔒', 2800); return; }
    const tier = masterTier(m);
    const coreMax = 100 + 10 * (meta.wins || 0);
    const mech = MMECH[m.id] || 'none';
    const heroId = (meta.equipHero && heroOwned(meta.equipHero)) ? meta.equipHero : null;
    battle = {
      m: m, tier: tier, bossHP: 100, bossMax: 100, core: coreMax, coreMax: coreMax,
      turn: 0, lastWeak: null, over: false, hand: [],
      weakDmg: 10, normDmg: 4, counter: 4.5 + tier * 0.65,
      mech: mech, hero: heroId, heroUsed: false, shield: false, crit2: false, freeHit: false, drainNext: false,
    };
    bEl('bEmoji').textContent = m.emoji;
    bEl('bName').textContent = m.name;
    bEl('bEra').textContent = m.era + (MECHLABEL[mech] ? ' · ' + MECHLABEL[mech].split(' ')[0] + ' ' + MECHLABEL[mech].split(' ')[1] : '');
    bEl('bMech').textContent = (MECHLABEL[mech] ? '【' + MECHLABEL[mech] + '】 ' : '') + '„' + (m.mechanic || '') + '”';
    bEl('bEnd').classList.remove('show');
    // equipped-hero ability button
    const hb = bEl('bHero');
    if (heroId) {
      const ab = HERO(heroId).battle || {};
      hb.style.display = 'block'; hb.classList.remove('used'); hb.disabled = false;
      hb.textContent = HERO(heroId).emoji + ' ' + (ab.name || 'Képesség') + ' — ' + (ab.desc || '');
      hb.onclick = useHeroAbility;
    } else { hb.style.display = 'none'; }
    el('masters').style.display = 'none';
    bEl('battle').classList.add('show');
    bossVisual(true);
    bLog('A párbaj megkezdődött — vesd be a felfedezett technológiáidat! ⚔️' + (heroId ? '<br>🦸 ' + HERO(heroId).name.split(',')[0] + ' melletted harcol.' : ''));
    drawHand(); renderBattle();
  }
  function bLog(html) { const l = bEl('bLog'); if (l) l.innerHTML = html; }

  function drawHand() {
    const m = battle.m, req = m.req || [];
    const weaks = req.filter(id => disc.has(id) && id !== battle.lastWeak);
    const others = [...disc].filter(id => req.indexOf(id) < 0);
    const hand = [];
    shuffle(weaks); shuffle(others);
    for (let i = 0; i < weaks.length && hand.length < 2; i++) hand.push(weaks[i]);
    for (let i = 0; i < others.length && hand.length < 4; i++) hand.push(others[i]);
    const reqOwned = req.filter(id => disc.has(id));
    for (let gi = 0; hand.length < 4 && reqOwned.length; gi++) hand.push(reqOwned[gi % reqOwned.length]);
    battle.hand = hand.slice(0, 4);
  }

  function renderBattle() {
    const b = battle; if (!b) return;
    bEl('bHP').style.width = Math.max(0, b.bossHP) / b.bossMax * 100 + '%';
    bEl('bHPtxt').textContent = Math.max(0, Math.ceil(b.bossHP)) + '%';
    bEl('bCore').style.width = Math.max(0, b.core) / b.coreMax * 100 + '%';
    bEl('bCoretxt').textContent = Math.max(0, Math.ceil(b.core)) + ' / ' + b.coreMax;
    const wrap = bEl('bHand'); wrap.innerHTML = '';
    const req = b.m.req || [];
    for (const id of b.hand) {
      const weak = req.indexOf(id) >= 0;
      const dmg = weak ? b.weakDmg : b.normDmg;
      const c = document.createElement('button'); c.className = 'bcard' + (weak ? ' weak' : '');
      c.innerHTML = '<div class="bce">' + E[id].e + '</div><div class="bcn">' + E[id].n +
        '</div><div class="bcd">' + (weak ? '⚔️' : '') + dmg + '</div>';
      c.onclick = () => playCard(id, weak);
      wrap.appendChild(c);
    }
  }

  function playCard(id, weak) {
    const b = battle; if (!b || b.over) return;
    let dmg = weak ? b.weakDmg : b.normDmg;
    if (b.mech === 'armor' && !weak) dmg = Math.max(1, dmg - 2);
    let crit2 = false, drained = false;
    if (b.crit2) { dmg *= 2; b.crit2 = false; crit2 = true; }
    if (b.drainNext) { dmg = Math.max(1, Math.round(dmg * 0.5)); b.drainNext = false; drained = true; }
    b.bossHP -= dmg;
    if (weak) b.lastWeak = id;
    burst(core.position.clone(), weak ? 0xffd24d : 0x9fd0ff, weak ? 20 : 10);
    coreFlash = 1; shake = weak ? 0.5 : 0.3; bossHit = 1;
    if (b.bossHP <= 0) { renderBattle(); setTimeout(playerWin, 260); return; }
    let regenTxt = '';
    if (b.mech === 'regen' && b.bossHP < b.bossMax) { b.bossHP = Math.min(b.bossMax, b.bossHP + 2); regenTxt = ' ♻️ +2 a Masternek.'; }
    b.turn++;
    let line = E[id].e + ' ' + (weak ? '<b style="color:#ffd24d">KRITIKUS</b> ' : '') + (crit2 ? '<b style="color:#9fe0ff">⚡2× </b>' : '') +
      (drained ? '<span style="color:#c9a6ff">🌀 </span>' : '') + '−' + dmg + ' a Masternek!' + regenTxt + '<br>';
    if (b.freeHit) {
      b.freeHit = false;
      line += '🔄 Ingyenes támadás — nincs ellentámadás.';
    } else {
      const enrage = b.mech === 'enrage2' ? (b.turn % 2 === 0) : (b.turn % 3 === 0);
      let cd = Math.round(b.counter * (0.85 + Math.random() * 0.3) * (enrage ? 1.5 : 1));
      if (b.shield) { b.shield = false; cd = 0; line += '🛡️ A pajzs elnyelte a csapást!'; }
      else line += (enrage ? '⚠️ <b style="color:#ff8a6b">Feltöltött csapás!</b> ' : 'A Master visszavág: ') + '−' + cd + ' a Magnak.';
      b.core -= cd;
      if (b.mech === 'drain' && b.turn % 3 === 0) { b.drainNext = true; line += ' <span style="color:#c9a6ff">A következő lapod gyengül.</span>'; }
    }
    bLog(line);
    if (b.core <= 0) { renderBattle(); setTimeout(playerLose, 260); return; }
    drawHand(); renderBattle();
  }

  function useHeroAbility() {
    const b = battle; if (!b || b.over || b.heroUsed || !b.hero) return;
    const ab = HERO(b.hero).battle || {}; b.heroUsed = true;
    const hb = bEl('bHero'); hb.classList.add('used'); hb.disabled = true; hb.textContent = '✓ ' + (ab.name || 'Képesség') + ' bevetve';
    burst(core.position.clone(), 0xffd24d, 16); coreFlash = 1;
    const who = HERO(b.hero).emoji + ' ' + (ab.name || '');
    if (ab.type === 'heal') { b.core = Math.min(b.coreMax, b.core + ab.val); bLog(who + ': +' + ab.val + ' Mag-integritás 💚'); renderBattle(); }
    else if (ab.type === 'shield') { b.shield = true; bLog(who + ': a következő Master-csapás elnyelve 🛡️'); }
    else if (ab.type === 'crit') { b.crit2 = true; bLog(who + ': a következő lapod dupla sebzés ⚡'); }
    else if (ab.type === 'draw') { b.freeHit = true; drawHand(); bLog(who + ': új kéz — a következő lap ingyenes 🔄'); renderBattle(); }
    else if (ab.type === 'nuke') {
      b.bossHP -= ab.val; shake = 0.6; bossHit = 1;
      if (b.bossHP <= 0) { bLog(who + ': −' + ab.val + ' 💥'); renderBattle(); setTimeout(playerWin, 260); return; }
      bLog(who + ': −' + ab.val + ' a Masternek 💥'); renderBattle();
    }
  }

  function playerWin() {
    const b = battle; if (!b) return; b.over = true;
    bossVisual(false, true);
    if (!isDefeated(b.m.id)) { meta.defeated.push(b.m.id); meta.wins = (meta.wins || 0) + 1; saveMeta(); }
    bEl('bEndEmoji').textContent = '🏆';
    bEl('bEndTitle').textContent = 'MASTER LEGYŐZVE';
    bEl('bEndSub').innerHTML = b.m.emoji + ' <b>' + b.m.name + '</b> elbukott. A Mag erősebb lett.<br>Legyőzött Masterek: ' + (meta.wins || 0) + ' / ' + MASTERS.length;
    bEl('bEnd').classList.add('show');
  }
  function playerLose() {
    const b = battle; if (!b) return; b.over = true;
    bossVisual(false);
    bEl('bEndEmoji').textContent = '💥';
    bEl('bEndTitle').textContent = 'A MAG MEGINGOTT';
    bEl('bEndSub').innerHTML = 'A Mag-integritás elfogyott. Fedezz fel több technológiát — minden új találmány erősíti a fegyvertáradat —, vagy próbáld újra.';
    bEl('bEnd').classList.add('show');
  }
  function closeBattle() {
    bEl('battle').classList.remove('show');
    bEl('bEnd').classList.remove('show');
    bossVisual(false);
    battle = null; updateTop();
  }
  // morph the Master Core into the boss (red, swollen, pulsing) and back
  function bossVisual(on, explode) {
    bossMode = on;
    if (on) { coreMat.color.setHex(0xff5a3c); coreMat.emissive.setHex(0xff2a1a); }
    else { coreMat.color.setHex(0x6f9bff); coreMat.emissive.setHex(0x2a4cff); if (!explode) core.scale.setScalar(1); }
    if (explode) { bossExplode = 1; burst(core.position.clone(), 0xff7a3d, 40); }
  }
  bEl('bEndBtn').onclick = () => { closeBattle(); renderMasters(); el('masters').style.display = 'block'; };
  bEl('bClose').onclick = () => { closeBattle(); renderMasters(); el('masters').style.display = 'block'; };

  // ---------- heroes · worlds · expeditions (meta layer) ----------
  // Config layered onto the generated MM_DATA so balance lives in one place.
  const HEROCFG = {
    emberke:      { unlock: {},          spd: 0.95, frag: 1.0,  rare: 1.0, battle: { type: 'nuke',   val: 28, name: 'Szikracsapás', desc: 'Azonnal 28 sebzés a Masternek.' } },
    oregkronikas: { unlock: { disc: 18 },spd: 1.0,  frag: 1.3,  rare: 1.0, battle: { type: 'heal',   val: 30, name: 'Kódex-emlék',  desc: '+30 Mag-integritás.' } },
    vasanya:      { unlock: { disc: 35 },spd: 0.9,  frag: 1.25, rare: 1.0, battle: { type: 'shield', val: 1,  name: 'Vaspajzs',     desc: 'A következő Master-csapást elnyeli.' } },
    aramvolgyi:   { unlock: { wins: 1 }, spd: 0.8,  frag: 1.0,  rare: 1.0, battle: { type: 'crit',   val: 2,  name: 'Túltöltés',    desc: 'A következő lapod dupla sebzést okoz.' } },
    szellocsillag:{ unlock: { disc: 70 },spd: 0.6,  frag: 1.1,  rare: 2.0, battle: { type: 'draw',   val: 1,  name: 'Felderítés',   desc: 'Új kéz; a következő lap nem vált ki ellentámadást.' } },
    adatlany:     { unlock: { wins: 2 }, spd: 0.85, frag: 1.1,  rare: 1.4, battle: { type: 'draw',   val: 1,  name: 'Hálózat',      desc: 'Új kéz; a következő lap nem vált ki ellentámadást.' } },
    fenymag:      { unlock: { disc: 130 },spd:0.85, frag: 1.0,  rare: 1.3, battle: { type: 'heal',   val: 40, name: 'Gyógyítás',    desc: '+40 Mag-integritás.' } },
    csillagkohacs:{ unlock: { wins: 5 }, spd: 0.9,  frag: 1.2,  rare: 1.5, battle: { type: 'nuke',   val: 34, name: 'Csillagtűz',   desc: 'Azonnal 34 sebzés a Masternek.' } },
  };
  const WORLDCFG = {
    stone_world: { dur: 30,  frag: 5,  perk: 'plain',       unlock: {} },
    ice_world:   { dur: 50,  frag: 7,  perk: 'safe',        unlock: { disc: 15 } },
    jungle:      { dur: 60,  frag: 9,  perk: 'double_frag', unlock: { disc: 28 } },
    ocean:       { dur: 75,  frag: 11, perk: 'plain',       unlock: { disc: 45 } },
    volcano:     { dur: 70,  frag: 15, perk: 'risky',       unlock: { wins: 1 } },
    sky_kingdom: { dur: 90,  frag: 13, perk: 'rare',        unlock: { disc: 75 } },
    moon:        { dur: 45,  frag: 9,  perk: 'fast',        unlock: { wins: 2 } },
    mars:        { dur: 110, frag: 17, perk: 'plain',       unlock: { disc: 105 } },
    europa:      { dur: 130, frag: 22, perk: 'rare',        unlock: { wins: 3 } },
    dyson_ring:  { dur: 100, frag: 26, perk: 'double_frag', unlock: { disc: 150 } },
    black_hole:  { dur: 160, frag: 36, perk: 'jackpot',     unlock: { wins: 5 } },
    multiverse:  { dur: 150, frag: 30, perk: 'double_elem', unlock: { wins: 7 } },
  };
  const PERKTAG = { plain: '', safe: 'GARANTÁLT LELET', double_frag: 'DUPLA 💠', risky: 'KOCKÁZATOS', rare: 'RITKA LELET', fast: 'GYORS', jackpot: 'JACKPOT 💠💠', double_elem: '2× LELET' };

  const HD = (window.MM_DATA && MM_DATA.heroes) || [];
  const WD = (window.MM_DATA && MM_DATA.worlds) || [];
  const HEROES = {}; HD.forEach(h => { HEROES[h.id] = Object.assign({}, h, HEROCFG[h.id] || {}); });
  const WORLDS = {}; WD.forEach(w => { WORLDS[w.id] = Object.assign({}, w, WORLDCFG[w.id] || {}); });
  const HERO = (id) => HEROES[id];

  function unlockMet(u) { u = u || {}; if (u.wins && (meta.wins || 0) < u.wins) return false; if (u.disc && disc.size < u.disc) return false; return true; }
  function heroOwned(id) { return !!HEROES[id] && unlockMet(HEROES[id].unlock); }
  function worldUnlocked(id) { return !!WORLDS[id] && unlockMet(WORLDS[id].unlock); }
  function unlockText(u) { u = u || {}; if (u.wins) return 'Győzz le ' + u.wins + ' Mastert 🔒'; if (u.disc) return 'Fedezz fel ' + u.disc + ' elemet 🔒'; return ''; }
  function heroBusy(id) { for (const w in meta.expeds) if (meta.expeds[w].hero === id) return w; return null; }
  function idleHeroes() { return HD.map(h => h.id).filter(id => heroOwned(id) && !heroBusy(id)); }

  function reachableUndiscovered() {
    const out = [];
    for (const r in RBYRES) { if (!disc.has(r) && E[r]) { const p = RBYRES[r]; if (disc.has(p[0]) && disc.has(p[1])) out.push(r); } }
    return out;
  }
  function fmtTime(ms) { const s = Math.max(0, Math.ceil(ms / 1000)); const m = Math.floor(s / 60); return m + ':' + String(s % 60).padStart(2, '0'); }

  function startExpedition(wid, hid) {
    const w = WORLDS[wid]; if (!w || meta.expeds[wid] || !heroOwned(hid) || heroBusy(hid)) return;
    const dur = Math.round(w.dur * 1000 * (HERO(hid).spd || 1));
    meta.expeds[wid] = { hero: hid, start: Date.now(), dur: dur, notified: false };
    saveMeta(); updateMapBadge(); renderMap();
  }
  function claimExpedition(wid) {
    const e = meta.expeds[wid]; if (!e) return;
    if (Date.now() - e.start < e.dur) return;
    const w = WORLDS[wid], h = HERO(e.hero) || {};
    let fragMult = 1, pElem = 0.6, nElem = 1, rareDeep = false;
    switch (w.perk) {
      case 'safe': pElem = 1.0; break;
      case 'double_frag': fragMult = 2; pElem = 0.5; break;
      case 'risky': if (Math.random() < 0.25) { fragMult = 0.5; pElem = 0; } else { fragMult = 1.6; pElem = 0.7; } break;
      case 'rare': pElem = 0.85; rareDeep = true; break;
      case 'fast': fragMult = 0.9; pElem = 0.5; break;
      case 'jackpot': fragMult = 3; pElem = 0.5; break;
      case 'double_elem': fragMult = 1.2; pElem = 0.8; nElem = 2; break;
    }
    const frag = Math.max(1, Math.round(w.frag * (h.frag || 1) * fragMult));
    meta.fragments = (meta.fragments || 0) + frag;
    const chance = pElem * (h.rare || 1);
    const pool = reachableUndiscovered();
    const got = [];
    for (let i = 0; i < nElem && pool.length; i++) {
      if (Math.random() < chance || (w.perk === 'safe' && i === 0)) {
        let idx = Math.floor(Math.random() * pool.length);
        if (rareDeep) idx = pool.reduce((bi, id, ii) => eraOrder(id) > eraOrder(pool[bi]) ? ii : bi, 0);
        const id = pool.splice(idx, 1)[0];
        disc.add(id); got.push(id);
      }
    }
    if (got.length) { save(); }
    delete meta.expeds[wid]; saveMeta(); updateTop(); updateMapBadge();
    toast('🗺️ Expedíció kész: +' + frag + ' 💠' + (got.length ? ' · új tudás: ' + got.map(id => E[id].e).join(' ') : ''), 3800);
    renderMap();
  }
  function skipExpedition(wid) {
    const e = meta.expeds[wid]; if (!e) return;
    const rem = e.dur - (Date.now() - e.start);
    if (rem <= 0) { claimExpedition(wid); return; }
    const cost = Math.max(1, Math.ceil(rem / 1000 / 15));
    if ((meta.fragments || 0) < cost) { toast('Nincs elég 💠 — ' + cost + ' kell a siettetéshez', 2800); return; }
    meta.fragments -= cost; e.start = Date.now() - e.dur; saveMeta(); claimExpedition(wid);
  }

  function renderMap() {
    const g = el('mapGrid'); if (!g) return; g.innerHTML = '';
    const now = Date.now();
    for (const w of WD) {
      const wid = w.id, cfg = WORLDS[wid];
      const e = meta.expeds[wid];
      const card = document.createElement('div'); card.className = 'wcard';
      const tag = PERKTAG[cfg.perk] || '';
      let body = '<div class="whead"><span class="we">' + w.emoji + '</span><span class="wn">' + w.name +
        '</span><span class="wtag">' + tag + '</span></div>';
      if (!worldUnlocked(wid)) {
        card.className += ' lock';
        body += '<div class="wmech">' + w.mechanic + '</div><div class="wstatus">' + unlockText(cfg.unlock) + '</div>';
      } else if (e) {
        const rem = e.dur - (now - e.start), ready = rem <= 0, hero = HERO(e.hero);
        card.className += ready ? ' ready' : ' run';
        const pct = Math.min(100, (now - e.start) / e.dur * 100);
        body += '<div class="wstatus">' + (hero ? hero.emoji + ' ' + hero.name.split(',')[0] : 'Hős') + ' · ' +
          (ready ? '<b style="color:#7fe08a">visszatért ✅</b>' : 'úton…') + '</div>' +
          '<div class="wprog"><div class="wfill" id="wp_' + wid + '" style="width:' + pct + '%"></div></div>' +
          '<div class="wrow"><span class="wstatus" id="wc_' + wid + '">' + (ready ? 'Kész!' : fmtTime(rem)) + '</span></div>';
        const row = document.createElement('div'); row.className = 'wrow'; row.style.marginTop = '8px';
        const main = document.createElement('button'); main.className = 'wbtn';
        main.textContent = ready ? '🎁 Begyűjt' : '⏩ Siettetés (💠' + Math.max(1, Math.ceil(rem / 1000 / 15)) + ')';
        if (!ready) main.classList.add('skip');
        main.onclick = ready ? () => claimExpedition(wid) : () => skipExpedition(wid);
        card.innerHTML = body; row.appendChild(main); card.appendChild(row); g.appendChild(card); continue;
      } else {
        body += '<div class="wmech">' + w.mechanic + '</div>';
        const idle = idleHeroes();
        if (idle.length) {
          body += '<div class="wstatus" style="margin-bottom:6px">Válassz hőst az expedícióhoz:</div>';
          card.innerHTML = body;
          const row = document.createElement('div'); row.className = 'wrow';
          idle.forEach(hid => {
            const h = HERO(hid);
            const chip = document.createElement('button'); chip.className = 'hchip';
            chip.innerHTML = '<span class="he">' + h.emoji + '</span>' + h.name.split(',')[0];
            chip.onclick = () => startExpedition(wid, hid);
            row.appendChild(chip);
          });
          card.appendChild(row); g.appendChild(card); continue;
        } else {
          body += '<div class="wstatus">Nincs szabad hős — old fel vagy hívj vissza egyet 🦸</div>';
        }
      }
      card.innerHTML = body; g.appendChild(card);
    }
    let ownedH = idleHeroes().length, running = Object.keys(meta.expeds).length;
    el('mapSub').innerHTML = '💠 ' + (meta.fragments || 0) + ' szilánk · ' + running + ' aktív expedíció · ' + ownedH + ' szabad hős';
  }

  function renderHeroes() {
    const g = el('heroesGrid'); if (!g) return; g.innerHTML = '';
    let owned = 0;
    for (const h of HD) {
      const have = heroOwned(h.id), busyAt = heroBusy(h.id), cfg = HEROES[h.id];
      if (have) owned++;
      const card = document.createElement('div'); card.className = 'hcard' + (have ? '' : ' lock') + (meta.equipHero === h.id ? ' equip' : '');
      const ab = cfg.battle || {};
      let state;
      if (!have) state = '<div class="hbstate" style="color:#9fb0d8">' + unlockText(cfg.unlock) + '</div>';
      else if (busyAt) state = '<div class="hbstate" style="color:#9fd0ff">Expedíción: ' + (WORLDS[busyAt] ? WORLDS[busyAt].emoji + ' ' + WORLDS[busyAt].name : busyAt) + '</div>';
      else state = '<div class="hbstate" style="color:#7fe08a">Szabad ✅</div>';
      card.innerHTML = '<div class="hbe">' + h.emoji + '</div><div class="hbb">' +
        '<div class="hbn">' + h.name + '</div>' +
        '<div class="hbrole">⚔️ ' + (ab.name || '—') + ' · 🗺️ ×' + (cfg.spd ? (1 / cfg.spd).toFixed(1) : '1') + ' tempó</div>' +
        '<div class="hbab">' + (ab.desc || '') + '</div>' +
        '<div class="hbab" style="color:#9fb0d8;margin-top:3px">' + h.ability + '</div>' + state + '</div>';
      if (have) {
        const eq = document.createElement('button'); eq.className = 'hbtn' + (meta.equipHero === h.id ? ' on' : '');
        eq.textContent = meta.equipHero === h.id ? '✓ Csatába állítva' : '⚔️ Csatába';
        eq.onclick = () => { meta.equipHero = (meta.equipHero === h.id ? null : h.id); saveMeta(); renderHeroes(); };
        card.querySelector('.hbb').appendChild(eq);
      }
      g.appendChild(card);
    }
    el('heroesSub').innerHTML = '🦸 ' + owned + ' / ' + HD.length + ' hős feloldva · állíts egyet csatába a Master-párbajokhoz';
  }

  function updateMapBadge() {
    const now = Date.now(); let ready = 0;
    for (const w in meta.expeds) if (meta.expeds[w].dur - (now - meta.expeds[w].start) <= 0) ready++;
    const b = el('mapBtn'); if (b) b.innerHTML = '🗺️' + (ready ? ' <b style="color:#7fe08a">' + ready + '</b>' : '');
  }
  function checkNewHeroes() {
    const newly = [];
    for (const id in HEROES) { if (heroOwned(id) && meta.heroSeen.indexOf(id) < 0) { meta.heroSeen.push(id); newly.push(id); } }
    if (newly.length) { saveMeta(); setTimeout(() => toast('🦸 Új hős: ' + newly.map(id => HERO(id).emoji + ' ' + HERO(id).name.split(',')[0]).join(', '), 3600), 900); }
  }
  // expedition ticker: live countdowns + return notifications even with the map closed
  setInterval(() => {
    const now = Date.now(); let newReady = false;
    for (const wid in meta.expeds) {
      const e = meta.expeds[wid], rem = e.dur - (now - e.start);
      if (rem <= 0 && !e.notified) { e.notified = true; newReady = true; }
      const cw = document.getElementById('wc_' + wid); if (cw) cw.textContent = rem > 0 ? fmtTime(rem) : 'Kész!';
      const wp = document.getElementById('wp_' + wid); if (wp) wp.style.width = Math.min(100, (now - e.start) / e.dur * 100) + '%';
    }
    if (newReady) {
      saveMeta(); updateMapBadge();
      if (el('map').style.display === 'block') renderMap();
      else toast('🗺️ Egy expedíció visszatért — gyűjtsd be a 🗺️ térképen!', 3400);
    }
  }, 1000);

  el('mapBtn').onclick = () => { renderMap(); el('map').style.display = 'block'; };
  el('mapClose').onclick = () => { el('map').style.display = 'none'; };
  el('heroesBtn').onclick = () => { renderHeroes(); el('heroes').style.display = 'block'; };
  el('heroesClose').onclick = () => { el('heroes').style.display = 'none'; };

  el('introBtn').onclick = () => { el('intro').style.display = 'none'; meta.introSeen = true; saveMeta(); maybeStreakToast(); };
  el('back').onclick = () => { save(); saveMeta(); if (window.Android && window.Android.back) window.Android.back(); };
  el('hintBtn').onclick = giveHint;
  el('streakPill').onclick = () => toast('🔥 ' + (meta.streak || 0) + ' napos széria · rekord ' + (meta.bestStreak || 0) + ' · ma ' + (meta.foundToday || 0) + ' felfedezés', 3200);

  // Welcome-back streak nudge: shown once per session, after the intro is dismissed.
  let streakToastShown = false;
  function maybeStreakToast() {
    if (streakToastShown || !streakAdvanced) return;
    streakToastShown = true;
    const pill = el('streakPill'); if (pill) { pill.classList.add('flash'); setTimeout(() => pill.classList.remove('flash'), 1000); }
    setTimeout(() => toast('Üdv újra! 🔥 ' + meta.streak + ' napos szériában vagy', 3400), 450);
  }

  // ---------- loop ----------
  let coreFlash = 0;
  function frame() {
    const dt = Math.min(clock.getDelta(), 0.05); const t = clock.elapsedTime;
    const ratio = disc.size / TOTAL;
    core.rotation.y += dt * 0.5; core.rotation.x = Math.sin(t * 0.3) * 0.15;
    const gather = 0.2 + (1 - ratio) * 1.1;             // shards pull in as you progress
    for (const s of shards) {
      s.position.copy(s.userData.dir).multiplyScalar(gather + Math.sin(t * 1.5) * 0.04);
      s.rotation.x += dt * s.userData.spin.x; s.rotation.y += dt * s.userData.spin.y;
    }
    if (bossExplode > 0) {
      bossExplode = Math.max(0, bossExplode - dt * 1.5);
      core.scale.setScalar(Math.max(0.001, bossExplode));
      coreMat.emissiveIntensity = 1.5;
    } else if (bossMode) {
      coreMat.emissiveIntensity = 1.2 + Math.sin(t * 6) * 0.2 + bossHit * 1.2;
      core.scale.setScalar(1.7 + Math.sin(t * 2.2) * 0.06 + bossHit * 0.25);
    } else {
      coreMat.emissiveIntensity = 0.3 + ratio * 0.7 + coreFlash * 0.8;
    }
    coreFlash = Math.max(0, coreFlash - dt * 2);
    bossHit = Math.max(0, bossHit - dt * 2.5);
    if (shake > 0) {
      camera.position.x = Math.sin(t * 50) * shake * 0.16;
      camera.position.y = 0.4 + Math.cos(t * 47) * shake * 0.12;
      shake = Math.max(0, shake - dt * 1.6);
    } else if (camera.position.x !== 0) { camera.position.x = 0; camera.position.y = 0.4; }
    [padL, padR].forEach(p => { if (p.userData.sp) p.userData.sp.position.y = 0.8 + Math.sin(t * 2) * 0.05; });
    for (let i = fx.length - 1; i >= 0; i--) { const m = fx[i]; m.userData.life -= dt;
      m.userData.v.multiplyScalar(0.96); m.position.addScaledVector(m.userData.v, dt);
      m.rotation.x += dt * 4; m.scale.setScalar(Math.max(0.01, m.userData.life));
      if (m.userData.life <= 0) { scene.remove(m); fx.splice(i, 1); } }
    renderer.render(scene, camera);
    requestAnimationFrame(frame);
  }
  addEventListener('resize', () => { camera.aspect = innerWidth / innerHeight; camera.updateProjectionMatrix(); renderer.setSize(innerWidth, innerHeight); });
  setInterval(save, 5000);

  updateTop(); renderInv(); updateMapBadge();
  // Intro only on the very first run; returning players land straight in the game.
  if (meta.introSeen) { el('intro').style.display = 'none'; maybeStreakToast(); }
  if (boot) boot.style.display = 'none';
  requestAnimationFrame(frame);
})();
