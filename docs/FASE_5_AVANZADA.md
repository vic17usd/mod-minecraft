# Fase 5 ampliada — GalaxyCraft 0.3.0

Minecraft **1.21**, NeoForge **21.0.167**, Java **21**. Esta ampliación reemplaza
los límites de 0.2.0 sin cambiar los identificadores de planetas, tanques ni cohetes.

## Equipamiento independiente

Pulsa **G**, usa el botón **Traje** del inventario de supervivencia o haz clic
derecho con la **Terminal de equipo espacial**. Puedes reasignar G en Controles.
No es obligatorio llevar la terminal para acceder al compartimento.

Hay **seis ranuras**: casco, pechera, pantalones, botas y dos tanques. Solo aceptan
su pieza correspondiente, con límite de uno y soporte de shift-clic. La armadura
tradicional conserva sus objetos y estadísticas. El traje se dibuja encima con
una capa propia, incluidos los tanques de la espalda; no se sustituyen objetos
del jugador para renderizarlo.

`ModEquipment` registra un attachment serializable del jugador. Las piezas se
guardan con él; el oxígeno del tanque sigue siendo el Data Component inmutable
`galaxycraft:oxygen_level`, con codecs de persistencia y red. El NBT del attachment
es almacenamiento de contenedores, no un regreso a NBT antiguo en los ítems.

El servidor sincroniza snapshots al dueño y a quienes observan al jugador, al
entrar, cambiar dimensión, reaparecer y cambiar contenido. El cliente solo puede
pedir abrir el menú; los movimientos se validan con el protocolo de contenedores.
No existe un paquete C2S para reemplazar las ranuras. Al morir se respeta
`keepInventory`: se conserva el equipo o se suelta y vacía, evitando duplicación.

Primero se consume oxígeno equipado; después mochila y mano secundaria por
compatibilidad. HUD, supervivencia y navegación consultan las mismas fuentes.
El botón de recarga solo funciona en atmósfera respirable y se valida en servidor.
Casco y suministro siguen bastando para respirar. Las cuatro piezas independientes
con suministro reducen **75 %** el daño ambiental de calor y radiación, sin añadir
una segunda estadística de armadura. Sus encantamientos no se aplican como una
segunda armadura vanilla. Las piezas también pueden equiparse en ranuras vanilla,
pero la bonificación completa requiere el compartimento independiente.

## Pestaña, herramientas y esquemas

La pestaña propia contiene separadores con nombre y color para construcción,
minerales, herramientas, traje, componentes, cohetes y esquemas. Son emblemas
renombrados mediante Data Components; no añaden registros vacíos.

El pico de titanio tiene capacidad de extracción y atributos de diamante. Se
fabrica con titanio, un diamante y un palo; se repara con diamantes. Las recetas
normales de progresión siguen disponibles. Cada estructura nueva contiene un
esquema propio que descubre una receta alternativa. Usarlo no lo consume;
**fabricar la alternativa sí consume el esquema**. Son recompensas opcionales,
no requisitos aleatorios para completar la progresión.

## Arte y cohete a escala

Los **84 bloques planetarios** tienen PNG individuales de **128×128**: roca,
regolito y mineral de cada destino. Se usan ruido multiescala periódico, grano,
fracturas, cráteres y sombreado integrado, con paletas de regolito lunar, hierro
oxidado marciano, basalto venusiano e hielos exteriores. La Tierra conserva el
Overworld; no se sustituyen globalmente sus texturas vanilla.

También se rehacen titanio, plataforma, metales del cohete, recursos, tanque,
casco, componentes, emblema, pieles de criaturas y armadura equipada. Los sprites
de ítems son de 64 px y la armadura usa capas de 128×64 con UV estándar. La capa
del sombrero es transparente para no tapar el visor, y las botas no ocultan las
perneras. Es arte procedural original con sombreado integrado; no necesita shaders
ni pretende ser un paquete PBR. No se procesan imágenes vanilla ni fotografías.

`FullScaleRocketRenderer` es un **BlockEntityRenderer** de unos **6,3 bloques** de
alto y 37 volúmenes: fuselaje segmentado, ocho paneles, anillos, cuatro aletas,
cuatro propulsores auxiliares, cabina, nariz y tobera. Los tres niveles tienen
atlas propios. La miniatura del inventario sigue usando JSON.

La reserva física es **3×3×7**, sobre una plataforma 3×3. Una raíz guarda inventario
y combustible; otras **62 piezas** reservan el volumen y aportan colisiones al
fuselaje y las aletas. No tienen BlockItem ni inventario. Su interacción abre el
menú central y romperlas desmonta un único cohete con su inventario. El combustible
no genera objetos al romperlo. Un cohete antiguo instala las piezas al recibir
ticks si hay espacio; no reemplaza construcciones cercanas. No se permite colocar
un cohete invadiendo entidades vivas.

**Encender** inicia **80 ticks / cuatro segundos** de llamas, humo, sonido,
pulsación de tobera y elevación suave. El servidor revalida jugador, distancia,
equipo, destino, base y combustible antes del traslado. Alejarse, desconectarse
o perder requisitos cancela sin gastar combustible. Reiniciar cancela el encendido;
el combustible y el inventario sí se guardan.

Después continúa el traslado entre dimensiones: no hay vuelo orbital libre ni
entidad pilotable. Se gasta combustible tras confirmar el traslado. La llegada
incluye una pasarela para aparecer fuera de las colisiones del vehículo.

## Cinco estructuras por cada cuerpo

**29 cuerpos × 5 = 145 conjuntos activos**, incluida la Tierra. Cada conjunto
tiene plantilla NBT, distribución, biomas, botín, esquema, receta y traducciones
propios. Son cinco familias arquitectónicas con materiales, desgaste y detalles
adaptados a cada cuerpo:

| Sufijo | Zona | Arquitectura |
| --- | --- | --- |
| `observatory` | Superficie | Plataforma y cúpula acristalada con instrumental |
| `wreck` | Superficie | Cráter con nave fracturada y alas |
| `sanctuary` | Superficie | Pirámide, altar y columnas monumentales |
| `geode` | Subsuelo | Cavidad elipsoidal mineralizada y entrada lateral |
| `vault` | Subsuelo | Cuatro salas, corredores y generador de criaturas |

Ejemplos: `galaxycraft:moon_geode`, `galaxycraft:mars_wreck`,
`galaxycraft:earth_observatory`, `galaxycraft:triton_vault`. La Tierra usa biomas
terrestres seleccionados del Overworld; los destinos, su bioma propio. Los gigantes
mantienen las superficies jugables ficticias de la fase 4.

El codec de `PlanetarySiteStructure` registra un tipo de estructura propio.
Se muestrean nueve puntos de la huella 21×21. Geodas y bóvedas quedan al menos
12 y 24 bloques bajo el suelo más bajo muestreado. Se rechazan ubicaciones demasiado
bajas, altas o abruptas. Los modificadores usan reglas de colocación y procesadores
reales de 1.21: `PlanetarySitePiece` rota y erosiona el 6 % de la roca local, sin
tocar cofres. No se inventa una API llamada `Structure Modifiers`.

Los tres IDs de 0.2.0 se conservan para cargar mundos antiguos, con biomas vacíos
y `frequency=0`. No se generan como un sexto conjunto ni se borran sus construcciones
existentes. Las estructuras nuevas requieren **chunks aún no generados**. La
plantilla vacía de integración tampoco es un conjunto generable.

El esquema del sitio está garantizado en el cofre; otros premios incluyen recurso
local, titanio, diamantes y combustible. Las bóvedas tienen generadores de reptadores.
Las pruebas verifican soporte y espacio de apertura de los cofres.

## Archivos y regeneración

Rutas relativas a la raíz del proyecto en IntelliJ:

| Función | Ruta |
| --- | --- |
| Equipamiento y red | `src/main/java/com/vic17usd/galaxycraft/equipment/` |
| Menú y pantalla | `menu/SpaceEquipmentMenu.java`, `client/SpaceEquipmentScreen.java` |
| Traje y cohete | `client/SpaceSuitLayer.java`, `client/FullScaleRocketRenderer.java` |
| Volumen físico | `block/RocketParts.java`, `block/RocketPartBlock.java` |
| Generación y modificadores | `worldgen/PlanetarySiteStructure.java`, `worldgen/PlanetarySitePiece.java` |
| Registros | `registry/ModEquipment.java`, `registry/ModSites.java` |
| Catálogo | `src/main/resources/expedition_sites.json` |
| Plantillas | `src/main/resources/data/galaxycraft/structure/expedition/` |
| Distribución | `src/main/resources/data/galaxycraft/worldgen/structure_set/` |
| Botín y recetas | `data/galaxycraft/loot_table/chests/expedition/`, `recipe/expedition/` bajo recursos |
| Arte y traducciones | `src/main/resources/assets/galaxycraft/` |

```bash
node scripts/generate-solar-system.mjs
./gradlew --no-daemon build
```

El generador encadena presentación y ampliación. Los archivos se incluyen en Git
y en el JAR; el usuario no necesita Node ni FFmpeg. El resultado es
`build/libs/galaxycraft-0.3.0.jar`, también distribuido en `downloads/`.

Se conservan el HUD, partículas y SoundEvents de 0.2.0. La presión sigue siendo
nominal de diseño y la altitud representa Y; no son simulación meteorológica.

## Verificación

JUnit comprueba los 145 contratos de plantillas, exactamente cinco por cuerpo,
botín, recetas, alturas y PNG, junto con los tests previos. Para integración:

Validación realizada: **195 pruebas JUnit**, **dos GameTests aprobados** y las
**145 estructuras generadas naturalmente y comprobadas en regiones guardadas**:
87 superficiales y 58 subterráneas, con una pieza válida por conjunto. Los tests
de integración también verificaron todos los ítems de la pestaña y la cancelación
del encendido al desconectar al piloto, conservando combustible.
Se comprobaron 1544 JSON y 290 PNG; regenerar los recursos no cambió sus hashes.

El [informe de regiones](validation/expedition-world-0.3.0.json) incluye los IDs,
chunks y cajas de las piezas. Se puede repetir su lectura con
`node scripts/check-expedition-world.mjs` en el mundo de desarrollo y sus logs.
La carga inicial de demasiados chunks en un tick activó el watchdog de pruebas;
la validación final se hizo en lotes pequeños confirmados, con el watchdog activo.
Se liberaron los chunks forzados y el servidor se detuvo guardando normalmente.
Persiste el aviso de red de Yggdrasil del entorno, ajeno a la generación del mod.

```bash
./gradlew --no-daemon runServer -PgalaxycraftGameTests
```

En consola, ejecuta una prueba cada vez y espera su resultado:

```text
execute positioned 12000 200 12000 run test run equipment_round_trip
execute positioned 12100 200 12000 run test run rocket_multiblock_cleanup
```

GameTests usa jugadores simulados y registros reales; NeoForge los desactiva en
producción. Comprueban restricciones, shift-clic, componentes persistidos, snapshot,
armadura tradicional, keepInventory y desmontaje. El marco prepara y limpia áreas:
ejecútalos en un mundo de desarrollo. La apariencia final de traje, HUD, animación,
mezcla de audio y el viaje completo con jugador real requieren un cliente gráfico.
