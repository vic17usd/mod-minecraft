// Arte original reproducible: PNG RGBA, audio sintetizado OGG y plantillas NBT sin dependencias npm.
// Se puede ejecutar solo o al final de generate-solar-system.mjs. FFmpeg solo se necesita para regenerar audio.
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const res = path.join(root, 'src/main/resources');
const assets = 'assets/galaxycraft', data = 'data/galaxycraft';
const bodies = JSON.parse(fs.readFileSync(path.join(res, 'solar_system.json')));
const read = file => JSON.parse(fs.readFileSync(path.join(res, file)));
function binary(file, bytes) { const target = path.join(res, file); fs.mkdirSync(path.dirname(target), { recursive: true }); fs.writeFileSync(target, bytes); }
function json(file, value) { binary(file, JSON.stringify(value, null, 2) + '\n'); }
const id = name => `galaxycraft:${name}`;
let seed = 281019;
const rng = () => { seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0; return seed / 4294967296; };
const mix = (color, amount) => color.map(v => Math.max(0, Math.min(255, Math.round(v + amount))));
const rgb = n => [n >> 16 & 255, n >> 8 & 255, n & 255];
// CRC de PNG, independiente de bibliotecas gráficas o imágenes de Minecraft.
function crc32(bytes) { let c = 0xFFFFFFFF; for (const b of bytes) { c ^= b; for (let i = 0; i < 8; i++) c = (c >>> 1) ^ (c & 1 ? 0xEDB88320 : 0); } return (c ^ 0xFFFFFFFF) >>> 0; }
function chunk(type, bytes) { const name = Buffer.from(type), size = Buffer.alloc(4), crc = Buffer.alloc(4); size.writeUInt32BE(bytes.length); crc.writeUInt32BE(crc32(Buffer.concat([name, bytes]))); return Buffer.concat([size, name, bytes, crc]); }
function png(file, width, height, pixel) {
    const rows = Buffer.alloc((width * 4 + 1) * height);
    for (let y = 0; y < height; y++) for (let x = 0; x < width; x++) {
        const color = pixel(x, y); const offset = y * (width * 4 + 1) + 1 + x * 4;
        rows.set(color.length === 3 ? [...color, 255] : color, offset);
    }
    const header = Buffer.alloc(13); header.writeUInt32BE(width); header.writeUInt32BE(height, 4); header[8] = 8; header[9] = 6;
    binary(`${assets}/textures/${file}.png`, Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]), chunk('IHDR', header), chunk('IDAT', zlib.deflateSync(rows)), chunk('IEND', Buffer.alloc(0))]));
}
const palettes = { moon: [130,139,156], mars: [156,69,49], mercury: [88,83,100], venus: [164,139,61],
    jupiter: [154,111,77], saturn: [182,159,102], uranus: [74,151,161], neptune: [51,77,163] };
for (const [i, body] of bodies.entries()) {
    if (body.id === 'earth') continue;
    const color = palettes[body.id] ?? mix(palettes[body.parent] ?? [95,120,139], (i % 5 - 2) * 12);
    const gem = body.id === 'venus' ? [255,218,67] : body.id === 'mars' ? [80,227,181] : mix(rgb((body.color ^ (0x85B3CE + i * 172933)) & 0xFFFFFF), 65);
    const clusters = Array.from({ length: 9 }, () => [Math.floor(rng() * 16), Math.floor(rng() * 16)]);
    for (const suffix of ['stone', 'regolith', 'ore']) {
        const name = `${body.id}_${suffix}`;
        png(`block/${name}`, 16, 16, (x, y) => {
            const delta = (rng() - .5) * (suffix === 'regolith' ? 15 : 32);
            if (suffix === 'ore' && clusters.some(([cx, cy]) => Math.abs(x - cx) + Math.abs(y - cy) < 2)) return mix(gem, (x + y) % 3 * 15 - 15);
            const crater = suffix === 'regolith' && ((x - 6) ** 2 + (y - 9) ** 2 < 12);
            return mix(color, delta + (suffix === 'regolith' ? 14 : 0) - (crater ? 25 : 0) - ((x + y * 3) % 19 === 0 ? 20 : 0));
        });
        json(`${assets}/models/block/${name}.json`, { parent: 'minecraft:block/cube_all', textures: { all: id(`block/${name}`) } });
    }
    // Cristal asimétrico con arista luminosa: cada recurso tiene color propio.
    png(`item/${body.resource}`, 32, 32, (x, y) => {
        const xx = x - 15, yy = y - 16;
        if (Math.abs(xx + yy * .18) / 9 + Math.abs(yy) / 14 > 1) return [0,0,0,0];
        return mix(gem, xx < -2 ? -55 : xx < 1 ? 55 : (rng() - .5) * 20);
    });
    json(`${assets}/models/item/${body.resource}.json`, { parent: 'minecraft:item/generated', textures: { layer0: id(`item/${body.resource}`) } });
    const biome = read(`${data}/worldgen/biome/${body.id}.json`);
    biome.effects.music = { sound: id('space_ambient'), min_delay: 600, max_delay: 1800, replace_current_music: false };
    json(`${data}/worldgen/biome/${body.id}.json`, biome);
    if (body.mob) png(`entity/${body.id}_crawler`, 64, 32, (x, y) => (x + y) % 13 < 2 ? gem : mix(color, (rng() - .5) * 30));
}
// Metal cepillado, paneles y balizas de fabricación propia.
const metal = [174,191,210], dark = [31,41,60], cyan = [65,211,236];
for (const [name, color] of Object.entries({ hull: metal, engine: dark, window: cyan,
    nose1: [213,73,68], nose2: [55,124,213], nose3: [153,80,211], launch_pad_side: dark, launch_pad_top: metal })) {
    png(`block/${name}`, 16, 16, (x, y) => {
        if (name === 'launch_pad_top' && ((x + y) % 8 < 3) && (x < 3 || x > 12 || y < 3 || y > 12)) return [231,173,40];
        if (name === 'window') return mix(color, (x < 4 || y < 3) ? 40 : -15);
        if (x === 0 || y === 0 || x === 15 || y === 15) return mix(color, -40);
        if ((x === 2 || x === 13) && (y === 2 || y === 13)) return mix(color, -65);
        return mix(color, (rng() - .5) * 16 + (y % 4 === 0 ? 5 : 0));
    });
}
png('block/titanium_ore', 16, 16, (x, y) => ((x * 3 + y * 7) % 19 < 4) ? mix([127,207,221], (x % 3) * 15) : mix([88,99,119], (rng() - .5) * 32));
json(`${assets}/models/block/titanium_ore.json`, { parent: 'minecraft:block/cube_all', textures: { all: id('block/titanium_ore') } });
json(`${assets}/models/block/launch_pad.json`, { parent: 'minecraft:block/cube_bottom_top', textures: { top: id('block/launch_pad_top'), bottom: id('block/engine'), side: id('block/launch_pad_side') } });
const rocket = read(`${assets}/models/block/tier1_rocket.json`);
rocket.textures = { particle: id('block/hull'), body: id('block/hull'), nose: id('block/nose1'), engine: id('block/engine'), window: id('block/window') };
json(`${assets}/models/block/tier1_rocket.json`, rocket);
for (const tier of [2, 3]) json(`${assets}/models/block/tier${tier}_rocket.json`, { parent: id('block/tier1_rocket'), textures: { nose: id(`block/nose${tier}`) } });
// Iconos dibujados sobre lienzo transparente; no se reutilizan sprites vanilla.
const transparent = [0,0,0,0];
for (const name of ['oxygen_tank', 'space_helmet', 'galaxy_emblem', 'thruster', 'navigation_core', 'lunar_schematic', 'martian_schematic']) {
    png(`item/${name}`, 32, 32, (x, y) => {
        if (name === 'galaxy_emblem') {
            const distance = Math.hypot(x - 15.5, y - 15.5);
            if (distance < 8) return mix([64,111,204], (x + y) % 7 * 9);
            if (Math.abs((y - 16) + (x - 16) * .45) < 1.5 && distance < 15) return [146,225,245];
            return x === 26 && y < 6 ? [255,240,176] : transparent;
        }
        if (name.includes('schematic')) {
            if (x < 5 || x > 26 || y < 3 || y > 28) return transparent;
            if (x === 5 || x === 26 || y === 3 || y === 28) return metal;
            return (x % 5 === 0 || y % 5 === 0) ? [88,180,222] : (Math.abs(x - 16) < 3 && y > 8 && y < 23 ? cyan : [26,66,118]);
        }
        if (name === 'oxygen_tank') {
            if (x >= 13 && x <= 18 && y >= 2 && y <= 5) return dark;
            if (x < 8 || x > 23 || y < 6 || y > 28 || (y === 6 && (x < 10 || x > 21))) return transparent;
            if (y > 13 && y < 19) return [48,143,197];
            return x < 11 ? [225,239,247] : x > 21 ? [87,109,139] : metal;
        }
        if (name === 'space_helmet') {
            if (Math.hypot((x - 16) / 1.1, y - 16) > 13 || y > 28) return transparent;
            if (y > 11 && y < 22 && x > 7 && x < 25) return mix([47,159,201], x < 12 ? 55 : -12);
            return mix(metal, x < 10 ? 35 : -15);
        }
        if (name === 'thruster') {
            if (y < 4 || y > 28 || Math.abs(x - 16) > (y < 16 ? 7 : 11)) return transparent;
            if (y > 24) return [243,169,59];
            return (x % 5 < 2 || y === 15) ? dark : metal;
        }
        if (Math.abs(x - 16) + Math.abs(y - 16) > 14) return transparent;
        return Math.abs(x - 16) + Math.abs(y - 16) < 6 ? cyan : x % 4 === 0 || y % 4 === 0 ? [201,165,58] : dark;
    });
    json(`${assets}/models/item/${name}.json`, { parent: 'minecraft:item/generated', textures: { layer0: id(`item/${name}`) } });
}
// UV 64x32 estándar del modelo de armadura 1.21; visor en la cara frontal de la cabeza.
for (const layer of [1, 2]) png(`models/armor/space_suit_layer_${layer}`, 64, 32, (x, y) => {
    if (x >= 8 && x < 16 && y >= 10 && y < 14) return mix([48,162,206], x < 11 ? 40 : 0);
    return y % 8 === 0 || x % 8 === 0 ? dark : mix(metal, (x + y) % 3 * 5);
});
for (const name of ['rocket_exhaust', 'rocket_smoke', 'lunar_dust']) {
    png(`particle/${name}`, 16, 16, (x, y) => {
        const r = Math.hypot(x - 7.5, y - 7.5) / 8;
        const c = name === 'rocket_exhaust' ? (r < .4 ? [255,246,187] : [255,139,38]) : name === 'rocket_smoke' ? [105,116,137] : [170,186,207];
        return [...c, Math.round(Math.max(0, 1 - r) * 230)];
    });
    json(`${assets}/particles/${name}.json`, { textures: [id(name)] });
}
// Audio mono Vorbis original: ruido filtrado y acordes suaves, sin muestras externas.
for (const [name, duration] of [['rocket_launch', 2.8], ['suit_depressurize', 1.2], ['space_ambient', 24]]) {
    const file = `${assets}/sounds/${name}.ogg`;
    if (!fs.existsSync(path.join(res, file)) || process.argv.includes('--audio')) {
        const rate = 22050, pcm = Buffer.alloc(Math.floor(rate * duration) * 2); let smooth = 0;
        for (let i = 0; i < pcm.length / 2; i++) {
            const t = i / rate, envelope = Math.min(1, t / .25) * Math.min(1, (duration - t) / .6);
            smooth = smooth * .86 + (rng() * 2 - 1) * .14;
            let sample = name === 'space_ambient'
                ? ([110,164.81,220,277.18].reduce((sum, f) => sum + Math.sin(t * f * Math.PI * 2) * .045, 0) * (.7 + .3 * Math.sin(t * .3)))
                : name === 'rocket_launch' ? smooth * 1.5 + Math.sin(t * (48 - t * 5) * Math.PI * 2) * .17
                : (rng() * 2 - 1) * .2 * (1 - t / duration);
            pcm.writeInt16LE(Math.round(Math.max(-1, Math.min(1, sample * envelope)) * 32767), i * 2);
        }
        fs.mkdirSync(path.dirname(path.join(res, file)), { recursive: true });
        execFileSync('ffmpeg', ['-y', '-hide_banner', '-loglevel', 'error', '-f', 's16le', '-ar', String(rate), '-ac', '1', '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '3', path.join(res, file)], { input: pcm });
    }
}
json(`${assets}/sounds.json`, Object.fromEntries(['rocket_launch', 'suit_depressurize', 'space_ambient'].map(name => [name, {
    subtitle: `subtitles.galaxycraft.${name}`, sounds: [{ name: id(name), ...(name === 'space_ambient' ? { stream: true } : {}) }]
}])));

// Escritor NBT mínimo tipado: listas homogéneas y compuestos, con compresión gzip requerida por plantillas.
const int = value => ({ type: 3, value }), str = value => ({ type: 8, value });
const list = (type, value) => ({ type: 9, value, subtype: type }), compound = value => ({ type: 10, value });
const intList = values => list(3, values.map(value => int(value)));
function short(value) { const b = Buffer.alloc(2); b.writeUInt16BE(value); return b; }
function integer(value) { const b = Buffer.alloc(4); b.writeInt32BE(value); return b; }
function string(value) { const b = Buffer.from(value); return Buffer.concat([short(b.length), b]); }
function payload(tag) {
    if (tag.type === 3) return integer(tag.value);
    if (tag.type === 8) return string(tag.value);
    if (tag.type === 9) return Buffer.concat([Buffer.from([tag.subtype]), integer(tag.value.length), ...tag.value.map(payload)]);
    if (tag.type === 10) return Buffer.concat([...Object.entries(tag.value).map(([name, child]) => Buffer.concat([Buffer.from([child.type]), string(name), payload(child)])), Buffer.from([0])]);
    throw new Error('Tipo NBT no soportado');
}
for (const [index, [name, biomes, material, resource, scheme]] of [
    ['lunar_research_module', ['moon'], 'moon_stone', 'lunar_crystal', 'lunar_schematic'],
    ['martian_alien_ruins', ['mars'], 'mars_stone', 'martian_metal', 'martian_schematic'],
    ['orbital_satellite', ['moon', 'mars'], 'launch_pad', 'mercurian_cobalt', 'martian_schematic']
].entries()) {
    const orbit = name === 'orbital_satellite', ruins = index === 1;
    const sx = orbit ? 13 : 9, sy = 6, sz = 9;
    const palette = [compound({ Name: str(id(material)) }), compound({ Name: str('minecraft:air') }),
        compound({ Name: str('minecraft:chest'), Properties: compound({ facing: str('south'), type: str('single'), waterlogged: str('false') }) }),
        compound({ Name: str('minecraft:sea_lantern') }), compound({ Name: str(id('titanium_ore')) })];
    const blocks = [];
    for (let x = 0; x < sx; x++) for (let z = 0; z < sz; z++) for (let y = 0; y < sy; y++) {
        // Planta central con puerta doble; ruinas abiertas y paneles solares a ambos lados del satélite.
        let state = 1;
        if (orbit) {
            if (x >= 4 && x <= 8 && z >= 2 && z <= 6 && (y === 0 || y === 4 || x === 4 || x === 8 || z === 2 || z === 6)) state = 0;
            if ((x < 4 || x > 8) && z >= 2 && z <= 6 && y === 1) state = 0;
            if (x === 6 && z === 6 && y >= 1 && y <= 2) state = 1;
        } else {
            if (y === 0 || (!ruins && y === 4) || (y < 4 && (x === 0 || x === sx - 1 || z === 0 || z === sz - 1))) state = 0;
            if (ruins && y > 1 && ((x + z * 3 + y) % 4 === 0 || y > 3)) state = 1;
            if ((x === 4 || x === 5) && z === sz - 1 && y >= 1 && y <= 2) state = 1;
        }
        if (y === 3 && x === Math.floor(sx / 2) && z === 4) state = 3;
        const chest = y === 1 && x === Math.floor(sx / 2) && z === 3;
        if (chest) state = 2;
        const block = { pos: intList([x,y,z]), state: int(state) };
        if (chest) block.nbt = compound({ id: str('minecraft:chest'), LootTable: str(id(`chests/${name}`)) });
        blocks.push(compound(block));
    }
    const template = compound({ DataVersion: int(3953), size: intList([sx,sy,sz]), palette: list(10, palette), blocks: list(10, blocks), entities: list(10, []) });
    binary(`${data}/structure/${name}.nbt`, zlib.gzipSync(Buffer.concat([Buffer.from([10,0,0]), payload(template)])));
    json(`${data}/tags/worldgen/biome/has_structure/${name}.json`, { replace: false, values: biomes.map(id) });
    json(`${data}/worldgen/template_pool/${name}.json`, { name: id(name), fallback: 'minecraft:empty', elements: [{ weight: 1, element: {
        element_type: 'minecraft:single_pool_element', location: id(name), processors: orbit ? 'minecraft:empty' : id(name), projection: 'rigid'
    } }] });
    // Modifica únicamente el material de la estructura; nunca toca cofres ni terreno ajeno.
    if (!orbit) json(`${data}/worldgen/processor_list/${name}.json`, { processors: [{ processor_type: 'minecraft:rule', rules: [{
        input_predicate: { predicate_type: 'minecraft:random_block_match', block: id(material), probability: .08 },
        location_predicate: { predicate_type: 'minecraft:always_true' },
        output_state: { Name: id(ruins ? 'mars_regolith' : 'moon_regolith') }
    }] }] });
    json(`${data}/worldgen/structure/${name}.json`, { type: 'minecraft:jigsaw', biomes: `#galaxycraft:has_structure/${name}`,
        step: 'surface_structures', spawn_overrides: {}, terrain_adaptation: orbit ? 'none' : 'beard_thin', start_pool: id(name), size: 1,
        start_height: { absolute: orbit ? 200 : 0 }, ...(orbit ? {} : { project_start_to_heightmap: 'WORLD_SURFACE_WG' }),
        use_expansion_hack: false, max_distance_from_center: 32 });
    json(`${data}/worldgen/structure_set/${name}.json`, { structures: [{ structure: id(name), weight: 1 }],
        placement: { type: 'minecraft:random_spread', spacing: orbit ? 40 : 24, separation: orbit ? 12 : 8, salt: 751900 + index * 97 } });
    const entry = (name, min, max) => ({ type: 'minecraft:item', name, functions: [{ function: 'minecraft:set_count', count: { type: 'minecraft:uniform', min, max } }] });
    json(`${data}/loot_table/chests/${name}.json`, { type: 'minecraft:chest', pools: [
        { rolls: 1, entries: [entry(id(scheme), 1, 1)] },
        { rolls: { type: 'minecraft:uniform', min: 2, max: 4 }, entries: [entry(id(resource), 2, 6), entry(id('titanium_ore'), 2, 5), entry('minecraft:lava_bucket', 1, 1)] }
    ] });
}
for (const [name, pattern, key] of [
    ['thruster', ['T T','TCT',' T '], { T: { item: id('titanium_ore') }, C: { item: id('lunar_crystal') } }],
    ['navigation_core', [' M ','CLC',' M '], { M: { item: id('martian_metal') }, C: { item: id('mercurian_cobalt') }, L: { item: id('lunar_crystal') } }]
]) json(`${data}/recipe/${name}.json`, { type: 'minecraft:crafting_shaped', category: 'equipment', pattern, key, result: { id: id(name), count: 1 } });
// Los componentes tienen uso inmediato y solo exigen materiales del nivel anterior.
const tier2 = read(`${data}/recipe/tier2_rocket.json`);
tier2.pattern = ['CHC', 'CRC', 'CTC']; tier2.key.H = { item: id('thruster') };
json(`${data}/recipe/tier2_rocket.json`, tier2);
const tier3 = read(`${data}/recipe/tier3_rocket.json`);
tier3.pattern = ['MNM', 'SRS', 'MCM']; tier3.key.N = { item: id('navigation_core') };
json(`${data}/recipe/tier3_rocket.json`, tier3);

const languageEntries = {
    'itemGroup.galaxycraft': ['GalaxyCraft — Exploración espacial', 'GalaxyCraft — Space Exploration'],
    'container.galaxycraft.rocket': ['Control del cohete', 'Rocket Control'],
    'celestial.galaxycraft.earth': ['Tierra', 'Earth'],
    'block.galaxycraft.tier2_rocket': ['Cohete de nivel 2', 'Tier 2 Rocket'],
    'block.galaxycraft.tier3_rocket': ['Cohete de nivel 3', 'Tier 3 Rocket'],
    'item.galaxycraft.galaxy_emblem': ['Emblema de GalaxyCraft', 'GalaxyCraft Emblem'],
    'item.galaxycraft.thruster': ['Propulsor de titanio', 'Titanium Thruster'],
    'item.galaxycraft.navigation_core': ['Núcleo de navegación', 'Navigation Core'],
    'item.galaxycraft.lunar_schematic': ['Esquema de propulsión lunar', 'Lunar Propulsion Schematic'],
    'item.galaxycraft.martian_schematic': ['Esquema de navegación marciana', 'Martian Navigation Schematic'],
    'message.galaxycraft.schematic': ['Receta añadida al libro. Conservas el esquema.', 'Recipe added to your book. The schematic is reusable.'],
    'hud.galaxycraft.oxygen': ['Oxígeno: %s min %s s', 'Oxygen: %s min %s s'],
    'hud.galaxycraft.environment': ['Altitud Y: %s m · P nominal: %s kPa', 'Altitude Y: %s m · Nominal P: %s kPa'],
    'hud.galaxycraft.helmet': ['¡Falta el casco espacial!', 'Space helmet required!'],
    'subtitles.galaxycraft.rocket_launch': ['Ruge el propulsor del cohete', 'Rocket engine roars'],
    'subtitles.galaxycraft.suit_depressurize': ['El traje pierde presión', 'Suit depressurizes'],
    'subtitles.galaxycraft.space_ambient': ['Resuena el ambiente espacial', 'Space ambience resonates']
};
for (const [index, lang] of ['es_es', 'en_us'].entries()) {
    const file = `${assets}/lang/${lang}.json`, texts = read(file);
    for (const [key, values] of Object.entries(languageEntries)) texts[key] = values[index];
    const detail = (name, es, en) => { texts[`tooltip.galaxycraft.detail.${name}`] = index === 0 ? es : en; };
    for (const body of bodies) if (body.id !== 'earth') {
        const english = body.id[0].toUpperCase() + body.id.slice(1);
        for (const suffix of ['stone', 'regolith', 'ore']) {
            detail(`${body.id}_${suffix}`, `Origen: ${body.name}. ${suffix === 'ore' ? 'Necesita pico de hierro; entrega el recurso local.' : 'Material de construcción planetario; extraer con pico.'}`,
                `Origin: ${english}. ${suffix === 'ore' ? 'Requires an iron pickaxe; drops the local resource.' : 'Planetary building material; mine with a pickaxe.'}`);
            if (index === 1) texts[`block.galaxycraft.${body.id}_${suffix}`] = `${english} ${suffix === 'stone' ? 'Stone' : suffix === 'ore' ? 'Ore' : 'Regolith'}`;
        }
        detail(body.resource, `Recurso exclusivo de ${body.name}; úsalo para tecnología o conserva tu reserva.`, `Exclusive ${english} resource; use for technology or keep a reserve.`);
        if (index === 1) texts[`celestial.galaxycraft.${body.id}`] = english === 'Moon' ? 'Moon' : english;
        if (index === 1 && body.mob) texts[`entity.galaxycraft.${body.id}_crawler`] = `${english} Crawler`;
    }
    detail('titanium_ore', 'Titanio de la Tierra y la Luna. Requiere pico de hierro; base de la tecnología espacial.', 'Titanium from Earth and the Moon. Requires an iron pickaxe; the basis of space technology.');
    detail('oxygen_tank', 'Lleva este tanque en el inventario o mano secundaria. El casco consume una unidad por segundo.', 'Carry in your main inventory or offhand. The helmet consumes one unit per second.');
    detail('space_helmet', 'Equípalo en la cabeza y lleva oxígeno. No protege del calor ni de la radiación.', 'Equip on your head and carry oxygen. Does not protect against heat or radiation.');
    detail('launch_pad', 'Construye una base completa de 3 × 3 y coloca el cohete en el centro.', 'Build a full 3 × 3 pad and place the rocket in the center.');
    for (const tier of [1,2,3]) detail(`tier${tier}_rocket`, `Nivel ${tier}. Consume 4000 mB de lava por viaje. Lleva combustible para regresar.`, `Tier ${tier}. Uses 4000 mB lava per trip. Carry return fuel.`);
    detail('thruster', 'Componente de titanio y cristal lunar. Necesario para fabricar el cohete de nivel 2.', 'Titanium and lunar crystal component. Required to craft a tier 2 rocket.');
    detail('navigation_core', 'Circuito con metal marciano y cobalto. Necesario para fabricar el cohete de nivel 3.', 'Martian metal and cobalt circuit. Required to craft a tier 3 rocket.');
    for (const name of ['lunar_schematic', 'martian_schematic']) detail(name, 'Clic derecho: descubre una receta en el libro. Se recupera de estructuras y no se consume.', 'Right-click: discover a recipe in your book. Found in structures; reusable.');
    detail('galaxy_emblem', 'Símbolo de la expedición GalaxyCraft e icono de su pestaña creativa.', 'GalaxyCraft expedition insignia and creative tab icon.');
    json(file, texts);
}
console.log('Presentación generada: texturas originales, audio, estructuras NBT, botín y traducciones.');
// La ampliación reutiliza los escritores tipados sin reabrir ni editar imágenes existentes.
const { generateAdvanced } = await import('./generate-advanced.mjs');
await generateAdvanced({ png, json, read, binary, id, int, str, list, compound, intList, payload, bodies, res, assets, data });
