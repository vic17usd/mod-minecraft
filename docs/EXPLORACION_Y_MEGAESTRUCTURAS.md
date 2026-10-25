# GalaxyCraft 0.9.0 — exploración desolada y megaestructuras

Minecraft **1.21**, NeoForge **21.0.167**, Java **21**. Instalar el mismo JAR
en cliente y servidor, retirando el anterior y respaldando el mundo.

## Catálogo estricto

Hay **290 tipos nuevos: exactamente diez por cada uno de los 29 cuerpos**,
incluida la Tierra/Overworld. Cada cuerpo tiene:

| Superficie: cinco | Subsuelo: cinco |
| --- | --- |
| Ciudad habitada | Mina ramificada |
| Fortaleza amurallada | Complejo industrial |
| Templo monumental | Catacumbas de tres niveles |
| Puerto/enclave expedicionario | Archivo subterráneo |
| Observatorio | Megamazmorra central de tres niveles y arena |

Los nombres, plantas, rutas NBT, paletas y botines se asignan a un solo mundo.
Las plantas se contrastan incluso ignorando nombre y materiales, normalizando
rotaciones y reflexiones: no se admite presentar una misma planta recoloreada
como otro diseño. Las plantas son composiciones procedurales originales,
no 290 edificios modelados manualmente.

El [catálogo completo](CATALOGO_EXPLORACION.md) enumera nombres e identificadores.
Son diez **tipos**, no un límite de diez apariciones en un mundo infinito:
un tipo puede encontrarse varias veces en su propio planeta, nunca generarse
naturalmente en otro. Las reglas cuentan estructuras de **GalaxyCraft**; no
eliminan estructuras vanilla del Overworld ni las de otros mods instalados.

Las ciudades son una de las diez entradas, no una undécima estructura aparte.
Las cinco expediciones antiguas y la colonia antigua por cuerpo tienen
`frequency: 0`. Sus registros, NBT, planos y recetas se conservan para poder
leer estructuras, cofres e inventarios anteriores sin borrar datos.

## Arquitectura y referencias

La inspiración se toma de principios arquitectónicos, sin copiar NBT, código,
modelos ni recursos protegidos de los mods citados y sin exigir instalarlos:

- **When Dungeons Arise:** composiciones extensas, murallas, torres, patios,
  puertas exteriores y ciudades de varios edificios.
- **YUNG's Better Structures:** minas ramificadas, niveles conectados,
  refuerzos, archivos, cámaras laterales y accesos transitables.
- **IDAS:** integración de habitaciones de distintas funciones, decoración,
  recursos y rutas de exploración dentro de un mismo complejo.
- **L_Ender's Cataclysm:** recorrido de megamazmorra y arena central amplia,
  pilares, altar y encuentro persistente de jefe.

Cada cuerpo define un tema propio: cúpulas selenitas, helióstatos mercurianos,
zigurats de basalto en Venus, terrazas oxidadas marcianas, arcos glaciares de
Europa, refinerías de Titán, murallas bicolores de Jápeto, ejes inclinados de
Urano o espirales retrógradas de Tritón. La gramática de salas, techos, soportes
y plantas añade diferencias geométricas, además de las paletas nativas.

Los complejos alcanzan **177×177 bloques** de extensión; los módulos abarcan
hasta 36 bloques verticales, además de apoyos y acceso al terreno. Las arenas
son de **51×51×20**. Hay **8813 módulos NBT** con rutas exclusivas del diseño.
Son piezas de un mismo `StructureStart`, no estructuras adicionales ni starts
independientes que puedan aparecer juntos para eludir la rareza.

Se utiliza ensamblado NBT con `Structure`, `TemplateStructurePiece` y piezas
serializables propias. No hay dependencia de Jigsaw ni crecimiento ilimitado
de pools. El uso de NBT satisface el formato de plantillas de esta actualización.
Los NBT pueden abrirse con un editor compatible para una revisión artística.

## Distancias reales entre descubrimientos

Cada cuerpo usa **un único Structure Set compartido por sus diez tipos**.
Así no existen diez rejillas independientes cuyos candidatos puedan coincidir.

```json
{
  "structures": [
    {"structure": "galaxycraft:moon_selenopolis_de_copernico", "weight": 1},
    {"structure": "galaxycraft:moon_muralla_de_tycho", "weight": 1}
  ],
  "placement": {
    "type": "galaxycraft:desolate_spread",
    "body": "moon",
    "spacing": 384,
    "separation": 192,
    "salt": 5808046
  }
}
```

El ejemplo abreviado muestra dos entradas; el archivo real contiene diez.
`spacing` y `separation` están en **chunks**, no bloques:

- Regiones de **6144×6144 bloques**, como máximo un candidato por región.
- Candidatos de regiones vecinas a **al menos 3088 bloques** de distancia.
- Con el radio máximo admitido de 112 bloques, queda un margen conservador
  de **más de 2800 bloques entre huellas** de complejos nuevos.
- Todos los tipos tienen peso uno. El terreno puede rechazar un candidato;
  no se garantiza una aparición en cada región ni una distancia máxima para
  encontrar un tipo concreto. Buscar la megamazmorra puede requerir kilómetros.

La separación se mantiene aunque dos candidatos sean de tipos distintos.
El terreno se consulta antes de aceptar un start: pendientes superficiales
de hasta 32 bloques, ausencia de líquidos en los apoyos muestreados, límites
verticales y cobertura del subsuelo. Los apoyos evitan dejar plataformas sin
soporte sobre depresiones pequeñas. No se nivela indiscriminadamente un planeta.

Los archivos se encuentran en
`src/main/resources/data/galaxycraft/worldgen/structure_set/expeditions_<cuerpo>.json`.
Para ajustar parámetros de forma permanente, modificar el generador, no sus
salidas; un datapack también puede sobrescribirlos. Salts y límites de codec
se validan. No usar diez sets separados para reducir distancias accidentalmente.

## Exclusividad dimensional y Structure Modifiers

Los biomas son un primer filtro, no la garantía de exclusividad.
`ExpeditionDimensions` vincula cada `ChunkGeneratorStructureState` y
`RandomState` con su dimensión durante `LevelEvent.Load`. La colocación y el
`findGenerationPoint` rechazan contextos de otro mundo, aunque otro datapack
copie el mismo bioma. Un contexto no vinculado se rechaza por seguridad.
Las piezas vuelven a comprobar el mundo antes de colocar bloques o habitantes.

`DesolatePlacement` extiende el colocador aleatorio nativo para conservar el
funcionamiento de `/locate`; no usa búsquedas de vecinos que carguen chunks
durante cada tick del jugador. No se envían semillas ni decisiones de generación
desde el cliente.

Los **Structure Modifiers reales de NeoForge** están en
`data/galaxycraft/neoforge/structure_modifier/expedition_v2/` y usan
`neoforge:add_spawns` sobre tags exclusivos de cada cuerpo. Añaden sus
acechadores nativos a fortalezas, templos y mazmorras. Las ciudades reservan una
lista de hostiles vacía. Los modifiers no deciden el spacing: eso corresponde
al Structure Set y a su colocador, no a un biome modifier.

## Subsuelo, habitantes y jefes

Las mazmorras tienen recibidor y un pozo de acceso de 5×5 hasta la superficie,
escalera continua y puertas a cada nivel de doce bloques. La entrada superficial
se abre a la cota del terreno, no obliga a excavar a ciegas para encontrarla.
La colocación de cada pieza se limita al chunk procesado y a la altura legal.

Las ciudades crean sabios trípodes nativos y fauna. Los marcadores consumidos
se guardan en la pieza, de modo que guardar/recargar o volver a procesar la pieza
no duplique los habitantes. Se conservan comercio, stock, contratos y dietas.
El comercio de planos ahora entrega el observatorio del catálogo nuevo.

Cada megamazmorra contiene **un único sello de jefe** con `EncounterBody`.
La ancla valida la dimensión y persiste UUID, despertar y derrota. Descargar
la zona, reiniciar o entrar otra vez no genera otro jefe. No se confunde
"jefe descargado" con "jefe muerto". En pacífico no se despiertan encuentros.

Se reutilizan los **dos arquetipos originales de jefe existentes** —guardián y
reina— con nombre, atributos y botín del cuerpo. No se añaden 29 modelos nuevos
de jefe en esta fase de estructuras. El jefe tiene barra de vida, ataques
existentes y restricción al altar; en las nuevas arenas hay espacio de combate.

## Recompensas y progresión

Cada uno de los 290 tipos tiene su propia loot table y plano; las cámaras
contienen minerales nativos, titanio, fichas, suministros y recetas alternativas.
El plano se consume al fabricar. El botín del custodio usa una tabla planetaria
separada y da el plano de la megamazmorra y materiales de encuentro.

Se mantiene la progresión original: el núcleo lunar y el corazón marciano
siguen dependiendo de sus respectivos jefes. Los custodios de otros mundos no
entregan núcleos de navegación avanzada directamente. Las recetas alternativas
de ese núcleo también exigen el corazón de la reina marciana; no desbloquean
cohetes superiores solo por encontrar un cofre terrestre.

## Encontrarlas y migrar

La escala favorece rover, combustible, módulos del traje, reservas de oxígeno y
refugios. No se añade un escáner que cargue regiones remotas permanentemente.
Con trucos habilitados, para validar en creativo:

```mcfunction
/execute in galaxycraft:moon positioned 0 100 0 run locate structure #galaxycraft:expedition_v2/moon
/execute in galaxycraft:moon run locate structure galaxycraft:moon_catedral_del_guardian_gris
/execute in galaxycraft:mars run locate structure galaxycraft:mars_palacio_de_la_reina_de_ares
/execute in galaxycraft:venus run locate structure galaxycraft:venus_palacio_de_la_caldera_eterna
```

`/locate` puede tardar al buscar tipos muy raros; no hacerlo repetidamente en un
servidor en uso. Elegir altura segura antes de teletransportarse: la coordenada
Y sugerida por el localizador no es una promesa de aterrizaje seguro.

**No hay retrogen ni borrado del mundo.** Las construcciones ya generadas se
conservan, incluso sus jefes, cofres y estructuras antiguas. Explorar chunks
nuevos activa el nuevo sistema, pero los starts anteriores que ya estaban
guardados pueden seguir completando sus piezas. La garantía de diez tipos
activos y separación corresponde a los starts nuevos de 0.9.0. Para una prueba
sin mezcla de legado, crear un mundo nuevo; no se promete imponer separación
retroactiva alrededor de construcciones antiguas o de otros mods.

## Desarrollo y comprobaciones

Rutas relativas a la carpeta raíz de IntelliJ:

- `scripts/expedition-themes.mjs`: diez títulos y tema por cuerpo.
- `scripts/generate-expeditions.mjs`: plantas, NBT, tablas, planos, sets y modifiers.
- `src/main/java/com/vic17usd/galaxycraft/expedition/`: catálogo, codecs,
  exclusividad dimensional, colocador y piezas de acceso/ensamblado.
- `src/main/resources/expeditions_v2.json`: manifiesto completo generado.
- `src/main/resources/expedition_generation.json`: resumen y huellas geométricas.
- `data/galaxycraft/structure/expedition_v2/<id>/`: módulos NBT.
- `data/galaxycraft/loot_table/chests/expedition_v2/`: botines de cámaras.
- `data/galaxycraft/loot_table/entities/expedition_wardens/`: recompensas de jefes.
- `data/galaxycraft/recipe/expedition_v2/`: recetas alternativas con planos.

```bash
node scripts/generate-solar-system.mjs
./gradlew --no-daemon build --console=plain
```

Esperar a que termine el generador antes de ejecutar Gradle. Está integrado
al final de la cadena histórica, para que sus etapas anteriores no reactiven
colonias o sets obsoletos. Es reproducible y no requiere descargar los mods
de referencia. No editar cientos de salidas generadas a mano.

Se validaron **540 pruebas unitarias y 31 GameTests**; consultar el
[informe reproducible](validation/expeditions-0.9.0.md).
La comprobación automatizada cubre catálogo, exclusividad, geometría, separación,
NBT, cofres, recetas, generación y persistencia. La revisión artística dentro
del juego, el equilibrio de recompensas a largo plazo y el rendimiento de
exploración con jugadores reales siguen siendo necesarios. No se afirma que
una prueba de compilación certifique el acabado visual de WDA/YUNG/IDAS/Cataclysm.
