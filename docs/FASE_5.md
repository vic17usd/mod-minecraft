# Fase 5 — Presentación y exploración

Minecraft **1.21 exacto**, NeoForge **21.0.167**, Java **21**. Versión del mod:
**0.2.0**. Conserva los identificadores y los Data Components de las fases anteriores.

## Pestaña y equipo

`registry/ModPresentation.java` registra la pestaña `galaxycraft:galaxycraft`, con
un emblema planetario propio. Su orden es estable: rocas y suelos, minerales y
recursos, supervivencia, plataforma y cohetes, componentes y esquemas. Incluye
todos los ítems registrados; las entradas previas en pestañas vanilla se conservan.

El casco conserva su defensa y durabilidad, pero ahora tiene material
`galaxycraft:space_suit` y textura equipada propios. Se repara con mineral de
titanio. Sigue siendo un casco con soporte para tanques, no un traje completo
que bloquee calor o radiación.

## Recursos originales

Todos los modelos de bloques e ítems utilizan PNG del namespace `galaxycraft`.
Los padres geométricos estándar como `minecraft:block/cube_all` e
`minecraft:item/generated` se conservan: son plantillas de geometría, no texturas
provisionales. Hay arte pixelado propio para los 84 bloques planetarios, los 28
recursos, titanio, plataforma, cohetes, tanque, casco, emblema y tecnología.
Los cohetes comparten geometría con pintura diferenciada por nivel. Los cuatro
reptadores conservan su geometría de silverfish, con pieles planetarias propias.

Archivos principales, relativos a la raíz del proyecto en IntelliJ:

| Contenido | Carpeta |
| --- | --- |
| Modelos de bloques e ítems | `src/main/resources/assets/galaxycraft/models/` |
| Texturas de bloques e ítems | `src/main/resources/assets/galaxycraft/textures/block/` y `textures/item/` |
| Casco equipado | `src/main/resources/assets/galaxycraft/textures/models/armor/` |
| Reptadores | `src/main/resources/assets/galaxycraft/textures/entity/` |
| Sprites y definiciones de partículas | `src/main/resources/assets/galaxycraft/textures/particle/` y `particles/` |
| Audio Vorbis y eventos | `src/main/resources/assets/galaxycraft/sounds/` y `sounds.json` |
| Traducciones y tooltips | `src/main/resources/assets/galaxycraft/lang/es_es.json` y `en_us.json` |

`scripts/generate-presentation.mjs` dibuja los PNG RGBA y sintetiza el audio, sin
copiar muestras ni imágenes de Minecraft. También escribe las plantillas NBT.
El generador solar lo ejecuta automáticamente al final para no restaurar los
placeholders de la fase anterior:

```bash
node scripts/generate-solar-system.mjs
```

Node es necesario solo para regenerar recursos; el JAR incluye todos los archivos.
FFmpeg se necesita solo si falta audio o al regenerarlo explícitamente:

```bash
node scripts/generate-presentation.mjs --audio
```

## Estructuras y modificadores

Se utilizan los registros dinámicos oficiales de Minecraft 1.21: `structure`,
`structure_set`, `template_pool`, `processor_list` y etiquetas de biomas. No se
introduce una API inventada llamada `Structure Modifiers`: las modificaciones
de las plantillas se expresan mediante procesadores de reglas. En módulos y
ruinas sustituyen el 8 % del material de sus muros por regolito erosionado;
no modifican cofres ni otros bloques del mundo.

| ID bajo `galaxycraft:` | Biomas | Aparición | Botín |
| --- | --- | --- | --- |
| `lunar_research_module` | Luna | Superficie, separación de regiones 24 chunks | Esquema lunar, cristales lunares, titanio y combustible |
| `martian_alien_ruins` | Marte | Superficie, separación de regiones 24 chunks | Esquema marciano, metal marciano, titanio y combustible |
| `orbital_satellite` | Luna y Marte | Altura inicial Y=200, regiones de 40 chunks | Esquema marciano, cobalto, titanio y combustible |

La separación interna mínima es de 8 chunks para instalaciones y de 12 para
satélites. No significa que cada estructura esté a esa distancia exacta. Los
satélites son instalaciones suspendidas en las dimensiones existentes; no crean
una dimensión orbital ni una simulación de órbitas. Su altura cabe en los mundos
planetarios de 256 bloques. Necesitarás construir acceso o volar en creativo.

Cada plantilla tiene un cofre con una tabla propia bajo
`data/galaxycraft/loot_table/chests/`. El esquema está garantizado; las otras
recompensas son aleatorias. Las estructuras aparecen de forma natural en chunks
nuevos; un mundo existente necesita explorar terreno aún no generado.

La distribución está en `data/galaxycraft/worldgen/structure_set/`; el montaje,
en `worldgen/structure/` y `worldgen/template_pool/`; los modificadores, en
`worldgen/processor_list/`; las plantillas comprimidas, en
`data/galaxycraft/structure/*.nbt` (carpeta singular en 1.21).

## Tecnología y esquemas

Clic derecho con un esquema descubre la receta en el libro, en el servidor,
sin consumirlo. No bloquea fabricar manualmente la receta conocida: encontrar
estructuras no es obligatorio para completar la progresión.

Propulsor (`T` titanio, `C` cristal lunar):

```text
T T
TCT
 T
```

Núcleo de navegación (`M` metal marciano, `C` cobalto mercuriano, `L` cristal lunar):

```text
 M
CLC
 M
```

El cohete de nivel 2 ahora incorpora un propulsor (`H`), conservando el cohete
anterior (`R`), cristal lunar (`C`) y titanio (`T`): `CHC / CRC / CTC`.
El nivel 3 incorpora un núcleo (`N`), cohete anterior (`R`), metal marciano (`M`),
azufre venusiano (`S`) y cobalto (`C`): `MNM / SRS / MCM`.
Las recetas solo requieren materiales accesibles con el nivel anterior.
Los cohetes ya fabricados siguen funcionando.

## HUD y efectos

`client/SpaceHud.java` registra una capa mediante `RegisterGuiLayersEvent`.
Solo se muestra en entornos no respirables o en la simulación de vacío. Respeta
F1 y espectadores; no ocupa las barras de vida ni la barra rápida. Suma los
tanques del inventario principal y mano secundaria, muestra tiempo de reserva,
barra proporcional y advertencia si falta casco. La reserva es tiempo de juego
a 20 TPS; en creativo no se consume oxígeno. No cuenta contenedores anidados.

La altitud representa la coordenada Y. La presión es una referencia **nominal de
diseño**, no una medición dinámica ni presión parcial de oxígeno: Luna y otras
lunas sin atmósfera 0 kPa, Marte 0,61, Venus 9200, Titán 146,7 y gigantes una
capa jugable de referencia de 100 kPa. Una atmósfera a presión no implica que
sea respirable. La física del oxígeno sigue bajo autoridad del servidor.

`client/SpaceParticle.java` proporciona tres partículas con sprites propios:
`rocket_exhaust`, `rocket_smoke` y `lunar_dust`. Los viajes confirmados emiten
fuego y humo en origen y polvo en destino. Caminar en mundos con gravedad menor
que 0,4 emite polvo local de vida larga. Se respeta el ajuste de partículas del
cliente; los eventos no fuerzan renderizado a distancia.

`rocket_launch` suena al viajar; `suit_depressurize` avisa de falta de suministro
como máximo cada cinco segundos por jugador. Los sonidos usan categorías
estándar y subtítulos traducidos. `space_ambient` es una pieza original Vorbis
de 24 segundos en streaming, asignada como música de los 28 biomas nuevos, con
pausas de 600 a 1800 ticks. Respeta el volumen de música. Los efectos acompañan
el viaje instantáneo existente; no incorporan animación de vuelo.

## Compilación y comprobación

```bash
./gradlew --no-daemon build
```

`PresentationAssetsTest` valida las texturas originales referenciadas por todos
los modelos, nombres y descripciones bilingües, firmas OGG, sprites y las tres
plantillas NBT: posiciones, paleta y enlace del cofre a su botín. La profundidad
jigsaw debe ser al menos 1: en esta versión un valor 0 omite incluso la pieza
inicial. Las pruebas de progresión comprueban también los componentes nuevos.

Validación realizada: **47 pruebas aprobadas**, 660 JSON válidos, 137 PNG
originales y tres pistas OGG verificadas. Regenerar los recursos produjo los
mismos hashes. El servidor dedicado arrancó y cargó los registros sin errores
del mod. Se localizaron, colocaron y consultaron los cofres de las tres estructuras;
sus tablas generaron esquemas y materiales correctamente.

También se generaron naturalmente las tres estructuras en chunks nuevos de
la semilla de pruebas `20240613`, y se verificaron sus piezas persistidas:

| Estructura | Dimensión | Chunk inicial | Límites XYZ de la pieza |
| --- | --- | --- | --- |
| Módulo lunar | Luna | 552, 558 | 8824,39,8920 → 8832,44,8928 |
| Ruinas marcianas | Marte | 567, 553 | 9064,116,8840 → 9072,121,8848 |
| Satélite | Luna | 587, 576 | 9392,199,9204 → 9400,204,9216 |

`scripts/check-structure-chunk.mjs` permite comprobar esas piezas leyendo las
regiones Anvil guardadas, sin modificar el mundo. Los chunks forzados durante
las pruebas fueron liberados y el servidor se detuvo con guardado normal.
El entorno de pruebas mantiene el aviso de red de Yggdrasil ya documentado en
fases anteriores; no bloqueó las pruebas locales de generación.

Las comprobaciones automáticas no sustituyen una sesión gráfica: quedan por
evaluar visualmente el HUD, la mezcla de audio, las partículas y un viaje completo
con jugador. Para probarlos, abre un mundo creativo con la pestaña GalaxyCraft,
equipa el casco y lleva tanques; en supervivencia recuerda el daño por calor y
radiación de la fase anterior.

Para buscar estructuras con trucos activados:

```mcfunction
/execute in galaxycraft:moon run locate structure galaxycraft:lunar_research_module
/execute in galaxycraft:mars run locate structure galaxycraft:martian_alien_ruins
/execute in galaxycraft:moon run locate structure galaxycraft:orbital_satellite
```

El JAR distribuido está en `downloads/galaxycraft-0.2.0.jar`. No instales a la vez
las versiones 0.1.0 y 0.2.0 en la misma carpeta `mods`.
