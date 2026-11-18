'use strict';

/* =========================================================
   LA MARCHE DES ÂMES — simulation de maquette
   Aucune donnée réelle n'est lue : les pas sont simulés.
   ========================================================= */

const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
const clamp = (v, a, b) => Math.min(b, Math.max(a, v));
const pad2 = n => String(n).padStart(2, '0');
const fmt = n => Math.round(n).toLocaleString('fr-FR');
const rnd = (a, b) => a + Math.random() * (b - a);
const randInt = (a, b) => Math.floor(rnd(a, b + 1));
const clockNow = () => {
  const d = new Date();
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
};
const timeAgo = (minutes) => {
  const d = new Date(Date.now() - minutes * 60000);
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
};
const isoDay = (offset = 0) => {
  const d = new Date();
  d.setDate(d.getDate() + offset);
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
};
const dayLabel = iso => `${iso.slice(8, 10)}/${iso.slice(5, 7)}`;
const dayLabelFull = iso => `${iso.slice(8, 10)}/${iso.slice(5, 7)}/${iso.slice(0, 4)}`;

/* ============================ DONNÉES ============================ */

const SLOT_ORDER = ['weapon', 'head', 'body', 'hands', 'feet', 'talisman'];
const SLOT_LABEL = {
  weapon: 'Arme', head: 'Tête', body: 'Torse',
  hands: 'Mains', feet: 'Bottes', talisman: 'Talisman'
};
const SLOT_KEY = {
  weapon: 'ARM', head: 'TÊT', body: 'TOR', hands: 'MAI', feet: 'BOT', talisman: 'TAL'
};

const STAT_LABEL = {
  atk: 'ATQ', hp: 'PV', vit: '% VIT', crit: '% crit.',
  regen: 'PV/s', ferveur: '% ferveur'
};

const MOB_POOL = {
  easy: [
    { name: 'Gobelin', file: 'goblin.png' },
    { name: 'Kobold', file: 'kobold.png' },
    { name: 'Chauve-souris', file: 'bat.png' },
    { name: 'Loup noir', file: 'wolf.png' },
    { name: 'Araignée', file: 'spider.png' },
    { name: 'Goule', file: 'ghoul.png' },
    { name: 'Scorpion', file: 'scorpion.png' }
  ],
  mid: [
    { name: 'Gnoll', file: 'gnoll.png' },
    { name: 'Orc', file: 'orc.png' },
    { name: 'Guerrier squelette', file: 'skeletal_warrior.png' },
    { name: 'Hobgobelin', file: 'hobgoblin.png' },
    { name: 'Naga', file: 'naga.png' },
    { name: 'Diablotin', file: 'crimson_imp.png' },
    { name: 'Manticore', file: 'manticore.png' }
  ],
  hard: [
    { name: 'Guerrier orc', file: 'orc_warrior.png' },
    { name: 'Ogre', file: 'ogre.png' },
    { name: 'Troll des abîmes', file: 'deep_troll.png' },
    { name: 'Ours noir', file: 'black_bear.png' },
    { name: 'Griffon', file: 'griffon.png' },
    { name: 'Golem de fer', file: 'iron_golem.png' },
    { name: 'Vampire', file: 'vampire.png' },
    { name: 'Spectre', file: 'ghost.png' },
    { name: 'Élémentaire de feu', file: 'fire_elemental.png' }
  ]
};

const BOSS_POOL = [
  { name: 'Hydre de Lerne', file: 'lernaean_hydra01.png', title: 'Gardien des Cryptes' },
  { name: 'Dragon de givre', file: 'ice_dragon.png', title: 'Gardien des Glaces' },
  { name: 'Géant de fer', file: 'iron_giant.png', title: 'Gardien de la Forge' },
  { name: 'Jörmungandr', file: 'jormungandr.png', title: 'Gardien des Abysses' },
  { name: 'Balrug', file: 'balrug.png', title: 'Gardien des Braises' },
  { name: 'Cerebov', file: 'cerebov.png', title: 'Seigneur de la Fournaise' },
  { name: "Dragon d'orage", file: 'storm_dragon.png', title: 'Gardien des Cimes' },
  { name: 'Liche ancienne', file: 'lich.png', title: 'Gardien des Serments' }
];

const CHEST_TYPES = {
  bois: {
    key: 'bois', label: 'COFFRE DE BOIS', quota: 150,
    goldMin: 40, goldMax: 80, loot: 'uncommon',
    reward: '40–80 or · 1 objet inhabituel'
  },
  fer: {
    key: 'fer', label: 'COFFRE DE FER', quota: 400,
    goldMin: 130, goldMax: 220, loot: 'rare',
    reward: '130–220 or · 1 objet rare'
  },
  runique: {
    key: 'runique', label: 'COFFRE RUNIQUE', quota: 900,
    goldMin: 380, goldMax: 550, loot: 'epic',
    reward: '380–550 or · 1 objet épique'
  }
};

const UPGRADES = [
  { key: 'blade', name: 'Lame affûtée', desc: '+3 ATQ par niveau', icon: 'long_sword3', base: 45, mult: 1.20 },
  { key: 'breath', name: 'Souffle ancien', desc: '+30 PV max par niveau', icon: 'i-heal-wounds', base: 40, mult: 1.18 },
  { key: 'swift', name: 'Pas vifs', desc: '+2,5 % VIT par niveau', icon: 'boots4_green', base: 70, mult: 1.24 }
];

const LOOT_TEMPLATES = [
  { name: 'Lame runique', slot: 'weapon', icon: 'long_sword3', stats: { atk: 12 } },
  { name: 'Hache de guerre', slot: 'weapon', icon: 'hand_axe2', stats: { atk: 10, crit: 2 } },
  { name: "Masse d'armes", slot: 'weapon', icon: 'mace2', stats: { atk: 9, crit: 3 } },
  { name: 'Lance du guet', slot: 'weapon', icon: 'spear2', stats: { atk: 11 } },
  { name: 'Cotte de mailles', slot: 'body', icon: 'scale_mail1', stats: { hp: 34, vit: 2 } },
  { name: 'Plate du gardien', slot: 'body', icon: 'plate1', stats: { hp: 52 } },
  { name: 'Heaume gravé', slot: 'head', icon: 'helmet3', stats: { hp: 22, crit: 3 } },
  { name: 'Gants de braise', slot: 'hands', icon: 'glove1', stats: { atk: 6 } },
  { name: 'Bottes de mousse', slot: 'feet', icon: 'boots4_green', stats: { vit: 5 } },
  { name: "Anneau d'âme", slot: 'talisman', icon: 'ring-regen', stats: { regen: 0.4, ferveur: 6 } }
];

const RARITY_LABEL = {
  common: 'COMMUN', uncommon: 'INHABITUEL', rare: 'RARE',
  epic: 'ÉPIQUE', legendary: 'LÉGENDAIRE'
};
const RARITY_VALUE = { common: 1, uncommon: 1.3, rare: 1.7, epic: 2.2, legendary: 3 };

const OATHS = [
  { id: 'steps', label: 'Marcher 3 000 pas', target: 3000, reward: '+1 éclat', icon: 'boots4_green' },
  { id: 'boss', label: 'Vaincre 1 gardien', target: 1, reward: '+150 or', icon: 'long_sword3' },
  { id: 'chest', label: 'Forcer 1 coffre', target: 1, reward: '+1 ration', icon: 'misc_box' }
];

const BIOMES = [
  { max: 10, name: 'CRYPTES DE VAEL', cls: 'crypt' },
  { max: 20, name: 'CATACOMBES', cls: 'catacomb' },
  { max: 30, name: 'CAVERNES DE CENDRE', cls: 'ember' },
  { max: 40, name: 'FORGE NOYÉE', cls: 'forge' },
  { max: 999, name: 'CIMES DU SERMENT', cls: 'peak' }
];

const SOURCE_META = {
  garmin: {
    label: 'Garmin',
    tag: 'GARMIN',
    state: 'Connecté · Personal API',
    capsule: 'clé personnelle · jeton 24 h'
  },
  health: {
    label: 'Santé Connect',
    tag: 'SANTÉ CONNECT',
    state: 'Autorisé · lecture des pas',
    capsule: 'Google Fit, Samsung Health, montre'
  }
};

/* ============================ ÉTAT ============================ */

const state = {
  floor: 3,
  distance: 66,
  totalDistance: 66,
  nextFight: 70,
  stepsToday: 3124,
  bank: 1240,
  unsynced: 1240,
  /* Source de pas : une seule active à la fois (comme dans l'application). */
  stepSource: 'garmin',
  historyStart: isoDay(-3),
  ledger: {},
  pendingDays: {},
  catchUpCap: 100000,
  campPending: 0,
  gold: 1240,
  rations: 9,
  shards: 7,
  genRations: 15,
  genShards: 6,
  ferveur: 62,
  level: 4,
  xp: 132,
  hp: 296,
  upgrades: { blade: 3, breath: 2, swift: 1 },
  equipment: {},
  inventory: [],
  mode: 'walk',
  fight: null,
  chest: null,
  farm: null,
  forcing: false,
  bossBlessing: 0,
  presageShown: false,
  bossEnragedShown: false,
  kills: 0,
  bosses: 0,
  chestsOpened: 0,
  retreats: 0,
  goldEarned: 0,
  journal: [],
  oathClaimed: { steps: true, boss: false, chest: false },
  lastSync: null,
  selectedItem: null,
  sheetEquipped: false,
  modalOpen: false,
  tab: 'stats'
};

/* ============================ HELPERS ============================ */

function mkItem(o) {
  return Object.assign({
    id: 'it' + Math.random().toString(36).slice(2, 8),
    rarity: 'common',
    level: 4,
    stats: {},
    value: 40,
    slotLabel: SLOT_LABEL[o.slot] || 'Objet'
  }, o);
}

function initEquipmentAndLoot() {
  state.equipment = {
    weapon: mkItem({ id: 'eq_weapon', name: 'Lame du veilleur', slot: 'weapon', rarity: 'rare', icon: 'long_sword3', level: 4, stats: { atk: 9, crit: 3 }, value: 64 }),
    head: mkItem({ id: 'eq_head', name: 'Heaume de fer', slot: 'head', rarity: 'uncommon', icon: 'helmet3', level: 3, stats: { hp: 18 }, value: 40 }),
    body: mkItem({ id: 'eq_body', name: 'Cotte de mailles', slot: 'body', rarity: 'rare', icon: 'scale_mail1', level: 4, stats: { hp: 36, vit: 4 }, value: 88 }),
    hands: mkItem({ id: 'eq_hands', name: 'Gantelets de braise', slot: 'hands', rarity: 'uncommon', icon: 'glove1', level: 3, stats: { atk: 5 }, value: 42 }),
    feet: mkItem({ id: 'eq_feet', name: 'Bottes de mousse', slot: 'feet', rarity: 'uncommon', icon: 'boots4_green', level: 3, stats: { vit: 4 }, value: 40 }),
    talisman: mkItem({ id: 'eq_talisman', name: 'Anneau de régénération', slot: 'talisman', rarity: 'rare', icon: 'ring-regen', level: 4, stats: { regen: 0.6, crit: 2 }, value: 76 })
  };

  state.inventory = [
    mkItem({ id: 'inv1', name: 'Épée ancienne', slot: 'weapon', rarity: 'epic', icon: 'greatsword1', level: 5, stats: { atk: 24, crit: 5 }, value: 140 }),
    mkItem({ id: 'inv2', name: 'Masse runique', slot: 'weapon', rarity: 'rare', icon: 'mace2', level: 4, stats: { atk: 16, crit: 2 }, value: 84 }),
    mkItem({ id: 'inv3', name: 'Hache de guerre', slot: 'weapon', rarity: 'uncommon', icon: 'hand_axe2', level: 3, stats: { atk: 12 }, value: 52 }),
    mkItem({ id: 'inv4', name: 'Lance du guet', slot: 'weapon', rarity: 'common', icon: 'spear2', level: 3, stats: { atk: 10 }, value: 36 }),
    mkItem({ id: 'inv5', name: 'Plate du gardien', slot: 'body', rarity: 'rare', icon: 'plate1', level: 4, stats: { hp: 60, vit: 2 }, value: 96 }),
    mkItem({ id: 'inv6', name: 'Cuirasse de cuir', slot: 'body', rarity: 'uncommon', icon: 'leather_armour1', level: 4, stats: { hp: 28, vit: 2 }, value: 48 }),
    mkItem({ id: 'inv7', name: 'Heaume elfique', slot: 'head', rarity: 'rare', icon: 'elven_leather_helm', level: 4, stats: { hp: 24, crit: 4 }, value: 80 }),
    mkItem({ id: 'inv8', name: "Amulette d'âme", slot: 'talisman', rarity: 'epic', icon: 'amulet-regen', level: 5, stats: { regen: 0.4, ferveur: 8 }, value: 160 })
  ];

  state.journal = [
    { type: 'good', text: 'Palier 2 scellé — Gardien vaincu', time: timeAgo(9) },
    { type: 'neutral', text: 'Coffre de fer repéré — serrure de 400 pas', time: timeAgo(7) },
    { type: 'bad', text: 'Repli de 20 m — farm de halte', time: timeAgo(6) }
  ];
  state.lastSync = Date.now() - 5 * 60 * 1000;
}

function getStats() {
  let atk = 22 + 3 * (state.level - 1) + 3 * state.upgrades.blade;
  let maxHp = 240 + 18 * (state.level - 1) + 30 * state.upgrades.breath;
  let vit = 1 + 0.01 * (state.level - 1) + 0.025 * state.upgrades.swift;
  let crit = 10;
  let regen = 0.6;

  Object.values(state.equipment).forEach(it => {
    if (!it) return;
    if (it.stats.atk) atk += it.stats.atk;
    if (it.stats.hp) maxHp += it.stats.hp;
    if (it.stats.vit) vit += it.stats.vit / 100;
    if (it.stats.crit) crit += it.stats.crit;
    if (it.stats.regen) regen += it.stats.regen;
  });

  return {
    atk: Math.round(atk),
    maxHp: Math.round(maxHp),
    vit,
    crit,
    regen
  };
}

function powerScore(s) {
  return Math.round(s.atk * 2 + s.maxHp / 4 + s.vit * 20 + s.crit * 1 + s.regen * 10);
}

function tierInfo(f) {
  if (f >= 100) return { n: 4, name: 'Âme ardente' };
  if (f >= 75) return { n: 3, name: 'Tempête' };
  if (f >= 50) return { n: 2, name: 'Fournaise' };
  if (f >= 25) return { n: 1, name: 'Braise' };
  return { n: 0, name: 'Tison' };
}

function upgradeCost(u, currentLevel) {
  return Math.ceil((u.base * Math.pow(u.mult, currentLevel - 1)) / 5) * 5;
}

function biomeFor(floor) {
  return BIOMES.find(b => floor <= b.max) || BIOMES[BIOMES.length - 1];
}

function updateFerveur() {
  state.ferveur = clamp(Math.floor(state.stepsToday / 50), 0, 100);
}

function addJournal(text, type = 'neutral') {
  state.journal.unshift({ text, type, time: clockNow() });
  state.journal = state.journal.slice(0, 10);
  renderJournal();
}

/* ============================ UI GÉNÉRALE ============================ */

let toastTimer = null;
function toast(msg, ms = 2400) {
  const el = $('#toast');
  el.innerHTML = msg;
  el.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove('show'), ms);
}

let bannerTimer = null;
function showBanner(title, sub, type = '', ms = 2300) {
  const b = $('#banner');
  $('#bannerTitle').textContent = title;
  $('#bannerSub').textContent = sub;
  b.className = 'banner show' + (type ? ' ' + type : '');
  clearTimeout(bannerTimer);
  bannerTimer = setTimeout(() => b.classList.remove('show'), ms);
}

function renderJournal() {
  const list = $('#journalList');
  list.innerHTML = state.journal.slice(0, 3).map(j => `
    <div class="jRow ${j.type}">
      <span class="jDot"></span>
      <span>${j.text}</span>
      <time>${j.time}</time>
    </div>`).join('');
}

function oathValue(id) {
  if (id === 'steps') return state.stepsToday;
  if (id === 'boss') return state.bosses;
  if (id === 'chest') return state.chestsOpened;
  return 0;
}

function renderOaths() {
  let doneCount = 0;
  const list = $('#oathList');
  if (!list) return;

  OATHS.forEach(o => {
    const v = oathValue(o.id);
    const done = v >= o.target;
    if (done) doneCount++;
    if (done && !state.oathClaimed[o.id]) {
      state.oathClaimed[o.id] = true;
      if (o.id === 'steps') {
        state.shards++;
        toast('Serment accompli · <b>+1 éclat d’âme</b>');
      } else if (o.id === 'boss') {
        state.gold += 150;
        toast('Serment accompli · <b>+150 or</b>');
      } else if (o.id === 'chest') {
        state.rations++;
        toast('Serment accompli · <b>+1 ration</b>');
      }
      addJournal('Serment accompli — ' + o.label, 'good');
      renderWallet();
      updateHud();
    }
  });

  list.innerHTML = OATHS.map(o => {
    const v = oathValue(o.id);
    const done = v >= o.target;
    const shown = o.id === 'steps' ? fmt(Math.min(v, o.target)) : Math.min(v, o.target);
    return `<div class="oathRow ${done ? 'done' : ''}">
      <span class="oathCheck">${done ? '✓' : ''}</span>
      <div class="oathBody">
        <b>${o.label}</b>
        <small>${shown} / ${fmt(o.target)}</small>
      </div>
      <em class="oathReward">${done ? 'ACCOMPLI' : o.reward}</em>
    </div>`;
  }).join('');

  $('#oathCount').textContent = `${doneCount} / 3`;
}

function renderWallet() {
  $('#goldTop').textContent = fmt(state.gold);
  $('#upGold').textContent = fmt(state.gold);
  $('#rationsTop').textContent = state.rations;
  $('#shardsTop').textContent = state.shards;
  $('#restCount').textContent = state.rations;
  $('#shardCount').textContent = state.shards;
}

function renderStats() {
  const s = getStats();
  const p = powerScore(s);
  const f = state.ferveur;
  const vitEff = s.vit * (1 + 0.004 * f);

  $('#powerVal').textContent = fmt(p);
  $('#invPower').textContent = fmt(p);
  $('#statAtk').textContent = s.atk;
  $('#statHp').textContent = s.maxHp;
  $('#statVit').textContent = vitEff.toFixed(2).replace('.', ',');

  $('#ferveurCardVal').textContent = f;
  $('#bonusVit').textContent = '+' + (0.4 * f).toFixed(1).replace('.', ',') + ' %';
  $('#bonusRegen').textContent = '+' + (0.8 * f).toFixed(1).replace('.', ',') + ' %';
  $('#bonusCrit').textContent = '+' + (0.02 * f).toFixed(1).replace('.', ',') + ' %';
  const t = tierInfo(f);
  $('#ferveurCardTier').textContent = t.name;
  $$('#segGauge .seg').forEach((seg, i) => {
    seg.classList.toggle('on', i < Math.ceil(f / 5));
  });

  $('#xpText').textContent = `${fmt(state.xp)} / ${fmt(xpNext())}`;
  $('#xpFill').style.width = clamp(state.xp / xpNext() * 100, 0, 100) + '%';
  $('#avatarLvl').textContent = pad2(state.level);
  $('.uLevel', $('#heroUnit')).textContent = 'NIV.' + pad2(state.level);
  $('#invCount').textContent = state.inventory.length;
  $('#invSubCount').textContent = `${state.inventory.length} objets`;
  renderOaths();
}

function xpNext() {
  return Math.round(120 * Math.pow(1.2, state.level - 1));
}

function renderSlots() {
  const wrap = $('#equipSlots');
  wrap.innerHTML = SLOT_ORDER.map(slot => {
    const it = state.equipment[slot];
    if (!it) {
      return `<button class="slot empty" data-slot="${slot}" aria-label="${SLOT_LABEL[slot]} vide"><span class="slotPlus">+</span><span class="slotName">${SLOT_LABEL[slot]}</span></button>`;
    }
    return `<button class="slot filled rarity-${it.rarity}" data-slot="${slot}" aria-label="${it.name}">
      <img src="assets/item/${it.icon}.png" alt="">
      <span class="slotKey">${SLOT_KEY[slot]}</span>
      <span class="slotName">${SLOT_LABEL[slot]}</span>
    </button>`;
  }).join('');
}

function renderInventory() {
  const grid = $('#invGrid');
  grid.innerHTML = state.inventory.map(it => `
    <button class="invItem rarity-${it.rarity}" data-item="${it.id}">
      <img src="assets/item/${it.icon}.png" alt="">
      <span>${it.name}</span>
      <em>${it.level}</em>
    </button>`).join('');
  renderSlots();
  renderStats();
  const sellBtn = $('#sellCommons');
  if (sellBtn) sellBtn.disabled = !state.inventory.some(it => it.rarity === 'common');
  const stash = $('#stashInfo');
  if (stash) stash.textContent = `${state.inventory.length} / 20`;
}

function renderUpgrades() {
  const list = $('#upList');
  list.innerHTML = UPGRADES.map(u => {
    const lvl = state.upgrades[u.key];
    const cost = upgradeCost(u, lvl + 1);
    const afford = state.gold >= cost;
    const dots = Array.from({ length: 10 }, (_, i) =>
      `<span class="upDot ${i < Math.min(lvl, 10) ? 'on' : ''}"></span>`).join('');
    return `<div class="upCard">
      <img src="assets/item/${u.icon}.png" alt="">
      <div class="upMid">
        <b>${u.name}</b>
        <small>${u.desc} · Niv. ${lvl}</small>
        <div class="upDots">${dots}</div>
      </div>
      <button class="buyBtn ${afford ? '' : 'disabled'}" data-up="${u.key}" ${afford ? '' : 'disabled'}>
        <b><img src="assets/item/01.png" alt="">${fmt(cost)}</b>
        <span>OR</span>
      </button>
    </div>`;
  }).join('');

  const boss = BOSS_POOL[(state.floor - 3 + BOSS_POOL.length) % BOSS_POOL.length];
  const nb = $('#nextBoss');
  if (nb) {
    nb.innerHTML = `
      <img src="assets/mon/${boss.file}" alt="">
      <div class="nbText">
        <span>PROCHAIN GARDIEN</span>
        <b>${boss.name}</b>
        <small>Palier ${pad2(state.floor)} · Niv. ${state.floor + 2}</small>
      </div>
      <em>6× PV<br>PHASE 2</em>`;
  }
}

function updateSegments() {
  const gauge = $('#segGauge');
  if (gauge.childElementCount) return;
  gauge.innerHTML = Array.from({ length: 20 }, () => '<span class="seg"></span>').join('');
}

function updateHud() {
  const s = getStats();
  state.hp = clamp(state.hp, 0, s.maxHp);

  $('#floorNum').textContent = pad2(state.floor);
  $('#biomeName').textContent = biomeFor(state.floor).name;
  $('#goldTop').textContent = fmt(state.gold);
  $('#stepsTop').textContent = fmt(state.stepsToday);
  $('#ferveurVal').textContent = state.ferveur;
  $('#ferveurFill').style.width = state.ferveur + '%';
  $('#ferveurTier').textContent = tierInfo(state.ferveur).name;
  $('#shardsTop').textContent = state.shards;
  $('#rationsTop').textContent = state.rations;
  $('#distLabel').textContent = `${Math.floor(state.distance)} / 100 m`;
  $('#progressFill').style.width = clamp(state.distance, 0, 100) + '%';
  $('#heroHp').style.width = clamp(state.hp / s.maxHp * 100, 0, 100) + '%';
  $('#statHp').textContent = s.maxHp;
  $('#statAtk').textContent = s.atk;

  const t = tierInfo(state.ferveur);
  const scene = $('#scene');
  [1, 2, 3, 4].forEach(n => scene.classList.toggle('tier-' + n, t.n >= n));

  $('#unsynced').textContent = fmt(state.unsynced);
  const badge = $('#syncBadge');
  if (state.unsynced >= 1) {
    badge.textContent = '+' + fmt(state.unsynced);
    $('.syncBtn').classList.remove('empty');
  } else {
    badge.textContent = 'À JOUR';
    $('.syncBtn').classList.add('empty');
  }

  if (state.fight) {
    const e = state.fight.enemy;
    $('#enemyHp').style.width = clamp(e.hp / e.maxHp * 100, 0, 100) + '%';
  }

  if (!$('#campOverlay').classList.contains('hidden')) {
    $('#campFloor').textContent = pad2(state.floor);
    $('#campDist').textContent = Math.floor(state.distance) + ' m';
    $('#campFerv').textContent = state.ferveur;
    $('#campPending').textContent = fmt(state.campPending);
    $('#restCount').textContent = state.rations;
    $('#shardCount').textContent = state.shards;
    $('#campSteps').textContent = fmt(state.stepsToday);
    $('#campGoldEarned').textContent = fmt(state.goldEarned);
    $('#campBossKills').textContent = state.bosses;
  }

  if (state.chest) {
    $('#chestProgress').style.width = clamp(state.chest.progress / state.chest.quota * 100, 0, 100) + '%';
    $('#chestCount').textContent = `${fmt(state.chest.progress)} / ${fmt(state.chest.quota)} pas`;
  }

  const chestVisible = state.chest && (state.mode === 'walk' || state.mode === 'chest' || state.mode === 'transition');
  $('#chestBtn').classList.toggle('show', !!chestVisible);

  updateFerveurClassesOnly();
}

function updateFerveurClassesOnly() {
  const aura = $('.ferveurAura');
  if (!aura) return;
  const t = tierInfo(state.ferveur);
  aura.style.opacity = t.n === 4 ? '.9' : t.n === 3 ? '.65' : t.n === 2 ? '.42' : t.n === 1 ? '.2' : '0';
}

function setWalking(on) {
  $('#scene').classList.toggle('walking', !!on);
}

function scrollScene() {
  const scene = $('#scene');
  scene.style.setProperty('--scroll', (state.totalDistance * 3.2) + 'px');
  scene.style.setProperty('--scrollFast', (state.totalDistance * 9) + 'px');
}

/* ============================ SYSTÈME DE PAS ============================ */

/* ============================ SOURCE & SYNCHRO ============================ */

function sourceMeta() {
  return SOURCE_META[state.stepSource];
}

function dayStatus(day) {
  const today = isoDay(0);
  if (day < state.historyStart) {
    return { label: 'AVANT CONNEXION', cls: 'out', steps: state.pendingDays[day] || 0 };
  }
  if (state.pendingDays[day]) {
    return { label: 'EN ATTENTE', cls: 'pending', steps: state.pendingDays[day] };
  }
  if (state.ledger[day]) {
    return { label: 'CRÉDITÉ', cls: 'done', steps: state.ledger[day] };
  }
  if (day === today) {
    return { label: 'EN COURS', cls: 'pending', steps: Math.round(state.unsynced) };
  }
  return { label: 'AUCUN PAS', cls: 'out', steps: 0 };
}

function renderDayList() {
  const list = $('#dayList');
  if (!list) return;
  const rows = [];
  for (let i = 0; i >= -6; i--) {
    const day = isoDay(i);
    const s = dayStatus(day);
    rows.push(`<div class="dayRow${i === 0 ? ' today' : ''}${s.cls === 'out' ? ' out' : ''}">
      <b>${i === 0 ? "Aujourd'hui" : dayLabel(day)}</b>
      <span class="daySteps">${s.steps > 0 ? fmt(s.steps) + ' pas' : '—'}</span>
      <span class="dayState ${s.cls}">${s.label}</span>
    </div>`);
  }
  list.innerHTML = rows.join('');
}

function renderSourceUi() {
  const meta = sourceMeta();
  $$('#sourceChips .sourceChip').forEach(chip => {
    const on = chip.dataset.source === state.stepSource;
    chip.classList.toggle('active', on);
    chip.setAttribute('aria-pressed', on ? 'true' : 'false');
  });
  const pending = Object.keys(state.pendingDays).length;
  const stateEl = $('#sourceState');
  if (stateEl) {
    stateEl.innerHTML = `<b>${meta.state}</b> · 1re connexion ${dayLabel(state.historyStart)}` +
      ` · ${pending ? pending + ' jour(s) à rattraper' : 'rattrapage ≤ 7 j'}`;
  }
  const tag = $('#syncSourceTag');
  if (tag) tag.textContent = meta.tag;
  const metaEl = $('#syncSourceMeta');
  if (metaEl) metaEl.textContent = meta.state;
  const startEl = $('#syncStart');
  if (startEl) startEl.textContent = dayLabelFull(state.historyStart);
  const sub = $('#syncSub');
  if (sub) sub.textContent = `${meta.label} · 1re fois : pas du jour · ensuite ≤ 7 j`;
  const sheetState = $('#syncSheetState');
  if (sheetState) sheetState.textContent = `${meta.label} · ${meta.capsule}`;
  const freeDays = [];
  for (let i = -1; i >= -6; i--) {
    const day = isoDay(i);
    if (day >= state.historyStart && !state.ledger[day] && !state.pendingDays[day]) freeDays.push(day);
  }
  const missed = $('#missedBtn');
  if (missed) {
    missed.disabled = freeDays.length === 0;
    missed.textContent = freeDays.length ? 'SIMULER UN OUBLI' : 'OUBLI RATTRAPÉ';
  }
  const sheet = $('#syncSheet');
  if (sheet && !sheet.classList.contains('hidden')) renderDayList();
}

function setStepSource(src) {
  if (!SOURCE_META[src] || src === state.stepSource) return;
  state.stepSource = src;
  renderSourceUi();
  updateHud();
  toast(`Source active : <b>${SOURCE_META[src].label}</b>. Le premier import de cette source sert de base : les pas déjà comptés ne sont pas recrédités.`);
  addJournal(`Source de pas : ${SOURCE_META[src].label}`, 'neutral');
}

function openSyncSheet() {
  renderSourceUi();
  renderDayList();
  $('#syncSheet').classList.remove('hidden');
  state.modalOpen = true;
}

function closeSyncSheet() {
  $('#syncSheet').classList.add('hidden');
  state.modalOpen = false;
}

function simulateMissedDays() {
  // Comme une lecture des 7 derniers jours : trois journées reçues, dont une
  // antérieure à la première connexion (elle sera ignorée à la synchronisation).
  const free = [];
  for (let i = -1; i >= -6 && free.length < 3; i--) {
    const day = isoDay(i);
    if (day >= state.historyStart && !state.ledger[day] && !state.pendingDays[day]) free.push(day);
  }
  if (!free.length) {
    toast('Tous les jours de la fenêtre sont déjà crédités. Rechargez la page pour rejouer la démo.');
    return;
  }
  free.forEach(day => { state.pendingDays[day] = randInt(3800, 9200); });
  const old = isoDay(-4);
  if (old < state.historyStart && !state.ledger[old] && !state.pendingDays[old]) {
    state.pendingDays[old] = randInt(3000, 7000);
  }
  renderSourceUi();
  renderDayList();
  const total = free.length + (state.pendingDays[old] ? 1 : 0);
  toast(`<b>Oubli simulé</b> · ${total} journées reçues, dont une avant la première connexion.`);
  addJournal(`Oubli de synchronisation simulé — ${total} journées reçues`, 'neutral');
}

function syncSteps() {
  const meta = sourceMeta();
  const today = isoDay(0);
  const minDay = isoDay(-6);
  const cap = state.catchUpCap;
  let used = 0, daysCredited = 0, stepsCredited = 0, daysIgnored = 0;

  // 1. Rattrapage : chaque jour de la fenêtre, une seule fois.
  Object.keys(state.pendingDays).sort().forEach(day => {
    const steps = state.pendingDays[day];
    if (day < state.historyStart || day < minDay || day > today) {
      if (day < state.historyStart) daysIgnored++;
      delete state.pendingDays[day];
      return;
    }
    const room = cap - used;
    if (room <= 0) return;
    const add = Math.min(steps, room);
    state.ledger[day] = add;
    stepsCredited += add;
    used += add;
    daysCredited++;
    delete state.pendingDays[day];
  });

  // 2. Les pas du jour, plafonnés par le reste de la synchronisation.
  const incoming = Math.round(state.unsynced);
  const amount = Math.min(incoming, Math.max(0, cap - used));
  state.unsynced = Math.max(0, incoming - amount);
  if (amount > 0) {
    state.stepsToday += amount;
    state.bank += amount;
    state.ledger[today] = (state.ledger[today] || 0) + amount;
  }

  if (daysCredited === 0 && amount === 0) {
    toast('Aucun nouveau pas pour le moment. Le compteur se remplit en marchant.');
    return;
  }

  const btn = $('#syncBtn');
  btn.classList.add('busy');

  const newGenR = Math.min(24, Math.floor(state.stepsToday / 200));
  const newGenS = Math.min(12, Math.floor(state.stepsToday / 500));
  const gainR = Math.max(0, newGenR - state.genRations);
  const gainS = Math.max(0, newGenS - state.genShards);
  state.genRations = newGenR;
  state.genShards = newGenS;
  state.rations = Math.min(24, state.rations + gainR);
  state.shards = Math.min(12, state.shards + gainS);

  state.lastSync = Date.now();
  updateFerveur();
  updateHud();
  renderOaths();
  renderWallet();
  renderSourceUi();

  const extras = [];
  if (gainR) extras.push(`+${gainR} ration${gainR > 1 ? 's' : ''}`);
  if (gainS) extras.push(`+${gainS} éclat${gainS > 1 ? 's' : ''}`);
  const parts = [];
  if (daysCredited) parts.push(`<b>${daysCredited} jour${daysCredited > 1 ? 's' : ''}</b> rattrapé${daysCredited > 1 ? 's' : ''} · +${fmt(stepsCredited)} pas`);
  if (amount) parts.push(`<b>${meta.label}</b> · +${fmt(amount)} pas`);
  if (daysIgnored) parts.push(`${daysIgnored} j avant connexion ignoré${daysIgnored > 1 ? 's' : ''}`);
  if (extras.length) parts.push(extras.join(' · '));
  toast(parts.join(' · '), 3200);

  if (daysCredited) addJournal(`${meta.label} · rattrapage ${daysCredited} jour${daysCredited > 1 ? 's' : ''} · +${fmt(stepsCredited)} pas`, 'good');
  if (amount) addJournal(`${meta.label} · +${fmt(amount)} pas aujourd'hui`, 'good');
  if (daysIgnored) addJournal(`${daysIgnored} jour${daysIgnored > 1 ? 's' : ''} avant la première connexion ignoré${daysIgnored > 1 ? 's' : ''}`, 'bad');

  setTimeout(() => {
    btn.classList.remove('busy');
    updateHud();
    updateSyncMeta();
    renderSourceUi();
  }, 760);
}

function updateSyncMeta() {
  const d = new Date(state.lastSync);
  $('#lastSync').textContent = `Dernière synchro · ${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
}

/* ============================ SCÈNE & COMBAT ============================ */

function showEnemy(enemy) {
  const unit = $('#enemyUnit');
  const img = $('#enemySprite');
  img.src = 'assets/mon/' + enemy.file;
  img.alt = enemy.name;
  $('#enemyName').textContent = enemy.name;
  $('#enemyLevel').textContent = 'NIV.' + pad2(enemy.level);
  $('#enemyHp').style.width = '100%';
  unit.classList.add('present');
  unit.classList.toggle('boss', !!enemy.isBoss);
  unit.classList.toggle('elite', !!enemy.elite);
  unit.classList.remove('enraged');
  unit.style.opacity = '';
}

function hideEnemy() {
  const unit = $('#enemyUnit');
  unit.classList.remove('present', 'boss', 'elite', 'enraged', 'attacking', 'hit');
}

function makeMob(farm = false) {
  const pos = state.distance;
  let pool = MOB_POOL.easy;
  if (pos >= 85) pool = MOB_POOL.hard;
  else if (pos >= 50) pool = MOB_POOL.mid;
  if (farm) pool = MOB_POOL.easy;

  const base = pool[randInt(0, pool.length - 1)];
  const elite = !farm && Math.random() < 0.1;
  let level = state.floor + (pos < 30 ? -1 : pos < 70 ? 0 : 1);
  if (farm) level = state.floor - 2;
  if (elite) level += 2;
  level = Math.max(1, level);

  const hp = (55 + 34 * (level - 1)) * (elite ? 2.2 : 1) * (farm ? 0.65 : 1);
  const atk = (6 + 2.2 * (level - 1)) * (elite ? 1.3 : 1) * (farm ? 0.6 : 1);
  const gold = (6 + 3 * level) * (elite ? 1.6 : 1) * (farm ? 0.5 : 1);
  const xp = (12 + 5 * level) * (elite ? 1.8 : 1) * (farm ? 0.5 : 1);

  return {
    name: base.name,
    file: base.file,
    level,
    maxHp: Math.round(hp),
    hp: Math.round(hp),
    atk,
    interval: elite ? 1100 : 1250,
    gold: Math.round(gold),
    xp: Math.round(xp),
    elite,
    isBoss: false,
    farm
  };
}

function makeBoss() {
  const b = BOSS_POOL[(state.floor - 3 + BOSS_POOL.length) % BOSS_POOL.length];
  const level = state.floor + 2;
  const hp = 6 * (55 + 34 * (level - 1));
  const atk = 1.6 * (6 + 2.2 * (level - 1));
  return {
    name: b.name,
    file: b.file,
    title: b.title,
    level,
    maxHp: Math.round(hp),
    hp: Math.round(hp),
    atk,
    interval: 1250,
    gold: Math.round(12 * (6 + 3 * level)),
    xp: Math.round(10 * (12 + 5 * level)),
    elite: false,
    isBoss: true,
    farm: false
  };
}

function startFight(isBoss = false) {
  if (state.mode === 'fight' || state.mode === 'boss') return;
  setWalking(false);
  const enemy = isBoss ? makeBoss() : makeMob();
  state.mode = isBoss ? 'boss' : 'fight';
  state.fight = {
    enemy,
    heroTimer: 380,
    enemyTimer: 900,
    elapsed: 0,
    specialTimer: 8000,
    usedRation: false,
    farm: false
  };
  state.bossEnragedShown = false;
  showEnemy(enemy);
  if (isBoss) {
    $('#gate').classList.add('active');
    $('#gateFrame').classList.add('active');
    showBanner('GARDIEN DU PALIER', `${enemy.name} · Niv. ${enemy.level}`, 'boss', 2600);
    addJournal(`${enemy.name} barre le passage du palier ${state.floor}`, 'bad');
  }
  updateHud();
}

function startFarmFight() {
  if (state.mode !== 'farmWait') return;
  const enemy = makeMob(true);
  state.mode = 'fight';
  state.fight = {
    enemy,
    heroTimer: 300,
    enemyTimer: 1000,
    elapsed: 0,
    specialTimer: 99999,
    usedRation: true,
    farm: true
  };
  showEnemy(enemy);
  updateHud();
}

function heroInterval() {
  const s = getStats();
  return 1000 / (s.vit * (1 + 0.004 * state.ferveur));
}

function heroAttack() {
  const f = state.fight;
  if (!f) return;
  const s = getStats();
  let atk = s.atk;
  if (f.enemy.isBoss && state.bossBlessing > 0) atk *= 1 + state.bossBlessing;
  let dmg = atk * rnd(0.9, 1.1);
  const critChance = (s.crit + state.ferveur * 0.02) / 100;
  const crit = Math.random() < critChance;
  if (crit) dmg *= 1.6;
  dmg = Math.max(1, Math.round(dmg));
  f.enemy.hp -= dmg;

  const hero = $('#heroUnit');
  const enemy = $('#enemyUnit');
  hero.classList.remove('attacking');
  void hero.offsetWidth;
  hero.classList.add('attacking');
  setTimeout(() => hero.classList.remove('attacking'), 300);

  spawnSlash();
  floatNumber(dmg, 'enemy', crit ? 'crit' : '');
  enemy.classList.add('hit');
  setTimeout(() => enemy.classList.remove('hit'), 110);

  if (f.enemy.hp <= 0) {
    f.enemy.hp = 0;
    finishFight(true);
  }
  updateHud();
}

function enemyAttack() {
  const f = state.fight;
  if (!f) return;
  let dmg = f.enemy.atk * rnd(0.9, 1.1);

  if (f.enemy.isBoss) {
    if (f.enemy.hp < f.enemy.maxHp * 0.5 && !state.bossEnragedShown) {
      state.bossEnragedShown = true;
      f.enemy.interval = 950;
      $('#enemyUnit').classList.add('enraged');
      showBanner('LE GARDIEN S’EMBRASE', 'Cadence accrue · frappe lourde', 'boss', 2000);
    }
    if (f.specialTimer <= 0) {
      dmg *= 1.8;
      f.specialTimer = 8000;
      showBanner('FRAPPE LOURDE', 'Le sol se fissure', 'boss', 1300);
    }
  }

  dmg = Math.max(1, Math.round(dmg));
  state.hp -= dmg;
  const enemy = $('#enemyUnit');
  const hero = $('#heroUnit');
  enemy.classList.remove('attacking');
  void enemy.offsetWidth;
  enemy.classList.add('attacking');
  setTimeout(() => enemy.classList.remove('attacking'), 280);

  floatNumber(dmg, 'hero', 'hurt');
  hero.classList.add('hit');
  setTimeout(() => hero.classList.remove('hit'), 120);

  if (state.hp <= 0) {
    if (f.farm) {
      state.hp = 1;
    } else {
      defeat();
      return;
    }
  }

  const s = getStats();
  if (!f.usedRation && state.hp < s.maxHp * 0.25 && state.rations > 0 && !f.farm) {
    f.usedRation = true;
    state.rations--;
    const heal = Math.round(s.maxHp * 0.35);
    state.hp = clamp(state.hp + heal, 0, s.maxHp);
    floatNumber('+' + heal, 'hero', 'heal');
    toast('Ration consommée automatiquement · <b>+' + heal + ' PV</b>');
    addJournal('Ration consommée en combat (+' + heal + ' PV)', 'neutral');
  }

  updateHud();
}

function finishFight(victory) {
  const f = state.fight;
  if (!f) return;
  state.fight = null;
  const e = f.enemy;

  if (!victory) {
    defeat();
    return;
  }

  state.kills++;
  state.gold += e.gold;
  state.goldEarned += e.gold;
  gainXp(e.xp);
  floatLoot(`+${fmt(e.gold)} OR`, 'enemy');
  addJournal(`${e.name} vaincu · +${fmt(e.gold)} or`, 'good');

  if (e.isBoss) {
    state.bosses++;
    state.shards += 2;
    state.rations += 1;
    state.bossBlessing = 0;
    const loot = makeLoot(state.floor, 'rare');
    state.inventory.push(loot);
    addJournal(`${e.name} vaincu · ${loot.name} obtenu`, 'good');
    renderInventory();
    toast(`<b>${e.name} vaincu !</b> +${fmt(e.gold)} or · +2 éclats · ${loot.name}`);
    setWalking(false);
    setTimeout(() => { hideEnemy(); }, 620);
    setTimeout(() => floorUp(), 1300);
    state.mode = 'transition';
  } else if (f.farm) {
    const s = getStats();
    state.hp = clamp(state.hp + s.maxHp * 0.08, 1, s.maxHp);
    state.farm.kills++;
    state.mode = 'farmWait';
    setTimeout(() => hideEnemy(), 380);
    if (state.farm.kills < 3) {
      setTimeout(() => startFarmFight(), 950);
    } else {
      setTimeout(() => endFarm(), 850);
    }
  } else {
    state.mode = 'transition';
    setTimeout(() => hideEnemy(), 520);
    setTimeout(() => {
      state.mode = 'walk';
      if (Math.random() < 0.18 && !state.chest) {
        spawnChest();
      }
      updateHud();
    }, 900);
  }
  updateHud();
}

function floorUp() {
  state.floor++;
  state.distance = 0;
  state.nextFight = 10;
  state.presageShown = false;
  state.bossEnragedShown = false;
  $('#gate').classList.remove('active');
  $('#gateFrame').classList.remove('active');
  hideEnemy();
  const b = biomeFor(state.floor);
  $('#scene').dataset.biome = b.cls;
  showBanner('PALIER ' + pad2(state.floor) + ' SCELLÉ', 'Le chemin s’ouvre vers ' + b.name.toLowerCase(), 'win', 2600);
  addJournal(`Palier ${state.floor - 1} scellé — nouveau palier ${state.floor}`, 'good');
  state.mode = 'walk';
  updateHud();
  renderStats();
  renderUpgrades();
}

function defeat() {
  if (state.mode === 'dead') return;
  const wasBoss = state.fight && state.fight.enemy.isBoss;
  const farm = state.fight && state.fight.farm;
  if (farm) {
    state.hp = 1;
    return;
  }
  state.mode = 'dead';
  state.fight = null;
  state.retreats++;
  const hero = $('#heroUnit');
  hero.style.transform = 'translateX(-42px)';
  $('#redFlash').classList.add('flash');
  setTimeout(() => $('#redFlash').classList.remove('flash'), 750);
  showBanner('VAINCU', 'Repli de 20 m — zone de farm', 'boss', 2400);
  addJournal('Défaite — repli de 20 m dans la halte', 'bad');
  setWalking(false);
  setTimeout(() => { hero.style.transform = ''; }, 700);
  setTimeout(() => { hideEnemy(); }, 450);

  if (wasBoss) {
    state.bossBlessing = 0.25;
    setTimeout(() => {
      toast('Le feu ravive votre lame : <b>+25 % ATQ</b> contre le gardien.');
    }, 1400);
    addJournal('Le feu de camp ravive la lame (+25 % ATQ contre le gardien)', 'neutral');
  }

  setTimeout(() => {
    state.distance = Math.max(0, Math.floor((state.distance - 20) / 10) * 10);
    state.nextFight = state.distance + 10;
    const s = getStats();
    state.hp = Math.round(s.maxHp * 0.6);
    startFarm();
    updateHud();
  }, 1150);
}

function startFarm() {
  state.mode = 'farmWait';
  state.farm = { kills: 0, startedAt: Date.now() };
  $('#farmTag').classList.add('show');
  setWalking(false);
  setTimeout(() => startFarmFight(), 750);
}

function endFarm() {
  state.mode = 'walk';
  state.farm = null;
  $('#farmTag').classList.remove('show');
  hideEnemy();
  state.nextFight = Math.floor(state.distance / 10) * 10 + 10;
  updateHud();
  toast('Fin du repli. Le sentier reprend.');
}

function gainXp(amount) {
  state.xp += amount;
  let leveled = false;
  let guard = 0;
  while (state.xp >= xpNext() && guard < 8) {
    state.xp -= xpNext();
    state.level++;
    leveled = true;
    guard++;
  }
  if (leveled) {
    const s = getStats();
    state.hp = clamp(state.hp + s.maxHp * 0.5, 0, s.maxHp);
    showBanner('NIVEAU ' + state.level, '+18 PV · +3 ATQ · +1 % VIT', 'win', 2200);
    toast('<b>Niveau ' + state.level + ' !</b> Votre puissance augmente.');
    addJournal('Niveau ' + state.level + ' atteint', 'good');
    renderStats();
  }
  renderStats();
}

/* ============================ FX ============================ */

function floatNumber(value, side, cls = '') {
  const el = document.createElement('span');
  el.className = 'dmgNum' + (cls ? ' ' + cls : '');
  el.textContent = value;
  el.style.left = side === 'enemy' ? '72%' : '23%';
  el.style.top = side === 'enemy' ? '61%' : '58%';
  $('#fxLayer').appendChild(el);
  setTimeout(() => el.remove(), 1000);
}

function floatLoot(text) {
  const el = document.createElement('span');
  el.className = 'lootNum';
  el.textContent = text;
  el.style.left = '71%';
  el.style.top = '57%';
  $('#fxLayer').appendChild(el);
  setTimeout(() => el.remove(), 1300);
}

function spawnSlash() {
  const s = document.createElement('span');
  s.className = 'slash';
  s.style.left = '64%';
  s.style.top = '50%';
  $('#fxLayer').appendChild(s);
  setTimeout(() => s.remove(), 340);
  for (let i = 0; i < 4; i++) {
    const p = document.createElement('span');
    p.className = 'sparkFx';
    p.style.left = (62 + rnd(-3, 5)) + '%';
    p.style.top = (48 + rnd(-4, 6)) + '%';
    p.style.setProperty('--dx', rnd(-14, 22) + 'px');
    p.style.setProperty('--dy', rnd(-26, -8) + 'px');
    $('#fxLayer').appendChild(p);
    setTimeout(() => p.remove(), 560);
  }
}

/* ============================ COFFRES ============================ */

function spawnChest(forceKey) {
  if (state.chest) return;
  let key = forceKey;
  if (!key) {
    const r = Math.random();
    key = r < 0.6 ? 'bois' : r < 0.9 ? 'fer' : 'runique';
  }
  const type = CHEST_TYPES[key];
  state.chest = {
    key,
    label: type.label,
    quota: type.quota,
    progress: 0,
    goldMin: type.goldMin,
    goldMax: type.goldMax,
    loot: type.loot,
    reward: type.reward
  };
  $('#chestBtn').classList.add('show');
  $('#chestRarity').textContent = type.label;
  $('#chestTitle').textContent = 'Serrure de ' + fmt(type.quota) + ' pas';
  $('#chestDesc').textContent = 'Consacrer vos pas fige la marche jusqu’à l’ouverture. La progression de la serrure est conservée.';
  $('#chestRewards').textContent = type.reward;
  $('#chestForce').textContent = 'CONSACRER MES PAS';
  updateChestUi();
}

function updateChestUi() {
  if (!state.chest) return;
  const c = state.chest;
  $('#chestProgress').style.width = clamp(c.progress / c.quota * 100, 0, 100) + '%';
  $('#chestCount').textContent = `${fmt(c.progress)} / ${fmt(c.quota)} pas`;
  if (state.forcing) {
    $('#chestForce').textContent = state.bank > 1 ? 'FORÇAGE EN COURS…' : 'EN ATTENTE DE PAS';
    $('#chestForce').classList.add('disabled');
  } else {
    $('#chestForce').textContent = c.progress > 0 ? 'REPRENDRE LE FORÇAGE' : 'CONSACRER MES PAS';
    $('#chestForce').classList.remove('disabled');
  }
}

function openChestSheet() {
  if (!state.chest) return;
  if (state.mode === 'fight' || state.mode === 'boss' || state.mode === 'dead') {
    toast('Terminez le combat avant de forcer un coffre.');
    return;
  }
  updateChestUi();
  $('#chestSheet').classList.remove('hidden');
  state.modalOpen = true;
}

function startForcing() {
  if (!state.chest || state.mode === 'camp' || state.mode === 'fight' || state.mode === 'boss') return;
  state.mode = 'chest';
  state.forcing = true;
  setWalking(false);
  updateChestUi();
}

function forceChest(dt) {
  const c = state.chest;
  if (!c) return;
  if (state.bank > 0.2) {
    const use = Math.min(state.bank, (55 * dt) / 1000, c.quota - c.progress);
    state.bank -= use;
    c.progress += use;
    if (c.progress >= c.quota) {
      completeChest();
      return;
    }
    updateChestUi();
  }
}

function completeChest() {
  const c = state.chest;
  if (!c) return;
  state.forcing = false;
  state.mode = 'walk';
  const gold = randInt(c.goldMin, c.goldMax);
  state.gold += gold;
  state.goldEarned += gold;
  state.rations += 1;
  state.shards += 1;
  const loot = makeLoot(state.floor, c.loot);
  state.inventory.push(loot);
  state.chestsOpened++;
  state.chest = null;
  $('#chestBtn').classList.remove('show');
  $('#chestSheet').classList.add('hidden');
  state.modalOpen = false;
  showBanner('COFFRE OUVERT', `${fmt(gold)} or · ${loot.name}`, 'win', 2400);
  toast(`<b>Serrure forcée !</b> +${fmt(gold)} or · +1 ration · +1 éclat · ${loot.name}`);
  addJournal(`Coffre ${c.label.toLowerCase()} ouvert · +${fmt(gold)} or`, 'good');
  addJournal(`Objet obtenu : ${loot.name}`, 'good');
  renderInventory();
  renderUpgrades();
  renderWallet();
  renderStats();
  updateHud();
}

function makeLoot(floor, rarity = 'rare') {
  const t = LOOT_TEMPLATES[randInt(0, LOOT_TEMPLATES.length - 1)];
  const mult = RARITY_VALUE[rarity] || 1;
  const stats = {};
  Object.keys(t.stats).forEach(k => {
    let v = t.stats[k] * (0.9 + Math.random() * 0.3) * mult;
    if (k === 'regen' || k === 'ferveur') v = Math.round(v * 10) / 10;
    else v = Math.max(1, Math.round(v));
    stats[k] = v;
  });
  return mkItem({
    name: t.name,
    slot: t.slot,
    icon: t.icon,
    rarity,
    level: Math.max(1, floor),
    stats,
    value: Math.round((12 + floor * 5) * mult)
  });
}

/* ============================ ITEMS / SHEET ============================ */

function statLine(stats, compact = false) {
  return Object.keys(stats).map(k => {
    const v = stats[k];
    const suffix = k === 'vit' ? ' % VIT' : k === 'crit' ? ' % crit.' : ' ' + STAT_LABEL[k];
    return '+' + String(v).replace('.', ',') + suffix;
  }).join(compact ? ' · ' : ' · ');
}

function openItem(id, equipped = false) {
  const item = equipped
    ? Object.values(state.equipment).find(it => it && it.id === id)
    : state.inventory.find(it => it.id === id);
  if (!item) return;

  state.selectedItem = item;
  state.sheetEquipped = equipped;

  $('#sheetIcon').src = 'assets/item/' + item.icon + '.png';
  $('#sheetIconWrap').className = 'sheetIcon rarity-' + item.rarity;
  $('#sheetName').textContent = item.name;
  $('#sheetRarity').textContent = RARITY_LABEL[item.rarity] || 'OBJET';
  $('#sheetRarity').style.color = 'var(--' + (item.rarity === 'common' ? 'common' : item.rarity) + ')';
  $('#sheetMeta').textContent = `${item.slotLabel} · Niv. ${item.level}`;

  const rows = Object.keys(item.stats).map(k => `
    <div class="sStat"><span>${STAT_LABEL[k]}</span><b>+${String(item.stats[k]).replace('.', ',')}</b></div>
  `).join('');
  $('#sheetStats').innerHTML = rows +
    `<div class="sStat"><span>Valeur de vente</span><b>${fmt(item.value)} or</b></div>`;

  const current = state.equipment[item.slot];
  const eqText = current ? statLine(current.stats, true) : 'Emplacement vide';
  $('#cmpEq').textContent = eqText;
  $('#cmpNew').textContent = statLine(item.stats, true);

  const keys = new Set([
    ...Object.keys(item.stats),
    ...(current ? Object.keys(current.stats) : [])
  ]);
  const deltas = [];
  keys.forEach(k => {
    const a = item.stats[k] || 0;
    const b = current ? (current.stats[k] || 0) : 0;
    const d = Math.round((a - b) * 10) / 10;
    if (d !== 0) {
      deltas.push(`<span class="delta ${d > 0 ? 'pos' : 'neg'}">${d > 0 ? '+' : ''}${String(d).replace('.', ',')} ${STAT_LABEL[k]}</span>`);
    }
  });
  if (equipped) {
    deltas.length = 0;
    deltas.push('<span class="delta neutral">Objet actuellement équipé</span>');
  }
  $('#sheetDeltas').innerHTML = deltas.length ? deltas.join('') : '<span class="delta neutral">Statistiques identiques</span>';

  const sell = $('#sellBtn');
  if (equipped) {
    sell.style.display = 'none';
    $('#equipBtn').textContent = 'RETIRER';
  } else {
    sell.style.display = '';
    $('#sellVal').textContent = '+' + fmt(item.value) + ' or';
    $('#equipBtn').textContent = 'ÉQUIPER';
  }

  $('#itemSheet').classList.remove('hidden');
  state.modalOpen = true;
}

function closeItemSheet() {
  $('#itemSheet').classList.add('hidden');
  state.selectedItem = null;
  state.modalOpen = false;
}

function equipSelected() {
  const item = state.selectedItem;
  if (!item) return;
  const oldMax = getStats().maxHp;

  if (state.sheetEquipped) {
    delete state.equipment[item.slot];
    state.inventory.push(item);
    toast(item.name + ' retiré et placé dans le butin.');
    addJournal(item.name + ' retiré', 'neutral');
  } else {
    const prev = state.equipment[item.slot];
    state.equipment[item.slot] = item;
    state.inventory = state.inventory.filter(it => it.id !== item.id);
    if (prev) state.inventory.push(prev);
    toast('<b>' + item.name + '</b> équipé.');
    addJournal(item.name + ' équipé', 'good');
  }

  const newMax = getStats().maxHp;
  state.hp = Math.round(state.hp * newMax / oldMax);
  state.hp = clamp(state.hp, 1, newMax);
  closeItemSheet();
  renderInventory();
  renderUpgrades();
  updateHud();
}

function sellSelected() {
  const item = state.selectedItem;
  if (!item || state.sheetEquipped) return;
  state.inventory = state.inventory.filter(it => it.id !== item.id);
  state.gold += item.value;
  state.goldEarned += item.value;
  toast(item.name + ' vendu · <b>+' + fmt(item.value) + ' or</b>');
  addJournal(item.name + ' vendu (+' + fmt(item.value) + ' or)', 'neutral');
  closeItemSheet();
  renderInventory();
  renderUpgrades();
  renderWallet();
  updateHud();
}

function sellCommons() {
  const commons = state.inventory.filter(it => it.rarity === 'common');
  if (!commons.length) {
    toast('Aucun objet commun à vendre dans le butin.');
    return;
  }
  const total = commons.reduce((sum, it) => sum + it.value, 0);
  state.inventory = state.inventory.filter(it => it.rarity !== 'common');
  state.gold += total;
  state.goldEarned += total;
  toast(`<b>${commons.length} objet${commons.length > 1 ? 's' : ''} commun${commons.length > 1 ? 's' : ''}</b> vendu${commons.length > 1 ? 's' : ''} · +${fmt(total)} or`);
  addJournal(`${commons.length} objet(s) commun(s) vendu(s) (+${fmt(total)} or)`, 'neutral');
  renderInventory();
  renderUpgrades();
  renderWallet();
  updateHud();
}

/* ============================ AMÉLIORATIONS ============================ */

function buyUpgrade(key) {
  const u = UPGRADES.find(x => x.key === key);
  if (!u) return;
  const lvl = state.upgrades[key];
  const cost = upgradeCost(u, lvl + 1);
  if (state.gold < cost) {
    toast('Il manque <b>' + fmt(cost - state.gold) + ' or</b> pour cette amélioration.');
    return;
  }
  state.gold -= cost;
  state.upgrades[key]++;
  const oldMax = getStats().maxHp;

  if (key === 'breath') {
    state.hp += 30;
  }
  const newMax = getStats().maxHp;
  if (key !== 'breath') state.hp = Math.round(state.hp * newMax / oldMax);
  state.hp = clamp(state.hp, 1, newMax);

  toast('<b>' + u.name + '</b> · niveau ' + state.upgrades[key] + ' atteint.');
  addJournal(u.name + ' amélioré au niveau ' + state.upgrades[key], 'good');
  renderUpgrades();
  renderStats();
  renderWallet();
  updateHud();
}

/* ============================ CAMP ============================ */

function openCamp() {
  if (state.mode === 'camp') return;
  $('#itemSheet').classList.add('hidden');
  $('#chestSheet').classList.add('hidden');
  state.modalOpen = false;

  if (state.mode === 'fight') {
    state.nextFight = state.distance;
    state.fight = null;
    hideEnemy();
  } else if (state.mode === 'boss') {
    state.fight = null;
    state.nextFight = 100;
    hideEnemy();
  } else if (state.mode === 'chest') {
    state.forcing = false;
    updateChestUi();
  }

  state.mode = 'camp';
  state.campPending = 0;
  setWalking(false);
  $('#campOverlay').classList.remove('hidden');
  const last = state.journal[0];
  $('#campLastEvent').textContent = 'Dernière braise · ' + (last ? last.text : 'Le feu attend vos pas');
  $('#campJournalList').innerHTML = state.journal.slice(0, 2).map(j => `
    <div class="campJRow ${j.type}">
      <span class="jDot"></span><span>${j.text}</span><time>${j.time}</time>
    </div>`).join('');
  $('#campSteps').textContent = fmt(state.stepsToday);
  $('#campGoldEarned').textContent = fmt(state.goldEarned);
  $('#campBossKills').textContent = state.bosses;
  updateHud();
}

function resumeCamp() {
  const pending = Math.round(state.campPending);
  state.stepsToday += pending;
  state.bank += pending;
  state.campPending = 0;
  updateFerveur();
  $('#campOverlay').classList.add('hidden');
  state.mode = 'walk';
  setWalking(true);
  updateHud();
  updateSyncMeta();
  if (pending > 0) {
    toast('<b>' + fmt(pending) + ' pas</b> accumulés au feu de camp.');
    addJournal(fmt(pending) + ' pas accumulés au campement', 'good');
  }
  if (state.distance >= 100) {
    state.mode = 'walk';
  }
}

function restAtCamp() {
  const s = getStats();
  if (state.rations < 1) {
    toast('Plus de rations. Une fraction de vos pas en génère de nouvelles.');
    return;
  }
  if (state.hp >= s.maxHp) {
    toast('Votre héros est déjà au sommet de sa forme.');
    return;
  }
  state.rations--;
  const heal = Math.round(s.maxHp * 0.4 * (1 + 0.002 * state.ferveur));
  state.hp = clamp(state.hp + heal, 0, s.maxHp);
  toast('Repos au feu · <b>+' + heal + ' PV</b>');
  addJournal('Repos au campement (+' + heal + ' PV)', 'good');
  updateHud();
  renderWallet();
}

function shardHeal() {
  const s = getStats();
  if (state.shards < 1) {
    toast('Aucun éclat d’âme disponible.');
    return;
  }
  if (state.hp >= s.maxHp) {
    toast('Votre héros est déjà au sommet de sa forme.');
    return;
  }
  state.shards--;
  state.hp = s.maxHp;
  toast('Éclat consumé · <b>PV complets</b>');
  addJournal('Éclat d’âme consommé — soin complet', 'good');
  updateHud();
  renderWallet();
}

/* ============================ BOUCLE ============================ */

let lastFrame = performance.now();
let demoAcc = 0;
let uiAcc = 0;
let campAcc = 0;
let firstPresage = false;

function loop(now) {
  const dt = Math.min(140, now - lastFrame);
  lastFrame = now;

  if (state.mode === 'walk') {
    if (!state.modalOpen) walk(dt);
  } else if (state.mode === 'fight' || state.mode === 'boss') {
    if (!state.modalOpen) fightTick(dt);
  } else if (state.mode === 'chest') {
    forceChest(dt);
    if (state.chest) updateChestUi();
  } else if (state.mode === 'farmWait') {
    setWalking(false);
  } else if (state.mode === 'camp') {
    campAcc += dt;
    state.campPending += dt * 0.0022;
  }

  if (state.mode !== 'camp' && state.mode !== 'dead') {
    demoAcc += dt;
    if (demoAcc >= 1000) {
      demoAcc -= 1000;
      state.stepsToday += 3;
      state.bank += 3;
      state.unsynced += 0.6;
      updateFerveur();
    }
  }

  regen(dt);

  uiAcc += dt;
  if (uiAcc >= 200) {
    uiAcc = 0;
    updateHud();
  }

  requestAnimationFrame(loop);
}

function walk(dt) {
  if (state.bank <= 0.4) {
    setWalking(false);
    return;
  }
  const rate = state.bank > 450 ? 26 : 13;
  const use = Math.min(state.bank, (rate * dt) / 1000);
  state.bank -= use;
  state.distance += use / 10;
  state.totalDistance += use / 10;
  setWalking(true);
  scrollScene();

  if (state.distance >= 88 && !state.presageShown) {
    state.presageShown = true;
    $('#gate').classList.add('active');
    $('#gateFrame').classList.add('active');
    showBanner('LE GARDIEN VEILLE', 'La porte runique scelle le palier', 'boss', 2300);
  }

  if (state.distance >= state.nextFight) {
    state.distance = Math.min(state.distance, state.nextFight);
    if (state.nextFight >= 100) {
      state.distance = 100;
      startFight(true);
    } else {
      startFight(false);
    }
  }
}

function fightTick(dt) {
  const f = state.fight;
  if (!f) return;
  f.elapsed += dt;
  f.specialTimer -= dt;

  f.heroTimer -= dt;
  if (f.heroTimer <= 0) {
    heroAttack();
    if (!state.fight) return;
    f.heroTimer = heroInterval();
  }

  const f2 = state.fight;
  if (!f2) return;
  f2.enemyTimer -= dt;
  if (f2.enemyTimer <= 0) {
    enemyAttack();
    if (!state.fight) return;
    f2.enemyTimer = f2.enemy.interval;
  }
}

function regen(dt) {
  if (state.hp <= 0) return;
  const s = getStats();
  if (state.hp >= s.maxHp) return;
  const inFight = state.mode === 'fight' || state.mode === 'boss';
  const rate = s.maxHp * 0.006 * (1 + 0.008 * state.ferveur) / 1000 * (inFight ? 1 / 3 : 1);
  state.hp = clamp(state.hp + rate * dt, 0, s.maxHp);
}

/* ============================ TABS & BINDINGS ============================ */

function switchTab(name) {
  state.tab = name;
  $$('.tab').forEach(t => {
    const active = t.dataset.tab === name;
    t.classList.toggle('active', active);
    t.setAttribute('aria-selected', active ? 'true' : 'false');
  });
  $$('.tabPanel').forEach(p => p.classList.toggle('active', p.id === 'panel-' + name));
  $('#panelScroll').scrollTop = 0;
}

function buildCampStars() {
  const wrap = $('#campStars');
  let html = '';
  for (let i = 0; i < 34; i++) {
    const size = Math.random() < 0.22 ? 2 : 1.4;
    html += `<span class="star" style="left:${rnd(2, 98)}%;top:${rnd(4, 62)}%;width:${size}px;height:${size}px;animation-delay:${rnd(0, 3).toFixed(1)}s"></span>`;
  }
  wrap.innerHTML = html;
}

function buildMotes() {
  const scene = $('#scene');
  for (let i = 0; i < 14; i++) {
    const m = document.createElement('span');
    m.className = 'mote';
    const size = rnd(1.4, 2.8);
    m.style.left = rnd(3, 96) + '%';
    m.style.top = rnd(34, 82) + '%';
    m.style.width = size + 'px';
    m.style.height = size + 'px';
    m.style.animationDuration = rnd(7, 15).toFixed(1) + 's';
    m.style.animationDelay = (-rnd(0, 12)).toFixed(1) + 's';
    scene.appendChild(m);
  }
}

function bind() {
  $$('.tab').forEach(t => t.addEventListener('click', () => switchTab(t.dataset.tab)));

  $('#syncBtn').addEventListener('click', syncSteps);

  $('#sourceChips').addEventListener('click', e => {
    const chip = e.target.closest('.sourceChip');
    if (chip) setStepSource(chip.dataset.source);
  });
  $('#windowBtn').addEventListener('click', openSyncSheet);
  $('#syncSheetBtn').addEventListener('click', syncSteps);
  $('#missedBtn').addEventListener('click', simulateMissedDays);
  $$('[data-close="sync"]').forEach(el => el.addEventListener('click', closeSyncSheet));

  $('#campBtn').addEventListener('click', openCamp);
  $('#campClose').addEventListener('click', resumeCamp);
  $('#resumeBtn').addEventListener('click', resumeCamp);
  $('#restBtn').addEventListener('click', restAtCamp);
  $('#shardBtn').addEventListener('click', shardHeal);
  $('#campInvBtn').addEventListener('click', () => {
    resumeCamp();
    switchTab('inventory');
    toast('Le feu reste allumé. Comparez et équipez votre butin.');
  });

  $('#chestBtn').addEventListener('click', openChestSheet);
  $('#chestForce').addEventListener('click', () => {
    if (!state.chest) return;
    if (state.forcing) return;
    if (state.chest.progress >= state.chest.quota) {
      completeChest();
      return;
    }
    startForcing();
  });

  $('#chestBtn').addEventListener('mouseenter', () => {
    if (state.chest) updateChestUi();
  });

  $('#equipBtn').addEventListener('click', equipSelected);
  $('#sellBtn').addEventListener('click', sellSelected);
  $('#sellCommons').addEventListener('click', sellCommons);

  $('#invGrid').addEventListener('click', e => {
    const btn = e.target.closest('.invItem');
    if (btn) openItem(btn.dataset.item, false);
  });

  $('#equipSlots').addEventListener('click', e => {
    const btn = e.target.closest('.slot.filled');
    if (!btn) return;
    const item = state.equipment[btn.dataset.slot];
    if (item) openItem(item.id, true);
  });

  $('#upList').addEventListener('click', e => {
    const btn = e.target.closest('.buyBtn');
    if (!btn || btn.disabled) return;
    buyUpgrade(btn.dataset.up);
  });

  $$('[data-close="item"]').forEach(el => el.addEventListener('click', closeItemSheet));
  $$('[data-close="chest"]').forEach(el => el.addEventListener('click', () => {
    if (state.forcing) {
      state.forcing = false;
      state.mode = 'walk';
      updateChestUi();
      toast('Forçage suspendu — progression de la serrure conservée.');
    }
    $('#chestSheet').classList.add('hidden');
    state.modalOpen = false;
  }));

  document.addEventListener('keydown', e => {
    if (e.key !== 'Escape') return;
    if (!$('#campOverlay').classList.contains('hidden')) {
      resumeCamp();
    } else if (!$('#itemSheet').classList.contains('hidden')) {
      closeItemSheet();
    } else if (!$('#chestSheet').classList.contains('hidden')) {
      $('#chestSheet').classList.add('hidden');
      state.modalOpen = false;
    } else if (!$('#syncSheet').classList.contains('hidden')) {
      closeSyncSheet();
    }
  });

  // Hover de comparaison rapide sur les objets
  $('#invGrid').addEventListener('mouseover', e => {
    const btn = e.target.closest('.invItem');
    if (!btn) return;
    const item = state.inventory.find(it => it.id === btn.dataset.item);
    if (item) btn.title = `${item.name} — ${statLine(item.stats, true)}`;
  });
}

/* ============================ INIT ============================ */

function init() {
  initEquipmentAndLoot();
  buildCampStars();
  buildMotes();
  updateSegments();
  bind();

  state.hp = 296;
  const b = biomeFor(state.floor);
  $('#scene').dataset.biome = b.cls;

  // Coffre de démonstration
  spawnChest('fer');
  state.chest.progress = 0;

  updateFerveur();
  updateSyncMeta();
  renderJournal();
  renderWallet();
  renderInventory();
  renderUpgrades();
  renderStats();
  renderSourceUi();
  updateHud();

  // Horloge réelle de la barre d'état
  $('.clock').textContent = clockNow();
  setInterval(() => { $('.clock').textContent = clockNow(); }, 20000);
  scrollScene();
  setWalking(true);

  applyHash();
  requestAnimationFrame(loop);
}

function applyHash() {
  const h = location.hash.replace('#', '');
  if (!h) return;
  setTimeout(() => {
    if (h === 'camp') {
      openCamp();
    } else if (h === 'inventory') {
      switchTab('inventory');
    } else if (h === 'upgrades') {
      switchTab('upgrades');
    } else if (h === 'chest') {
      openChestSheet();
    } else if (h === 'sync') {
      openSyncSheet();
    } else if (h === 'sheet') {
      openItem(state.inventory[0].id, false);
    } else if (h === 'fight') {
      state.distance = 70;
      state.nextFight = 70;
      startFight(false);
    } else if (h === 'boss') {
      state.distance = 100;
      state.nextFight = 100;
      state.totalDistance = 300;
      $('#gate').classList.add('active');
      $('#gateFrame').classList.add('active');
      startFight(true);
    } else if (h === 'farm') {
      state.distance = 80;
      state.nextFight = 90;
      state.hp = Math.round(getStats().maxHp * 0.6);
      startFarm();
    }
  }, 350);
}

document.addEventListener('DOMContentLoaded', init);



