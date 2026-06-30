/* MicroMasters 3D — a low-poly WebGL game (Three.js r148, offline in a WebView).
   Loop: workers harvest resource nodes -> resources -> hire workers / build towers ->
   towers + taps clear the pests -> destroy the nest -> world reclaimed. */
(function () {
  'use strict';
  const boot = document.getElementById('boot');
  function fail(msg) { if (boot) { boot.textContent = msg; boot.style.display = 'flex'; } }
  if (typeof THREE === 'undefined') { fail('A 3D motor nem töltött be.'); return; }

  // ---- per-world theme ----
  const WORLDS = {
    kitchen:  { name:'Konyha',     sky:['#ffd9a3','#ffb36b'], ground:0x8a5a2e, grass:0xb07a3e, accent:0xffd27a, node:0xfff1c0, pest:0x6b3b2a },
    bathroom: { name:'Fürdőszoba', sky:['#bff3fa','#7fd8e6'], ground:0x2c8a93, grass:0x37a6b0, accent:0xbff3fa, node:0xd8fbff, pest:0x205058 },
    garden:   { name:'Kert',       sky:['#d6ffb0','#9fe07a'], ground:0x3c8a3c, grass:0x4fae4f, accent:0xe6ffc2, node:0xf0ffd0, pest:0x2c5e1e },
    spaceship:{ name:'Űrhajó',     sky:['#3a4e8c','#0e1430'], ground:0x243056, grass:0x33406b, accent:0x9fb8ff, node:0xbcd0ff, pest:0x10183a },
    workshop: { name:'Műhely',     sky:['#cdd2da','#8a8f99'], ground:0x55504a, grass:0x6b655d, accent:0xf2a33c, node:0xffe1b0, pest:0x33302c },
    fridge:   { name:'Hűtő',       sky:['#e8faff','#bfe8f5'], ground:0x3c7e96, grass:0x4f99b0, accent:0xe8faff, node:0xeafcff, pest:0x244a5a },
    toybox:   { name:'Játékdoboz', sky:['#ffe27a','#ff9f5a'], ground:0x3a7bd5, grass:0x4f90e0, accent:0xfff0a0, node:0xfff4c0, pest:0x2a3f6b },
  };
  const worldId = (new URLSearchParams(location.search).get('world')) || 'kitchen';
  const W = WORLDS[worldId] || WORLDS.kitchen;
  document.getElementById('sky').style.background = 'linear-gradient(180deg,' + W.sky[0] + ',' + W.sky[1] + ')';
  document.getElementById('world').textContent = W.name;

  // ---- state (persisted per world) ----
  const KEY = 'mm3d_' + worldId;
  let S = { res: 20, pop: 0, nest: 100 };
  try { const j = JSON.parse(localStorage.getItem(KEY) || 'null'); if (j) S = j; } catch (e) {}
  let hireCost = 15, towerCost = 40, won = S.nest <= 0;
  function save() { try { localStorage.setItem(KEY, JSON.stringify(S)); } catch (e) {} }

  // ---- three.js setup ----
  let renderer, scene, camera, clock;
  try {
    renderer = new THREE.WebGLRenderer({ canvas: document.getElementById('c'), antialias: true, alpha: true });
    renderer.setClearColor(0x000000, 0);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    renderer.setSize(innerWidth, innerHeight);
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    renderer.outputEncoding = THREE.sRGBEncoding;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.05;
  } catch (e) { fail('WebGL nem érhető el ezen az eszközön.'); return; }

  scene = new THREE.Scene();
  scene.fog = new THREE.Fog(new THREE.Color(W.sky[1]), 22, 46);
  camera = new THREE.PerspectiveCamera(46, innerWidth / innerHeight, 0.1, 200);
  clock = new THREE.Clock();

  const hemi = new THREE.HemisphereLight(new THREE.Color(W.sky[0]), new THREE.Color(W.ground), 0.95);
  scene.add(hemi);
  const sun = new THREE.DirectionalLight(0xffffff, 1.5);
  sun.position.set(8, 14, 6);
  sun.castShadow = true;
  sun.shadow.mapSize.set(1024, 1024);
  sun.shadow.camera.near = 1; sun.shadow.camera.far = 50;
  sun.shadow.camera.left = -12; sun.shadow.camera.right = 12;
  sun.shadow.camera.top = 12; sun.shadow.camera.bottom = -12;
  sun.shadow.bias = -0.0006;
  scene.add(sun);

  const ISLAND_R = 6.2;
  const std = (color, rough, metal) => new THREE.MeshStandardMaterial({ color: color, roughness: rough == null ? 0.85 : rough, metalness: metal || 0.0, flatShading: true });

  // floating island
  const island = new THREE.Group();
  const top = new THREE.Mesh(new THREE.CylinderGeometry(ISLAND_R, ISLAND_R * 0.96, 1.0, 9), std(W.grass));
  top.position.y = 0; top.receiveShadow = true; island.add(top);
  const dirt = new THREE.Mesh(new THREE.ConeGeometry(ISLAND_R * 0.95, 4.2, 9), std(W.ground, 1.0));
  dirt.position.y = -2.4; dirt.rotation.y = Math.PI / 9; island.add(dirt);
  scene.add(island);

  // scattered decor (trees + rocks)
  function tree(x, z) {
    const g = new THREE.Group();
    const trunk = new THREE.Mesh(new THREE.CylinderGeometry(0.12, 0.16, 0.6, 5), std(0x6b4a2a));
    trunk.position.y = 0.8; trunk.castShadow = true; g.add(trunk);
    const leaf = new THREE.Mesh(new THREE.IcosahedronGeometry(0.55, 0), std(W.grass === 0x4fae4f ? 0x3c8a3c : (W.accent)));
    leaf.position.y = 1.35; leaf.castShadow = true; g.add(leaf);
    g.position.set(x, 0.5, z); g.scale.setScalar(0.8 + Math.random() * 0.5);
    return g;
  }
  for (let i = 0; i < 7; i++) {
    const a = Math.random() * Math.PI * 2, r = ISLAND_R * (0.62 + Math.random() * 0.28);
    island.add(tree(Math.cos(a) * r, Math.sin(a) * r));
  }

  // base HQ (centre)
  const base = new THREE.Group();
  const bb = new THREE.Mesh(new THREE.BoxGeometry(1.8, 1.2, 1.8), std(W.accent, 0.7, 0.1));
  bb.position.y = 1.1; bb.castShadow = true; bb.receiveShadow = true; base.add(bb);
  const roof = new THREE.Mesh(new THREE.ConeGeometry(1.5, 1.0, 4), std(0xcb5a3c));
  roof.position.y = 2.2; roof.rotation.y = Math.PI / 4; roof.castShadow = true; base.add(roof);
  const flag = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.32, 0.04), std(0xffd24d, 0.6, 0.2));
  flag.position.set(0.32, 3.0, 0); base.add(flag);
  const pole = new THREE.Mesh(new THREE.CylinderGeometry(0.03, 0.03, 1.0, 6), std(0xffffff));
  pole.position.set(0.08, 2.9, 0); base.add(pole);
  base.position.y = 0.5; scene.add(base);

  // resource nodes
  const nodes = [];
  function makeNode(x, z) {
    const m = new THREE.Mesh(new THREE.IcosahedronGeometry(0.46, 0), new THREE.MeshStandardMaterial({ color: W.node, emissive: new THREE.Color(W.accent), emissiveIntensity: 0.35, roughness: 0.3, metalness: 0.1, flatShading: true }));
    m.position.set(x, 0.95, z); m.castShadow = true;
    m.userData = { baseY: 0.95, ph: Math.random() * 6.28 };
    scene.add(m); nodes.push(m); return m;
  }
  for (let i = 0; i < 5; i++) { const a = (i / 5) * Math.PI * 2 + 0.4; const r = ISLAND_R * 0.66; makeNode(Math.cos(a) * r, Math.sin(a) * r); }

  // nest (enemy spawner) at the rim
  const nestAng = Math.PI * 1.25, nestR = ISLAND_R * 0.82;
  const nest = new THREE.Group();
  const dome = new THREE.Mesh(new THREE.SphereGeometry(0.95, 8, 6, 0, Math.PI * 2, 0, Math.PI / 2), std(0x2a1c14, 1.0));
  dome.scale.y = 0.8; dome.castShadow = true; nest.add(dome);
  const spike = new THREE.Mesh(new THREE.ConeGeometry(0.2, 0.7, 5), std(0x14100c));
  spike.position.y = 0.85; nest.add(spike);
  nest.position.set(Math.cos(nestAng) * nestR, 0.5, Math.sin(nestAng) * nestR);
  scene.add(nest);

  // ---- entities ----
  const workers = [], pests = [], towers = [], fx = [];
  function rimPoint(rf) { const a = Math.random() * Math.PI * 2; const r = ISLAND_R * (rf || 0.6) * Math.random(); return new THREE.Vector3(Math.cos(a) * r, 0.5, Math.sin(a) * r); }

  function makeWorker() {
    const g = new THREE.Group();
    const body = new THREE.Mesh(new THREE.BoxGeometry(0.34, 0.4, 0.26), std(W.accent, 0.6));
    body.position.y = 0.42; body.castShadow = true; g.add(body);
    const head = new THREE.Mesh(new THREE.SphereGeometry(0.2, 10, 8), std(0xe8b888, 0.7));
    head.position.y = 0.78; head.castShadow = true; g.add(head);
    const hat = new THREE.Mesh(new THREE.ConeGeometry(0.24, 0.18, 8), std(0xffc83d, 0.5, 0.1));
    hat.position.y = 0.95; g.add(hat);
    const leftLeg = new THREE.Mesh(new THREE.BoxGeometry(0.1, 0.24, 0.1), std(0x394056));
    leftLeg.position.set(-0.08, 0.16, 0); g.add(leftLeg);
    const rightLeg = leftLeg.clone(); rightLeg.position.x = 0.08; g.add(rightLeg);
    const carry = new THREE.Mesh(new THREE.BoxGeometry(0.2, 0.2, 0.2), std(W.node, 0.4));
    carry.position.y = 1.05; carry.visible = false; g.add(carry);
    g.position.copy(rimPoint(0.5));
    scene.add(g);
    const w = { g, legs: [leftLeg, rightLeg], carry, state: 'toNode', target: null, t: 0, ph: Math.random() * 6.28 };
    workers.push(w); return w;
  }

  function makePest() {
    const g = new THREE.Group();
    const bodyMat = std(W.pest, 0.6);
    const b = new THREE.Mesh(new THREE.IcosahedronGeometry(0.28, 0), bodyMat);
    b.position.y = 0.3; b.castShadow = true; g.add(b);
    const eye1 = new THREE.Mesh(new THREE.SphereGeometry(0.06, 6, 6), std(0xff5050, 0.3, 0));
    eye1.position.set(0.12, 0.4, 0.18); g.add(eye1);
    const eye2 = eye1.clone(); eye2.position.x = -0.12; g.add(eye2);
    g.position.copy(nest.position.clone()).add(new THREE.Vector3((Math.random() - 0.5), 0, (Math.random() - 0.5)));
    g.position.y = 0.5;
    scene.add(g);
    const p = { g, body: b, hp: 2, ph: Math.random() * 6.28 };
    pests.push(p); return p;
  }

  function makeTower(pos) {
    const g = new THREE.Group();
    const baseM = new THREE.Mesh(new THREE.CylinderGeometry(0.34, 0.42, 0.5, 7), std(0x9aa3b2, 0.6, 0.3));
    baseM.position.y = 0.25; baseM.castShadow = true; g.add(baseM);
    const turret = new THREE.Mesh(new THREE.SphereGeometry(0.3, 10, 8), std(0x3aa0ff, 0.4, 0.4));
    turret.position.y = 0.65; g.add(turret);
    const barrel = new THREE.Mesh(new THREE.CylinderGeometry(0.07, 0.07, 0.5, 6), std(0x2a3550));
    barrel.rotation.z = Math.PI / 2; barrel.position.set(0.3, 0.65, 0); turret.add(barrel);
    g.position.copy(pos); g.position.y = 0.5;
    scene.add(g);
    const t = { g, turret, cd: 0, range: 3.4 };
    towers.push(t); return t;
  }

  function spark(pos, color, n) {
    for (let i = 0; i < (n || 8); i++) {
      const m = new THREE.Mesh(new THREE.IcosahedronGeometry(0.08, 0), new THREE.MeshStandardMaterial({ color: color, emissive: color, emissiveIntensity: 0.6, flatShading: true }));
      m.position.copy(pos);
      const a = Math.random() * 6.28, sp = 1.5 + Math.random() * 2.5;
      m.userData = { v: new THREE.Vector3(Math.cos(a) * sp, 2 + Math.random() * 2, Math.sin(a) * sp), life: 0.7 };
      scene.add(m); fx.push(m);
    }
  }

  function beam(from, to) {
    const dir = new THREE.Vector3().subVectors(to, from);
    const len = dir.length();
    const m = new THREE.Mesh(new THREE.CylinderGeometry(0.04, 0.04, len, 5), new THREE.MeshBasicMaterial({ color: 0x9fe0ff }));
    m.position.copy(from).add(dir.multiplyScalar(0.5));
    m.quaternion.setFromUnitVectors(new THREE.Vector3(0, 1, 0), dir.clone().normalize());
    m.userData = { life: 0.12, beam: true };
    scene.add(m); fx.push(m);
  }

  // ---- HUD ----
  const el = (id) => document.getElementById(id);
  function hud() {
    el('res').textContent = Math.floor(S.res);
    el('pop').textContent = S.pop;
    el('pests').textContent = won ? '0' : pests.length;
    el('hireCost').textContent = hireCost;
    el('towerCost').textContent = towerCost;
    el('hire').disabled = S.res < hireCost;
    el('tower').disabled = S.res < towerCost;
  }
  el('back').onclick = () => { save(); if (window.Android && window.Android.back) window.Android.back(); };
  el('hire').onclick = () => { if (S.res >= hireCost) { S.res -= hireCost; S.pop++; hireCost = Math.round(hireCost * 1.6); makeWorker(); save(); hud(); } };
  el('tower').onclick = () => { if (S.res >= towerCost) { S.res -= towerCost; towerCost = Math.round(towerCost * 1.8); makeTower(rimPoint(0.55)); save(); hud(); } };
  el('rally').onclick = () => { if (won) return; S.nest = Math.max(0, S.nest - 12); spark(nest.position.clone().setY(1), 0xffd24d, 14); checkWin(); hud(); };

  function checkWin() {
    if (!won && S.nest <= 0) {
      won = true; save();
      for (const p of pests) scene.remove(p.g); pests.length = 0;
      scene.remove(nest);
      el('bannerTxt').textContent = '🏆 Világ visszafoglalva!';
      el('banner').style.display = 'flex';
    }
  }
  el('bannerBtn').onclick = () => { el('banner').style.display = 'none'; };

  // start with a couple of workers
  for (let i = 0; i < Math.max(2, S.pop); i++) makeWorker();
  S.pop = workers.length;

  // ---- tap to squish pests / poke the nest ----
  const ray = new THREE.Raycaster(), ptr = new THREE.Vector2();
  function onTap(cx, cy) {
    ptr.x = (cx / innerWidth) * 2 - 1; ptr.y = -(cy / innerHeight) * 2 + 1;
    ray.setFromCamera(ptr, camera);
    const ph = ray.intersectObjects(pests.map(p => p.g), true)[0];
    if (ph) { const p = pests.find(p => p.g === ph.object || p.g === ph.object.parent); if (p) { killPest(p, true); return; } }
    if (!won) { const nh = ray.intersectObject(nest, true)[0]; if (nh) { S.nest = Math.max(0, S.nest - 4); spark(nest.position.clone().setY(1), 0xff7a4d, 6); checkWin(); hud(); return; } }
    // tapping the ground gives a small collect
    const gh = ray.intersectObject(top, false)[0];
    if (gh) { S.res += 2; spark(gh.point.clone().setY(0.6), 0xffd24d, 4); hud(); }
  }
  function killPest(p, reward) {
    spark(p.g.position.clone().setY(0.5), W.pest, 8);
    scene.remove(p.g);
    const i = pests.indexOf(p); if (i >= 0) pests.splice(i, 1);
    if (reward) { S.res += 5; }
    hud();
  }
  renderer.domElement.addEventListener('pointerdown', (e) => onTap(e.clientX, e.clientY));

  // ---- loop ----
  let camA = 0.7, spawnT = 2.5, harvestRate = 1;
  const tmp = new THREE.Vector3();
  function nearestNode(pos) { let best = null, bd = 1e9; for (const n of nodes) { const d = pos.distanceTo(n.position); if (d < bd) { bd = d; best = n; } } return best; }

  function frame() {
    const dt = Math.min(clock.getDelta(), 0.05);
    const t = clock.elapsedTime;

    // camera slow orbit
    camA += dt * 0.06;
    camera.position.set(Math.cos(camA) * 13, 9.5, Math.sin(camA) * 13);
    camera.lookAt(0, 1.2, 0);

    base.rotation.y += dt * 0.2;
    for (const n of nodes) { n.rotation.y += dt * 0.8; n.position.y = n.userData.baseY + Math.sin(t * 2 + n.userData.ph) * 0.08; }

    // workers: harvest loop
    for (const w of workers) {
      const g = w.g;
      const legSwing = Math.sin(t * 10 + w.ph);
      if (w.state === 'toNode') {
        if (!w.target) w.target = nearestNode(g.position);
        if (moveTo(g, w.target.position, dt, 2.2)) { w.state = 'harvest'; w.t = 0.8; }
        animLegs(w, legSwing);
      } else if (w.state === 'harvest') {
        w.t -= dt; g.scale.y = 1 + Math.sin(t * 14) * 0.05;
        if (w.t <= 0) { w.state = 'toBase'; w.carry.visible = true; g.scale.y = 1; }
      } else if (w.state === 'toBase') {
        if (moveTo(g, base.position, dt, 2.4)) { S.res += harvestRate; w.carry.visible = false; w.state = 'toNode'; w.target = null; flash(); }
        animLegs(w, legSwing);
      }
      g.position.y = 0.5 + Math.abs(Math.sin(t * 10 + w.ph)) * 0.06;
    }

    // pests spawn + advance toward base
    if (!won) {
      spawnT -= dt;
      if (spawnT <= 0 && pests.length < 7) { makePest(); spawnT = 3.2 + Math.random() * 2; }
      for (let i = pests.length - 1; i >= 0; i--) {
        const p = pests[i];
        p.g.rotation.y += dt * 2;
        p.body.position.y = 0.3 + Math.abs(Math.sin(t * 8 + p.ph)) * 0.12;
        if (moveTo(p.g, base.position, dt, 1.1)) { S.res = Math.max(0, S.res - 3); flashRed(); killPest(p, false); }
      }
    }

    // towers auto-fire
    for (const tw of towers) {
      tw.cd -= dt;
      let target = null, bd = tw.range;
      for (const p of pests) { const d = tw.g.position.distanceTo(p.g.position); if (d < bd) { bd = d; target = p; } }
      if (target) {
        tmp.copy(target.g.position).sub(tw.g.position);
        tw.turret.rotation.y = Math.atan2(tmp.x, tmp.z);
        if (tw.cd <= 0) {
          tw.cd = 0.8;
          beam(tw.g.position.clone().setY(1.15), target.g.position.clone().setY(0.5));
          target.hp -= 1; if (target.hp <= 0) killPest(target, false);
        }
      }
    }

    // fx
    for (let i = fx.length - 1; i >= 0; i--) {
      const m = fx[i]; m.userData.life -= dt;
      if (m.userData.beam) { m.scale.x = m.scale.z = Math.max(0.01, m.userData.life / 0.12); }
      else { m.userData.v.y -= 9 * dt; m.position.addScaledVector(m.userData.v, dt); m.scale.setScalar(Math.max(0.01, m.userData.life / 0.7)); }
      if (m.userData.life <= 0) { scene.remove(m); fx.splice(i, 1); }
    }

    renderer.render(scene, camera);
    requestAnimationFrame(frame);
  }

  function moveTo(g, target, dt, speed) {
    tmp.copy(target).setY(g.position.y).sub(g.position);
    const d = tmp.length();
    if (d < 0.9) return true;
    tmp.normalize();
    g.position.addScaledVector(tmp, Math.min(speed * dt, d));
    g.rotation.y = Math.atan2(tmp.x, tmp.z);
    return false;
  }
  function animLegs(w, s) { w.legs[0].rotation.x = s * 0.7; w.legs[1].rotation.x = -s * 0.7; }
  let flashT = 0, flashRedT = 0;
  function flash() { flashT = 0.15; }
  function flashRed() { flashRedT = 0.2; }

  addEventListener('resize', () => { camera.aspect = innerWidth / innerHeight; camera.updateProjectionMatrix(); renderer.setSize(innerWidth, innerHeight); });
  setInterval(save, 4000);

  hud();
  if (boot) boot.style.display = 'none';
  requestAnimationFrame(frame);
})();
