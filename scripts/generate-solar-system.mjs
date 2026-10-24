// Generador reproducible de recursos 1.21 a partir del catálogo compartido con Java.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const resources = path.join(root, 'src/main/resources');
const bodies = JSON.parse(fs.readFileSync(path.join(resources, 'solar_system.json'), 'utf8'));
function write(file, value) {
    const target = path.join(resources, file);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, JSON.stringify(value, null, 2) + '\n');
}
const data = 'data/galaxycraft';
const assets = 'assets/galaxycraft';
const id = name => `galaxycraft:${name}`;
const blockState = name => ({ Name: id(name) });
const drops = (name, item, type = 'block') => ({ type: `minecraft:${type}`, pools: [{ rolls: 1,
    entries: [{ type: 'minecraft:item', name: id(item) }],
    ...(type === 'block' ? { conditions: [{ condition: 'minecraft:survives_explosion' }] } : {}) }] });
const mineable = [];
for (const [i, body] of bodies.entries()) {
    if (body.id === 'earth') continue;
    const stone = `${body.id}_stone`, soil = `${body.id}_regolith`, ore = `${body.id}_ore`;
    for (const [name, texture, item] of [[stone, body.stoneTexture, stone], [soil, body.soilTexture, soil], [ore, 'iron_ore', body.resource]]) {
        mineable.push(id(name));
        write(`${assets}/blockstates/${name}.json`, { variants: { '': { model: `galaxycraft:block/${name}` } } });
        write(`${assets}/models/block/${name}.json`, { parent: 'minecraft:block/cube_all', textures: { all: `minecraft:block/${texture}` } });
        write(`${assets}/models/item/${name}.json`, { parent: `galaxycraft:block/${name}` });
        write(`${data}/loot_table/blocks/${name}.json`, drops(name, item));
    }
    write(`${assets}/models/item/${body.resource}.json`, { parent: 'minecraft:item/generated', textures: {
        layer0: `minecraft:item/${body.id === 'venus' ? 'glowstone_dust' : i % 2 ? 'amethyst_shard' : 'raw_iron'}` } });
    write(`${data}/dimension_type/${body.id}.json`, {
        ultrawarm: body.heat >= 3, natural: false, coordinate_scale: 1, has_skylight: true,
        has_ceiling: false, ambient_light: 0.05, fixed_time: body.atmosphere === 'none' ? 18000 : 6000,
        piglin_safe: false, bed_works: false, respawn_anchor_works: false, has_raids: false,
        min_y: 0, height: 256, logical_height: 256, infiniburn: '#minecraft:infiniburn_overworld',
        effects: 'minecraft:overworld', monster_spawn_block_light_limit: 15, monster_spawn_light_level: 15
    });
    write(`${data}/dimension/${body.id}.json`, { type: id(body.id), generator: {
        type: 'minecraft:noise', settings: id(body.id), biome_source: { type: 'minecraft:fixed', biome: id(body.id) }
    }});
    // Densidad por gradiente vertical y ruido: cada cuerpo tiene relieve y escala propios.
    const density = { type: 'minecraft:add', argument1: { type: 'minecraft:y_clamped_gradient',
        from_y: 0, to_y: body.terrainHeight * 2, from_value: 1, to_value: -1 },
        argument2: { type: 'minecraft:mul', argument1: .8, argument2: {
            type: 'minecraft:noise', noise: 'minecraft:continentalness', xz_scale: body.terrainScale, y_scale: 0 } } };
    const router = Object.fromEntries(['barrier', 'fluid_level_floodedness', 'fluid_level_spread', 'lava', 'temperature',
        'vegetation', 'continents', 'erosion', 'depth', 'ridges', 'vein_toggle', 'vein_ridged', 'vein_gap'].map(k => [k, 0]));
    Object.assign(router, { final_density: density, initial_density_without_jaggedness: density });
    write(`${data}/worldgen/noise_settings/${body.id}.json`, {
        sea_level: 0, disable_mob_generation: false, aquifers_enabled: false, ore_veins_enabled: false,
        legacy_random_source: false, default_block: blockState(stone), default_fluid: { Name: 'minecraft:air' },
        noise: { min_y: 0, height: 256, size_horizontal: 2, size_vertical: 1 }, noise_router: router,
        spawn_target: [], surface_rule: { type: 'minecraft:sequence', sequence: [
            { type: 'minecraft:condition', if_true: { type: 'minecraft:vertical_gradient', random_name: 'minecraft:bedrock_floor',
                true_at_and_below: { above_bottom: 0 }, false_at_and_above: { above_bottom: 4 } },
                then_run: { type: 'minecraft:block', result_state: { Name: 'minecraft:bedrock' } } },
            { type: 'minecraft:condition', if_true: { type: 'minecraft:stone_depth', offset: 2,
                add_surface_depth: false, secondary_depth_range: 0, surface_type: 'floor' },
                then_run: { type: 'minecraft:block', result_state: blockState(soil) } }
        ] }
    });
    const features = Array.from({ length: 11 }, () => []);
    features[6].push(id(`${body.id}_ore`));
    features[2].push(id(`${body.id}_boulders`));
    if (body.id === 'moon') features[6].push(id('lunar_titanium'));
    write(`${data}/worldgen/biome/${body.id}.json`, {
        has_precipitation: false, temperature: body.heat ? 2 : .1, downfall: 0,
        effects: { sky_color: body.color, fog_color: body.color, water_color: body.color, water_fog_color: body.color,
            grass_color: body.color, foliage_color: body.color },
        carvers: {}, features, spawn_costs: {}, spawners: body.mob ? {
            monster: [{ type: id(`${body.id}_crawler`), weight: 12, minCount: 1, maxCount: 3 }]
        } : {}
    });
    const configuredOre = (name, result) => {
        write(`${data}/worldgen/configured_feature/${name}.json`, { type: 'minecraft:ore', config: {
            size: 6 + i % 4, discard_chance_on_air_exposure: 0,
            targets: [{ target: { predicate_type: 'minecraft:block_match', block: id(stone) }, state: blockState(result) }]
        }});
        write(`${data}/worldgen/placed_feature/${name}.json`, { feature: id(name), placement: [
            { type: 'minecraft:count', count: 8 }, { type: 'minecraft:in_square' },
            { type: 'minecraft:height_range', height: { type: 'minecraft:uniform', min_inclusive: { absolute: 5 },
                max_inclusive: { absolute: body.terrainHeight } } }, { type: 'minecraft:biome' }
        ] });
    };
    configuredOre(`${body.id}_ore`, ore);
    if (body.id === 'moon') configuredOre('lunar_titanium', 'titanium_ore');
    write(`${data}/worldgen/configured_feature/${body.id}_boulders.json`, { type: 'minecraft:forest_rock', config: { state: blockState(stone) } });
    write(`${data}/worldgen/placed_feature/${body.id}_boulders.json`, { feature: id(`${body.id}_boulders`), placement: [
        { type: 'minecraft:count', count: 1 + i % 5 }, { type: 'minecraft:in_square' },
        { type: 'minecraft:heightmap', heightmap: 'WORLD_SURFACE_WG' }, { type: 'minecraft:biome' }
    ] });
    if (body.mob) write(`${data}/loot_table/entities/${body.id}_crawler.json`, drops('', body.resource, 'entity'));
}
write(`${data}/tags/block/mineable_planetary.json`, { replace: false, values: mineable });
write('data/minecraft/tags/block/base_stone_overworld.json', { replace: false,
    values: mineable.filter(name => name.endsWith('_stone') || name.endsWith('_regolith')) });
// Se reutilizan los recursos de nivel 1 sin duplicar geometría ni texturas.
for (const tier of [2, 3]) {
    const name = `tier${tier}_rocket`;
    write(`${assets}/blockstates/${name}.json`, { variants: { '': { model: `galaxycraft:block/${name}` } } });
    write(`${assets}/models/block/${name}.json`, { parent: 'galaxycraft:block/tier1_rocket', textures: {
        nose: `minecraft:block/${tier === 2 ? 'blue_concrete' : 'purple_concrete'}` } });
    write(`${assets}/models/item/${name}.json`, { parent: `galaxycraft:block/${name}` });
    write(`${data}/loot_table/blocks/${name}.json`, drops(name, name));
}
for (const [tier, key, pattern] of [
    [2, { C: { item: id('lunar_crystal') }, R: { item: id('tier1_rocket') }, T: { item: id('titanium_ore') } }, ['CTC', 'CRC', 'CTC']],
    [3, { M: { item: id('martian_metal') }, S: { item: id('venusian_sulfur') }, C: { item: id('mercurian_cobalt') }, R: { item: id('tier2_rocket') } }, ['MCM', 'SRS', 'MCM']]
]) write(`${data}/recipe/tier${tier}_rocket.json`, { type: 'minecraft:crafting_shaped', category: 'equipment', pattern, key, result: { id: id(`tier${tier}_rocket`), count: 1 } });
// Actualiza solo claves propiedad de este generador y conserva las traducciones anteriores.
for (const language of ['es_es', 'en_us']) {
    const file = `${assets}/lang/${language}.json`;
    const translations = JSON.parse(fs.readFileSync(path.join(resources, file), 'utf8'));
    for (const body of bodies) {
        translations[`celestial.galaxycraft.${body.id}`] = body.name;
        if (body.id === 'earth') continue;
        translations[`block.galaxycraft.${body.id}_stone`] = `Roca de ${body.name}`;
        translations[`block.galaxycraft.${body.id}_regolith`] = `Suelo de ${body.name}`;
        translations[`block.galaxycraft.${body.id}_ore`] = `Mineral de ${body.name}`;
        const labels = { lunar_crystal: ['Cristal lunar', 'Lunar Crystal'],
            martian_metal: ['Metal marciano', 'Martian Metal'], venusian_sulfur: ['Azufre venusiano', 'Venusian Sulfur'],
            mercurian_cobalt: ['Cobalto mercuriano', 'Mercurian Cobalt'] };
        translations[`item.galaxycraft.${body.resource}`] = labels[body.resource]?.[language === 'es_es' ? 0 : 1]
            ?? (language === 'es_es' ? `Cristal de ${body.name}` : `${body.id} Crystal`);
        if (body.mob) translations[`entity.galaxycraft.${body.id}_crawler`] = `Reptador de ${body.name}`;
    }
    for (const tier of [2, 3]) translations[`block.galaxycraft.tier${tier}_rocket`] = `Cohete de nivel ${tier}`;
    Object.assign(translations, language === 'en_us' ? {
        'gui.galaxycraft.destination': 'Destination: %s (T%s)',
        'gui.galaxycraft.launch': 'Travel', 'gui.galaxycraft.next': 'Next destination',
        'message.galaxycraft.travel_failed': 'Travel failed. Check fuel, launch pad, equipment and destination.',
        'message.galaxycraft.arrived': 'Arrived at %s. The rocket is empty: bring fuel for the return trip.'
    } : {
        'gui.galaxycraft.destination': 'Destino: %s (N%s)',
        'gui.galaxycraft.launch': 'Viajar', 'gui.galaxycraft.next': 'Destino siguiente',
        'message.galaxycraft.travel_failed': 'No se pudo viajar. Comprueba combustible, base, equipo y destino.',
        'message.galaxycraft.arrived': 'Llegada a %s. El cohete está vacío: conserva combustible para volver.'
    });
    write(file, translations);
}
console.log(`Recursos generados para ${bodies.length - 1} dimensiones; Tierra = Overworld.`);
// El acabado propio se aplica al final para que regenerar la fase 4 no restaure placeholders.
await import('./generate-presentation.mjs');
