# Validación de GalaxyCraft 0.9.0

Minecraft 1.21, NeoForge 21.0.167, Java 21.0.9 y Gradle 9.2.1.
JAR: `downloads/galaxycraft-0.9.0.jar`, **59 992 028 bytes**.
SHA-256: `6a5c7d63d78e9ab7f2a1383028e3f54ca286722d88aa563e36dd881eff156acb`.
`unzip -t` no detecta errores. La metadata expandida declara versión 0.9.0.

## Compilación y contratos

```bash
source /workspace/.toolchains/activate-galaxycraft.sh
node scripts/generate-solar-system.mjs
./gradlew --no-daemon build --console=plain
```

Resultado final: **BUILD SUCCESSFUL, 540 pruebas JUnit, cero fallos**.
Se conservan las 243 anteriores y se añaden 297 contratos: siete pruebas
generales y una prueba parametrizada para cada uno de los 290 sitios.
La prueba histórica de equipamiento se actualiza de 145 a **435 planos**
registrados: 145 de compatibilidad más 290 del catálogo activo.

Cobertura nueva:

- Diez tipos por cuerpo: 5+5, una ciudad y una megamazmorra con arena.
- Identificadores/rutas exclusivos; plantas distintas ignorando paletas,
  nombres, traslación, rotación y reflexión.
- Un solo Structure Set por mundo; todos los sets antiguos desactivados.
- Separación mínima comprobada con cinco semillas, incluyendo negativas y
  `Long.MAX_VALUE`, y regiones de coordenadas negativas/positivas.
- Codec de colocación, exclusividad dimensional y rechazo de estados desconocidos.
- Pendientes, alturas legales y cobertura del subsuelo.
- Lectura de los 8813 NBT: tamaños, estados, posiciones sin duplicar,
  paletas nativas, loot tables, planos, cofres con espacio sobre la tapa y
  exactamente un sello en cada megamazmorra, ninguno en los otros tipos.
- Structure Modifiers de NeoForge y tablas de recompensa de los custodios.

Se analizaron **5104 JSON y 36 `.mcmeta`** sin errores.
Regenerar todos los recursos produjo el mismo hash agregado antes/después:

```text
c8343d98a5d34cf38f96ea3dab746db04639aae9e1637ec3d658198bc1b329f6
```

La generación debe terminar antes de copiar recursos con Gradle: las etapas
históricas producen salidas intermedias que la etapa final reemplaza/desactiva.

## Integración real en servidor

```bash
./gradlew --no-daemon runServer -PgalaxycraftGameTests -PgalaxycraftTestWorld --console=plain
```

Servidor aislado en `run/ecology-validation`, solo `127.0.0.1:25565`, semilla
723417. Arranca y carga **1966 recetas** y todas las dimensiones. Los fallos DNS
del servicio público Yggdrasil del entorno no impiden el servidor local de prueba.
Se detuvo con `stop` y guardó las dimensiones.

La ejecución final secuencial en `(26000, 200, 26000)` produjo **31 resultados
correctos**, esperando el marcador de cada prueba antes de iniciar la siguiente:

```text
GALAXYCRAFT_INTEGRATION_OK expedition_city_full_chunks
GALAXYCRAFT_INTEGRATION_OK expedition_dimension_exclusivity
GALAXYCRAFT_INTEGRATION_OK expedition_colonists_roundtrip
GALAXYCRAFT_INTEGRATION_OK expedition_shaft_access
GALAXYCRAFT_INTEGRATION_OK expedition_warden_authority
GALAXYCRAFT_INTEGRATION_OK expedition_natural_starts
GALAXYCRAFT_INTEGRATION_OK alien_pose_authority
GALAXYCRAFT_INTEGRATION_OK native_worm_and_glider
GALAXYCRAFT_INTEGRATION_OK colony_blueprint_payment
GALAXYCRAFT_INTEGRATION_OK native_crop_rules
GALAXYCRAFT_INTEGRATION_OK colonial_trade_authority
GALAXYCRAFT_INTEGRATION_OK native_fauna_breeding
GALAXYCRAFT_INTEGRATION_OK extreme_terrain_columns
GALAXYCRAFT_INTEGRATION_OK colony_piece_population
GALAXYCRAFT_INTEGRATION_OK hunter_combat_variants
GALAXYCRAFT_INTEGRATION_OK parallel_storage_priority
GALAXYCRAFT_INTEGRATION_OK energy_cable_conservation
GALAXYCRAFT_INTEGRATION_OK oxygen_and_launchpad_pipes
GALAXYCRAFT_INTEGRATION_OK refinery_and_bulk_persistence
GALAXYCRAFT_INTEGRATION_OK unique_boss_encounters
GALAXYCRAFT_INTEGRATION_OK space_weather_and_meteors
GALAXYCRAFT_INTEGRATION_OK solar_storm_shelter
GALAXYCRAFT_INTEGRATION_OK station_round_trip
GALAXYCRAFT_INTEGRATION_OK quests_and_modules
GALAXYCRAFT_INTEGRATION_OK sealed_room_resources
GALAXYCRAFT_INTEGRATION_OK rover_authority
GALAXYCRAFT_INTEGRATION_OK exposure_and_flight
GALAXYCRAFT_INTEGRATION_OK orbital_station
GALAXYCRAFT_INTEGRATION_OK equipment_round_trip
GALAXYCRAFT_INTEGRATION_OK rocket_multiblock_cleanup
GALAXYCRAFT_INTEGRATION_OK navigation_authority
```

Las seis nuevas comprueban estados de generación vinculados a las 29
dimensiones, rechazo cruzado, starts naturales, piezas con habitantes guardadas
y procesadas de nuevo, acceso de superficie/escaleras/plantas y ancla que no
despierta en otro mundo ni repite un jefe derrotado. Las 25 previas vuelven a
validar vehículos, supervivencia, recetas, equipo, clima, misiones y navegación.

## Starts naturales y ciudad completa

No se colocaron manualmente estas plantillas para obtener los starts:

| Mundo | Chunk candidato | Estructura |
| --- | --- | --- |
| Luna | `[950, -1064]` | `moon_osario_de_los_selenitas` |
| Marte | `[947, -1085]` | `mars_palacio_de_la_reina_de_ares` |
| Encélado | `[947, -1005]` | `enceladus_ciudadela_de_damasco` |

Además se encontró naturalmente `enceladus_ciudad_de_los_penachos`, se cargaron
a estado FULL los chunks de una vivienda y se verificó el suelo real de paneles
en **`[131160, 79, 25624]`**. Tras dar tiempo a cargar entidades y considerar el
radio de movimiento, había **seis colonos** en el entorno y el marcador de la
casa constaba consumido (`Spawned: 1`). La prueba inicial de comprobación
inmediata/en una caja más pequeña se corrigió para no confundir carga o
desplazamiento con ausencia de generación.

Durante worldgen, la colisión se consulta en `WorldGenRegion`/`LevelAccessor`,
sin pedir chunks FULL recursivamente al `ServerLevel`. Los tickets temporales
del test se retiran en `finally`; al terminar:

```text
No force loaded chunks were found in galaxycraft:enceladus
```

Esto comprueba generación y entidades reales, no una captura del resultado
artístico. El test de ciudad se repitió tras guardar y reiniciar el servidor.

## Alcance y pendiente

Los diseños son composiciones NBT originales inspiradas en principios de los
mods de referencia, no assets extraídos ni 290 edificios creados manualmente.
Se conservan los dos arquetipos existentes de jefe con encuentros y recompensas
planetarios. No hay retrogen ni imposición de spacing sobre construcciones
antiguas, vanilla o de otros mods. La Tierra conserva su generación vanilla.

Faltan revisión visual con cliente, equilibrio de la exploración/recompensas
a largo plazo y mediciones de rendimiento con jugadores reales. Las pruebas
automatizadas no certifican un acabado artístico equivalente a los mods citados.
