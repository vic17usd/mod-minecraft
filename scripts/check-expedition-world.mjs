// Verificación de solo lectura del mundo: cruza las 145 búsquedas con las piezas persistidas en Anvil.
// Uso: node scripts/check-expedition-world.mjs [carpeta mundo] [carpeta logs]
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const world = path.resolve(process.argv[2] ?? path.join(root,'run/server/world'));
const logFolder = path.resolve(process.argv[3] ?? path.join(root,'run/server/logs'));
const sites = JSON.parse(fs.readFileSync(path.join(root,'src/main/resources/expedition_sites.json')));
const found = new Map();
for (const file of fs.readdirSync(logFolder).sort()) {
    if (!file.endsWith('.log') && !file.endsWith('.log.gz')) continue;
    const raw = fs.readFileSync(path.join(logFolder,file));
    const text = (file.endsWith('.gz') ? zlib.gunzipSync(raw) : raw).toString();
    for (const match of text.matchAll(/The nearest galaxycraft:([a-z_]+) is at \[(-?\d+), ~, (-?\d+)\]/g))
        found.set(match[1],{ x:Number(match[2]), z:Number(match[3]) });
}
const verified = [], failures = [];
for (const site of sites) {
    const position = found.get(site.id);
    if (!position) { failures.push({ id:site.id, error:'No hay búsqueda guardada en los logs' }); continue; }
    const folder = site.body === 'earth' ? path.join(world,'region') : path.join(world,'dimensions/galaxycraft',site.body,'region');
    try {
        const result = JSON.parse(execFileSync(process.execPath,[path.join(root,'scripts/check-structure-chunk.mjs'),folder,
            String(Math.floor(position.x / 16)),String(Math.floor(position.z / 16)),`galaxycraft:${site.id}`],{ encoding:'utf8',stdio:['ignore','pipe','pipe'] }));
        if (result.status !== 'minecraft:full' || result.pieces !== 1) throw new Error('Chunk incompleto o sin pieza');
        for (const box of result.boundingBoxes) if (box[1] < (site.body === 'earth' ? -56 : 8) || box[4] >= (site.body === 'earth' ? 316 : 252)) throw new Error('Pieza fuera de límites');
        verified.push({ id:site.id, body:site.body, underground:site.underground, ...result });
    } catch (error) { failures.push({ id:site.id, error:error.stderr?.toString().split('\n').slice(0,5).join('\n') ?? error.message }); }
}
const report = { expected:sites.length, verified:verified.length, surface:verified.filter(site=>!site.underground).length,
    underground:verified.filter(site=>site.underground).length, failures, sites:verified };
const target = path.join(root,'build/reports/expedition-world-validation.json');
fs.mkdirSync(path.dirname(target),{ recursive:true }); fs.writeFileSync(target,JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify({ expected:report.expected, verified:report.verified, surface:report.surface, underground:report.underground, failures, report:target }));
if (failures.length) process.exitCode = 1;
