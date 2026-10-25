// Ampliación reproducible: texturas geológicas 128 px y cinco familias arquitectónicas por cada cuerpo.
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
export async function generateAdvanced({ png, json, read, binary, id, int, str, list, compound, intList, payload, bodies, res, assets, data }) {
    const kinds = ['observatory', 'wreck', 'sanctuary', 'geode', 'vault'];
    const labels = { observatory: ['Observatorio', 'Observatory'], wreck: ['Pecio en cráter', 'Crater Wreck'],
        sanctuary: ['Santuario alienígena', 'Alien Sanctuary'], geode: ['Geoda profunda', 'Deep Geode'], vault: ['Bóveda subterránea', 'Underground Vault'] };
    const hash = (x, y, seed) => { let n = Math.imul(x, 374761393) ^ Math.imul(y, 668265263) ^ Math.imul(seed, 1274126177); n = Math.imul(n ^ n >>> 13, 1274126177); return ((n ^ n >>> 16) >>> 0) / 4294967295; };
    const blend = (a, b, t) => a.map((v, i) => v * (1 - t) + b[i] * t);
    const clamp = color => color.map(v => Math.round(Math.max(0, Math.min(255, v))));
    function noise(u, v, scale, seed) {
        const x = Math.floor(u * scale), y = Math.floor(v * scale), smooth = t => t * t * (3 - 2 * t);
        const tx = smooth(u * scale - x), ty = smooth(v * scale - y);
        const h = (dx, dy) => hash(((x + dx) % scale + scale) % scale, ((y + dy) % scale + scale) % scale, seed);
        return (h(0,0) * (1 - tx) + h(1,0) * tx) * (1 - ty) + (h(0,1) * (1 - tx) + h(1,1) * tx) * ty;
    }
    const fbm = (u, v, seed) => noise(u,v,4,seed) * .5 + noise(u,v,8,seed + 1) * .27 + noise(u,v,16,seed + 2) * .15 + noise(u,v,32,seed + 3) * .08;
    const palettes = { moon: [126,129,135], mercury: [81,77,75], venus: [57,52,47], mars: [147,75,53],
        jupiter: [145,113,87], saturn: [176,156,110], uranus: [113,172,183], neptune: [56,86,150],
        io: [179,148,68], europa: [165,191,202], enceladus: [185,205,216], titan: [135,111,73], triton: [143,174,187] };
    for (const [index, body] of bodies.entries()) {
        if (body.id === 'earth') continue;
        const base = palettes[body.id] ?? blend(palettes[body.parent] ?? [130,145,160], [80 + index * 2, 112, 140], .22);
        const seed = 1133 + index * 7919;
        const oreColor = body.id === 'mars' ? [80,202,155] : body.id === 'venus' ? [229,183,54]
            : body.id === 'mercury' ? [79,128,204] : [94 + hash(1,2,seed) * 135, 140 + hash(3,4,seed) * 100, 130 + hash(5,6,seed) * 120];
        for (const suffix of ['stone', 'regolith', 'ore']) {
            const height = (u, v) => fbm(u, v, seed) + .12 * Math.abs(noise(u, v, 8, seed + 91) - .5);
            png(`block/${body.id}_${suffix}`, 128, 128, (x, y) => {
                const u = x / 128, v = y / 128, h = height(u, v);
                const normal = (height(u - 1/128, v) - height(u + 1/128, v)) * 210
                    + (height(u, v - 1/128) - height(u, v + 1/128)) * 135;
                let color = base.map(c => c * (.64 + h * .72) + normal + (hash(x,y,seed) - .5) * (suffix === 'regolith' ? 23 : 12));
                const vein = Math.abs(fbm(u + .14, v - .22, seed + 37) - .51);
                if (body.id === 'mars' && vein < .025) color = blend(color, [126,53,31], .6);
                if (body.id === 'venus' && vein < .014) color = blend(color, [145,111,33], .6);
                if (suffix === 'stone' && Math.abs(noise(u,v,8,seed + 15) - .5) < .014) color = color.map(c => c * .57);
                if (suffix === 'regolith') {
                    color = blend(color, base.map(c => c + 14), .28);
                    // Microcráteres toroidales: bordes sin costuras al repetir la textura.
                    for (let crater = 0; crater < 5; crater++) {
                        let dx = Math.abs(u - hash(crater,1,seed)), dy = Math.abs(v - hash(crater,2,seed)); dx = Math.min(dx, 1 - dx); dy = Math.min(dy, 1 - dy);
                        const radius = .025 + hash(crater,3,seed) * .07, d = Math.hypot(dx,dy) / radius;
                        if (d < 1) color = color.map(c => c - (1 - d) * 35 + (dx - dy) * 55);
                        else if (d < 1.13) color = color.map(c => c + 12);
                    }
                }
                if (suffix === 'ore' && (vein < .028 || noise(u,v,16,seed + 63) > .78)) {
                    color = blend(color, oreColor.map(c => c * (.65 + h * .7) + normal * 1.8), .88);
                }
                return clamp(color);
            });
        }
        png(`item/${body.resource}`,64,64,(x,y) => {
            const xx = (x - 31) / 18, yy = (y - 32) / 27;
            if (Math.abs(xx + yy * .17) + Math.abs(yy) > 1) return [0,0,0,0];
            const facet = xx < -.12 ? .53 : xx < .12 ? 1.28 : .85;
            return clamp(oreColor.map(c => c * facet + noise(x/64,y/64,16,seed) * 12));
        });
        if (body.mob) png(`entity/${body.id}_crawler`,128,64,(x,y) => {
            const plated = x % 16 < 2 || y % 12 < 2;
            return clamp(base.map(c => c * (plated ? .48 : 1) + fbm(x/128,y/64,seed) * 30));
        });
    }
    // Acabado de la infraestructura: metal cepillado, tornillería, señales y vidrio polarizado.
    for (const [name,color] of Object.entries({ hull:[174,188,200], engine:[38,44,55], window:[37,139,184],
        nose1:[179,44,38], nose2:[34,92,179], nose3:[112,51,164], launch_pad_side:[54,62,75], launch_pad_top:[120,134,143] })) {
        png(`block/${name}`,128,128,(x,y) => {
            if (name === 'launch_pad_top' && (x < 16 || x > 111 || y < 16 || y > 111)) return (x + y) % 32 < 16 ? [207,155,37] : [28,31,37];
            const panel = x % 64 < 2 || y % 64 < 2;
            const rivet = [6,57].includes(x % 64) && [6,57].includes(y % 64);
            const inset = name === 'launch_pad_top' && Math.abs(x - 64) + Math.abs(y - 64) < 32;
            return clamp(color.map(c => c + Math.sin(x / 128 * Math.PI) * 16 + (hash(x,y,55) - .5) * 7 + Math.sin(y * 2.6) * 2 - (panel ? 42 : rivet ? 65 : inset ? 25 : 0)));
        });
    }
    png('block/titanium_ore',128,128,(x,y) => {
        const h = fbm(x/128,y/128,786), vein = Math.abs(fbm(x/128,y/128,831) - .48);
        return clamp((vein < .035 ? [101,170,187] : [83,91,107]).map(c => c * (.7 + h * .65) + (hash(x,y,19) - .5) * 16));
    });
    for (const name of ['oxygen_tank','space_helmet','galaxy_emblem','thruster','navigation_core','lunar_schematic','martian_schematic']) {
        png(`item/${name}`,64,64,(x,y) => {
            const metal = [169,188,202], dark = [34,46,62], blue = [38,151,189]; let color;
            if (name === 'oxygen_tank') {
                if (x >= 27 && x < 37 && y >= 3 && y < 12) color = dark;
                else if (x > 15 && x < 48 && y > 10 && y < 59 && !((y < 14 || y > 55) && (x < 20 || x > 43)))
                    color = y > 28 && y < 37 ? blue : x > 28 && x < 35 && y > 40 && y < 49 ? [84,202,133] : metal;
            } else if (name === 'space_helmet') {
                if (Math.hypot((x - 32) / 1.1,y - 33) < 26 && y < 58) color = y > 24 && y < 44 && x > 13 && x < 51 ? blue : metal;
            } else if (name === 'galaxy_emblem') {
                const r = Math.hypot(x - 32,y - 32);
                if (r < 17) color = [39,102,166];
                else if (Math.abs(y - 32 + (x - 32) * .45) < 2 && r < 30) color = [145,214,225];
            } else if (name === 'thruster') {
                if (y > 7 && y < 58 && Math.abs(x - 32) < (y < 30 ? 13 : 21)) color = y > 49 ? [209,139,52] : x % 8 < 2 ? dark : metal;
            } else if (name === 'navigation_core') {
                if (Math.abs(x - 32) + Math.abs(y - 32) < 29) color = Math.hypot(x - 32,y - 32) < 11 ? blue : x % 8 < 2 || y % 8 < 2 ? [178,142,54] : dark;
            } else if (x > 9 && x < 54 && y > 5 && y < 59) color = x % 8 < 1 || y % 8 < 1 ? [72,154,191] : [24,65,108];
            if (!color) return [0,0,0,0];
            return clamp(color.map(c => c * (.77 + .25 * Math.cos((x - 26) / 25)) + (hash(x,y,61) - .5) * 5));
        });
    }
    for (const layer of [1,2]) png(`models/armor/space_suit_layer_${layer}`,128,64,(x,y) => {
        // La capa del sombrero debe ser transparente para no ocultar el visor del casco.
        if (x >= 64 && y < 32 || layer === 1 && x < 32 && y >= 40 && y < 56) return [0,0,0,0];
        const visor = x >= 16 && x < 32 && y >= 20 && y < 28;
        const seam = x % 16 < 2 || y % 16 < 2;
        return clamp((visor ? [25,122,164] : seam ? [38,53,69] : [174,190,199]).map(c => c + Math.cos(x / 10) * 12 + (hash(x,y,23) - .5) * 5));
    });
    for (const tier of [1,2,3]) png(`entity/rocket_tier${tier}`, 128, 128, (x,y) => {
        if (x >= 100 && y >= 110) return [255, 145 + (x + y) % 95, 35, 230];
        let base = y >= 100 ? [[180,46,38],[35,96,181],[111,53,170]][tier - 1] : [172,187,199];
        if (x >= 106 && y < 18) base = [25,112,161];
        const seam = x % 16 === 0 || y % 16 === 0, rivet = (x % 16 === 2 || x % 16 === 13) && (y % 16 === 2 || y % 16 === 13);
        return clamp(base.map(c => c + Math.sin(x / 16 * Math.PI) * 15 + (hash(x,y,tier) - .5) * 9 - (seam ? 47 : rivet ? 68 : 0)));
    });
    // Herramienta y traje: lienzos propios de 64 px con aristas, sombras y visor polarizado.
    const newItems = ['equipment_console', 'titanium_pickaxe', 'space_chestplate', 'space_leggings', 'space_boots'];
    for (const name of newItems) {
        png(`item/${name}`, 64, 64, (x,y) => {
            let inside = false, detail = false;
            if (name === 'equipment_console') { inside = x > 9 && x < 55 && y > 7 && y < 57; detail = x > 15 && x < 49 && y > 14 && y < 43; }
            if (name === 'titanium_pickaxe') { inside = Math.abs((x - 17) - (y - 17)) < 4 && y > 20 && y < 57 || y > 9 && y < 22 && x > 8 && x < 51 || x > 43 && x < 53 && y < 33 && y > 16; detail = y < 24; }
            if (name === 'space_chestplate') { inside = y > 12 && y < 55 && (x > 18 && x < 46 || y < 31 && x > 8 && x < 56); detail = x > 25 && x < 39 && y > 22 && y < 39; }
            if (name === 'space_leggings') { inside = y > 9 && y < 57 && x > 16 && x < 49 && !(y > 25 && x > 29 && x < 36); detail = y < 16; }
            if (name === 'space_boots') { inside = y > 12 && y < 55 && (x > 7 && x < 27 || x > 37 && x < 57) && (y > 41 || x % 30 > 12); detail = y > 46; }
            if (!inside) return [0,0,0,0];
            return clamp((detail ? [37,147,190] : [174,188,199]).map(c => c + 24 * Math.sin(x / 8) - (y % 12 < 2 ? 30 : 0)));
        });
        json(`${assets}/models/item/${name}.json`, { parent: name === 'titanium_pickaxe' ? 'minecraft:item/handheld' : 'minecraft:item/generated', textures: { layer0: id(`item/${name}`) } });
    }
    json(`${data}/recipe/equipment_console.json`, { type: 'minecraft:crafting_shaped', category: 'equipment', pattern: ['TGT','TRT'],
        key: { T: { item: id('titanium_ore') }, G: { item: 'minecraft:glass' }, R: { item: 'minecraft:redstone' } }, result: { id: id('equipment_console'), count: 1 } });
    json(`${data}/recipe/titanium_pickaxe.json`, { type: 'minecraft:crafting_shaped', category: 'equipment', pattern: ['TTT',' D ',' S '],
        key: { T: { item: id('titanium_ore') }, D: { item: 'minecraft:diamond' }, S: { item: 'minecraft:stick' } }, result: { id: id('titanium_pickaxe'), count: 1 } });
    for (const [name, pattern] of [['space_chestplate',['T T','TTT','TTT']], ['space_leggings',['TTT','T T','T T']], ['space_boots',['T T','T T']]])
        json(`${data}/recipe/${name}.json`, { type: 'minecraft:crafting_shaped', category: 'equipment', pattern, key: { T: { item: id('titanium_ore') } }, result: { id: id(name), count: 1 } });
    // Pieza auxiliar invisible: no tiene BlockItem ni entrega botín propio.
    json(`${assets}/blockstates/rocket_part.json`, { multipart: [{ apply: { model: id('block/rocket_part') } }] });
    json(`${assets}/models/block/rocket_part.json`, { textures: { particle: id('block/hull') }, elements: [] });
    json(`${data}/loot_table/blocks/rocket_part.json`, { type: 'minecraft:block', pools: [] });

    // Se conservan IDs antiguos para leer mundos guardados, desactivando su generación natural.
    for (const legacy of ['lunar_research_module','martian_alien_ruins','orbital_satellite']) {
        const set = read(`${data}/worldgen/structure_set/${legacy}.json`); set.placement.frequency = 0;
        json(`${data}/worldgen/structure_set/${legacy}.json`, set);
        json(`${data}/tags/worldgen/biome/has_structure/${legacy}.json`, { replace: true, values: [] });
    }
    const manifest = [];
    for (const [bodyIndex, body] of bodies.entries()) for (const [kindIndex, kind] of kinds.entries()) {
        const name = `${body.id}_${kind}`, underground = kindIndex >= 3;
        const template = `expedition/${name}`, schematic = `${name}_schematic`, seed = bodyIndex * 127 + kindIndex * 31;
        const rock = id(`${body.id}_${kind}_panel`);
        const soil = body.id === 'earth' ? 'minecraft:deepslate_tiles' : id(`${body.id}_regolith`);
        const ore = body.id === 'earth' ? id('titanium_ore') : id(`${body.id}_ore`);
        const resource = body.id === 'earth' ? 'titanium_ore' : body.resource;
        const palette = [rock, soil, ore, 'minecraft:air', 'minecraft:glass', id('launch_pad'), 'minecraft:sea_lantern', 'minecraft:chest', 'minecraft:spawner']
            .map(Name => compound({ Name: str(Name), ...(Name === 'minecraft:chest' ? { Properties: compound({ facing: str('south'), type: str('single'), waterlogged: str('false') }) } : {}) }));
        const bossRoom=kind==='vault'&&['moon','mars'].includes(body.id);
        if(bossRoom)palette.push(compound({Name:str(id(body.id==='moon'?'lunar_boss_anchor':'martian_boss_anchor'))}));
        let chest = kind === 'observatory' ? [7,1,10] : [10,1,10]; if (kind === 'sanctuary') chest = [10,5,10]; if (kind === 'vault') chest = [15,1,15]; if (kind === 'geode') chest = [10,2,10];
        const blocks = [];
        for (let x = 0; x < 21; x++) for (let z = 0; z < 21; z++) for (let y = 0; y < 12; y++) {
            const dx = x - 10, dz = z - 10, r = Math.hypot(dx,dz); let state = null;
            if (kind === 'observatory') {
                const radius = 7 + bodyIndex % 2;
                if (r <= radius + 1 && y === 0) state = 5;
                const dome = Math.sqrt(dx * dx + dz * dz + (y - 2) ** 2);
                if (y > 0 && dome < radius) state = dome > radius - 1.2 ? (y > 4 && (x + z) % 4 < 2 ? 4 : 0) : 3;
                if (Math.abs(dx) < 2 && z > 10 && y >= 1 && y <= 3) state = 3;
                if (Math.abs(dx) < 1 && Math.abs(dz) < 1 && y >= 2 && y < 8) state = y > 5 ? 2 : 5;
            } else if (kind === 'wreck') {
                const rim = 8 + bodyIndex % 2;
                if (r < rim + 1 && y === 0 || r > rim - 1 && r < rim + 1 && y <= 2) state = 1;
                const along = (dx + dz) * .707, cross = (dx - dz) * .707;
                if (Math.abs(along) < 8 && Math.abs(cross) < 3 && y > 0 && y < 6) {
                    state = Math.abs(cross) > 2 || y === 1 || y === 5 ? 5 : 3;
                    if (along > 2 && hash(x,z,seed) < .25) state = 3;
                }
                if (Math.abs(along) < 2 && Math.abs(cross) < 6 && y === 2) state = 5;
            } else if (kind === 'sanctuary') {
                if (Math.max(Math.abs(dx), Math.abs(dz)) < 8 - y && y < 5) state = (x + z + bodyIndex) % 7 === 0 ? 2 : 0;
                const column = [3,17].includes(x) && [3,17].includes(z);
                if (column && y < (bodyIndex % 2 === 0 ? 10 : 7 + bodyIndex % 4)) state = y % 3 === 0 ? 2 : 0;
                if (y === 9 && (z === 3 || z === 17) && x >= 3 && x <= 17 && bodyIndex % 2 === 0) state = 0;
            } else if (kind === 'geode') {
                const ellipsoid = dx * dx / 81 + dz * dz / 64 + (y - 5) ** 2 / 25;
                if (ellipsoid < 1) state = ellipsoid > .73 ? 0 : ellipsoid > .51 && hash(x + y,z,seed) > .48 ? 2 : 3;
                if (x >= 9 && x <= 11 && z < 6 && y >= 3 && y <= 5) state = 3;
            } else {
                const room = [5,15].some(cx => [5,15].some(cz => Math.abs(x - cx) <= 4 && Math.abs(z - cz) <= 4));
                const corridor = (Math.abs(dx) <= 1 && z >= 5 && z <= 15) || (Math.abs(dz) <= 1 && x >= 5 && x <= 15);
                if ((room || corridor) && y <= 7) {
                    const wall = [0,7].includes(y) || !corridor && ([1,9,11,19].includes(x) || [1,9,11,19].includes(z));
                    state = wall ? ((x + z + bodyIndex) % 9 === 0 ? 2 : 0) : 3;
                }
                if ((x === 5 && z === 5 || x === 15 && z === 5) && y === 6) state = 6;
                if (x === 5 && z === 15 && y === 1) state = 8;
            }
            const isChest = x === chest[0] && y === chest[1] && z === chest[2];
            if (x === chest[0] && z === chest[2] && y === chest[1] - 1) state = 0;
            if (x === chest[0] && z === chest[2] && y > chest[1] && y <= chest[1] + 2) state = 3;
            if (isChest) state = 7;
            if(bossRoom&&x===5&&z===5&&y===1)state=9;
            if (state === null) continue; // Fuera de la arquitectura se conserva el terreno del chunk.
            const block = { pos: intList([x,y,z]), state: int(state) };
            if (isChest) block.nbt = compound({ id: str('minecraft:chest'), LootTable: str(id(`chests/expedition/${name}`)) });
            if(state===9)block.nbt=compound({id:str(id('boss_anchor'))});
            if (state === 8) block.nbt = compound({ id: str('minecraft:mob_spawner'), Delay: int(100), MinSpawnDelay: int(300), MaxSpawnDelay: int(700), SpawnCount: int(2), MaxNearbyEntities: int(4), RequiredPlayerRange: int(12), SpawnRange: int(3), SpawnData: compound({ entity: compound({ id: str(id(`${body.mob ? body.id : 'moon'}_crawler`)) }) }) });
            blocks.push(compound(block));
        }
        const nbt = compound({ DataVersion: int(3953), size: intList([21,12,21]), palette: list(10,palette), blocks: list(10,blocks), entities: list(10,[]) });
        binary(`${data}/structure/${template}.nbt`, zlib.gzipSync(Buffer.concat([Buffer.from([10,0,0]), payload(nbt)])));
        const burial = kind === 'geode' ? 12 : kind === 'vault' ? 24 : 0;
        json(`${data}/tags/worldgen/biome/has_structure/${name}.json`, { replace: false, values: body.id === 'earth'
            ? ['minecraft:plains','minecraft:forest','minecraft:taiga','minecraft:desert','minecraft:savanna','minecraft:badlands'] : [id(body.id)] });
        json(`${data}/worldgen/structure/${name}.json`, { type: id('planetary_site'), biomes: `#galaxycraft:has_structure/${name}`,
            step: underground ? 'underground_structures' : 'surface_structures', terrain_adaptation: underground ? 'none' : 'beard_thin', spawn_overrides: {},
            template: id(template), underground, burial_depth: burial });
        json(`${data}/worldgen/structure_set/${name}.json`, { structures: [{ structure: id(name), weight: 1 }],
            placement: { type: 'minecraft:random_spread', spacing: 20 + kindIndex * 3, separation: 7 + kindIndex, salt: 917100 + bodyIndex * 997 + kindIndex * 71 } });
        const item = (name, min, max, weight = 1) => ({ type: 'minecraft:item', name, weight, functions: [{ function: 'minecraft:set_count', count: { type: 'minecraft:uniform', min, max } }] });
        json(`${data}/loot_table/chests/expedition/${name}.json`, { type: 'minecraft:chest', pools: [
            { rolls: 1, entries: [item(id(schematic),1,1)] },
            { rolls: { type: 'minecraft:uniform', min: 3, max: 6 }, entries: [item(id(resource),3,9,4), item(id('titanium_ore'),2,6,2), item('minecraft:lava_bucket',1,1), item('minecraft:diamond',1,2)] }
        ] });
        const result = ['equipment_console','thruster','launch_pad','oxygen_tank','navigation_core'][kindIndex];
        json(`${data}/recipe/expedition/${name}.json`, { type: 'minecraft:crafting_shaped', category: 'equipment', pattern: ['RPR','TST','RGR'], key: {
            R: { item: id(resource) }, P: { item: kindIndex >= 3 ? 'minecraft:diamond' : 'minecraft:redstone' }, T: { item: id('titanium_ore') },
            S: { item: id(schematic) }, G: { item: 'minecraft:glass' }
        }, result: { id: id(result), count: result === 'launch_pad' ? 8 : 1 } });
        png(`item/${schematic}`,64,64,(x,y) => {
            if (x < 10 || x > 53 || y < 6 || y > 57) return [0,0,0,0];
            if (x < 12 || x > 51 || y < 8 || y > 55) return [170,190,206];
            const diagram = kind === 'observatory' ? Math.abs(Math.hypot(x - 32,y - 34) - 15) < 1.6 : kind === 'wreck' ? Math.abs(x - y + 4) < 3
                : kind === 'sanctuary' ? y > 43 - Math.abs(x - 32) / 2 && y < 47 : kind === 'geode' ? Math.abs(Math.hypot(x - 32,y - 33) - 13) < 2.5
                : (x % 14 < 2 || y % 14 < 2) && x > 17 && x < 47 && y > 18 && y < 49;
            const bodyColor = palettes[body.id] ?? palettes[body.parent] ?? [118,157,188];
            return diagram ? clamp(bodyColor.map(c => c + 70)) : x % 8 === 0 || y % 8 === 0 ? [48,102,143] : [19,46,83];
        });
        json(`${assets}/models/item/${schematic}.json`, { parent: 'minecraft:item/generated', textures: { layer0: id(`item/${schematic}`) } });
        manifest.push({ id: name, body: body.id, kind, underground, burial_depth: burial, template: id(template), schematic: id(schematic), recipe: id(`expedition/${name}`), result: id(result) });
    }
    json('expedition_sites.json', manifest);
    // Plantilla vacía de integración: no es una estructura generable ni añade un sexto sitio.
    const empty = compound({ DataVersion: int(3953), size: intList([21,12,21]), palette: list(10,[compound({ Name: str('minecraft:air') })]), blocks: list(10,[]), entities: list(10,[]) });
    binary(`${data}/structure/tests/empty.nbt`, zlib.gzipSync(Buffer.concat([Buffer.from([10,0,0]),payload(empty)])));
    for (const [languageIndex, language] of ['es_es','en_us'].entries()) {
        const file = `${assets}/lang/${language}.json`, texts = read(file), es = languageIndex === 0;
        const entries = {
            'container.galaxycraft.equipment': ['Equipamiento espacial', 'Space Equipment'],
            'gui.galaxycraft.suit_button': ['Traje', 'Suit'],
            'key.galaxycraft.equipment': ['Abrir equipamiento espacial', 'Open space equipment'],
            'key.categories.galaxycraft': ['GalaxyCraft', 'GalaxyCraft'],
            'gui.galaxycraft.refill_tanks': ['Recargar en atmósfera respirable', 'Refill in breathable air'],
            'item.galaxycraft.equipment_console': ['Terminal de equipo espacial', 'Space Equipment Terminal'],
            'item.galaxycraft.titanium_pickaxe': ['Pico de titanio', 'Titanium Pickaxe'],
            'item.galaxycraft.space_chestplate': ['Pechera espacial', 'Space Chestplate'],
            'item.galaxycraft.space_leggings': ['Pantalones espaciales', 'Space Leggings'],
            'item.galaxycraft.space_boots': ['Botas espaciales', 'Space Boots'],
            'tooltip.galaxycraft.detail.equipment_console': ['Clic derecho: abre seis ranuras de traje y tanques sin quitar tu armadura.', 'Right-click: open six suit and tank slots without removing armor.'],
            'tooltip.galaxycraft.detail.titanium_pickaxe': ['Herramienta de titanio con capacidad de extracción de diamante; reparación con diamantes.', 'Titanium tool with diamond mining capability; repairs with diamonds.'],
            'gui.galaxycraft.status.1': ['Necesita 3 × 3 × 7 libres', 'Requires 3 × 3 × 7 clearance'],
            'gui.galaxycraft.launch': ['Encender', 'Ignite']
        };
        for (const [key, values] of Object.entries(entries)) texts[key] = values[languageIndex];
        for (const [i, names] of [['Cas','Helm'],['Pec','Chest'],['Pan','Legs'],['Bot','Feet'],['O₂','O₂'],['O₂','O₂']].entries()) texts[`gui.galaxycraft.slot.${i}`] = names[languageIndex];
        for (const [key, names] of Object.entries({ building: ['CONSTRUCCIÓN','BUILDING'], resources: ['MINERALES','RESOURCES'], tools: ['HERRAMIENTAS','TOOLS'], suit: ['TRAJE ESPACIAL','SPACE SUIT'], technology: ['COMPONENTES','COMPONENTS'], rockets: ['COHETES','ROCKETS'], schematics: ['ESQUEMAS DE EXPLORACIÓN','EXPLORATION SCHEMATICS'] })) texts[`section.galaxycraft.${key}`] = '━━ ' + names[languageIndex] + ' ━━';
        for (const name of ['space_chestplate','space_leggings','space_boots','space_helmet']) texts[`tooltip.galaxycraft.detail.${name}`] = es
            ? 'Equipa en la terminal espacial. El traje completo alimentado reduce calor y radiación un 75 % sin ocupar tu armadura.'
            : 'Equip in the space terminal. A fully supplied suit reduces heat and radiation by 75% without using armor slots.';
        for (const site of manifest) {
            const body = bodies.find(body => body.id === site.body), planet = es ? body.name : body.id[0].toUpperCase() + body.id.slice(1);
            texts[`item.galaxycraft.${site.id}_schematic`] = `${labels[site.kind][languageIndex]} — ${planet}`;
            texts[`tooltip.galaxycraft.detail.${site.id}_schematic`] = es
                ? `Esquema exclusivo de ${body.name}. Clic derecho: aprende una receta alternativa; fabricar consume el esquema.`
                : `Exclusive ${planet} schematic. Right-click to learn an alternate recipe; crafting consumes the schematic.`;
        }
        texts['tooltip.galaxycraft.rocket_pad'] = es ? 'Base 3 × 3 y volumen libre de 3 × 3 × 7. Encendido de cuatro segundos.' : '3 × 3 pad and 3 × 3 × 7 clearance. Four-second ignition.';
        json(file,texts);
    }
    console.log(`Ampliación: ${manifest.length} estructuras activas, cinco por cuerpo; texturas geológicas de 128 px.`);
    const { generateNavigation } = await import('./generate-navigation-assets.mjs');
    generateNavigation({ png,json,read,binary,id,bodies,res,assets,data });
}
