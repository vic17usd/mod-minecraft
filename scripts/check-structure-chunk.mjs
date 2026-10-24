// Diagnóstico de solo lectura: comprueba las piezas de una estructura en un chunk ya guardado.
// Uso: node scripts/check-structure-chunk.mjs <carpeta region> <chunkX> <chunkZ> <ID estructura>
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
const [folder, xText, zText, expected] = process.argv.slice(2);
const x = Number(xText), z = Number(zText);
if (!folder || !expected || !Number.isInteger(x) || !Number.isInteger(z)) throw new Error('Faltan region, chunkX, chunkZ e ID');
const region = fs.readFileSync(path.join(folder, `r.${Math.floor(x / 32)}.${Math.floor(z / 32)}.mca`));
const slot = ((x & 31) + (z & 31) * 32) * 4;
const offset = region.readUIntBE(slot, 3) * 4096;
if (!offset) throw new Error('El chunk todavía no se ha guardado');
const size = region.readUInt32BE(offset), compression = region[offset + 4];
const raw = region.subarray(offset + 5, offset + 4 + size);
const bytes = compression === 2 ? zlib.inflateSync(raw) : compression === 1 ? zlib.gunzipSync(raw) : raw;
let cursor = 0;
function number(method, length) { const value = bytes[method](cursor); cursor += length; return value; }
const string = () => { const length = number('readUInt16BE', 2); const result = bytes.toString('utf8', cursor, cursor + length); cursor += length; return result; };
function value(type) {
    if (type === 1) return number('readInt8', 1);
    if (type === 2) return number('readInt16BE', 2);
    if (type === 3) return number('readInt32BE', 4);
    if (type === 4) { cursor += 8; return null; }
    if (type === 5) return number('readFloatBE', 4);
    if (type === 6) return number('readDoubleBE', 8);
    if (type === 8) return string();
    if (type === 9) { const child = number('readUInt8', 1), length = number('readInt32BE', 4); return Array.from({ length }, () => value(child)); }
    if (type === 10) {
        const result = {};
        for (;;) { const child = number('readUInt8', 1); if (!child) return result; const name = string(); result[name] = value(child); }
    }
    if (type === 11) { const count = number('readInt32BE', 4); return Array.from({ length: count }, () => number('readInt32BE', 4)); }
    if (type === 7 || type === 12) { const count = number('readInt32BE', 4); cursor += count * (type === 7 ? 1 : 8); return null; }
    throw new Error(`Tipo NBT desconocido: ${type}`);
}
const type = number('readUInt8', 1); string(); const chunk = value(type);
const start = chunk.structures?.starts?.[expected];
if (!start || !start.Children?.length) throw new Error(`No hay piezas de ${expected} en ${x},${z}`);
console.log(JSON.stringify({ structure: start.id, chunk: [x, z], pieces: start.Children.length,
    boundingBoxes: start.Children.map(piece => piece.BB), status: chunk.Status }));
