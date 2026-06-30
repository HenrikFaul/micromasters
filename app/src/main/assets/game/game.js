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
  const TOTAL = Object.keys(E).length;

  // ---------- state ----------
  const KEY = 'mm_codex_v1';
  let disc = new Set(BASES);
  try { const j = JSON.parse(localStorage.getItem(KEY) || 'null'); if (Array.isArray(j)) disc = new Set(j.filter(id => E[id])); } catch (e) {}
  BASES.forEach(b => disc.add(b));
  const save = () => { try { localStorage.setItem(KEY, JSON.stringify([...disc])); } catch (e) {} };
  let sel = [];           // selected element ids (max 2)

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
  function renderInv() {
    const inv = el('inv'); inv.innerHTML = '';
    const ids = [...disc].sort((a, b) => eraOrder(a) - eraOrder(b));
    for (const id of ids) {
      const d = E[id];
      const c = document.createElement('div'); c.className = 'chip' + (sel.includes(id) ? ' sel' : '');
      c.innerHTML = '<div class="e">' + d.e + '</div><div class="n">' + d.n + '</div>';
      c.onclick = () => pick(id);
      inv.appendChild(c);
    }
  }
  function updateTop() {
    el('cx').textContent = disc.size + '/' + TOTAL;
    el('core').textContent = Math.round(disc.size / TOTAL * 100) + '%';
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
      disc.add(r); save(); updateTop();
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

  el('introBtn').onclick = () => { el('intro').style.display = 'none'; };
  el('back').onclick = () => { save(); if (window.Android && window.Android.back) window.Android.back(); };

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
    coreMat.emissiveIntensity = 0.3 + ratio * 0.7 + coreFlash * 0.8;
    coreFlash = Math.max(0, coreFlash - dt * 2);
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

  updateTop(); renderInv();
  if (boot) boot.style.display = 'none';
  requestAnimationFrame(frame);
})();
