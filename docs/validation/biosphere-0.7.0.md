# Validación de GalaxyCraft 0.7.0

Minecraft 1.21, NeoForge 21.0.167, Java 21.0.9 y Gradle 9.2.1.

Artefacto: `downloads/galaxycraft-0.7.0.jar`, JAR/ZIP verificado sin errores.
SHA-256: `5d110ec4efaeaa30ae0bbfe850f83c30a6a3fb6ec24197eb1404e77b57150f4c`.

## Compilación y contratos

`./gradlew --no-daemon build --console=plain`: **BUILD SUCCESSFUL**.
225 pruebas JUnit: las 217 anteriores y ocho nuevas. Se verifican perfiles,
identidades/semillas, límites y determinismo del relieve, materiales/cultivos,
fachadas de las 145 plantillas, colonias/biomas, pieles y discos celestes,
persistencia de contratos y codec del paquete de acciones.

Los contratos de generación anteriores se actualizan para esperar el nuevo
ChunkGenerator/BiomeSource, conservando sus comprobaciones de minerales, loot,
recursos y compatibilidad del Overworld. No se eliminan esas verificaciones.

Los 3793 JSON se analizaron correctamente. Regenerar los recursos es idempotente:
se obtuvo el mismo hash agregado antes y después de ejecutar el generador completo.

## Servidor y GameTests

Mundo nuevo aislado en `run/ecology-validation`, semilla 723417, servidor
escuchando únicamente en localhost. Las 22 pruebas se ejecutaron secuencialmente,
esperando su marcador individual:

```text
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

Cobertura nueva efectiva:

- Rechazo de cultivo lunar en el Overworld, crecimiento/madurez en suelo lunar,
  cosecha única por maduración y reinicio de edad.
- Pago exacto de comercio, tanque recibido, stock persistente, aceptación y
  entrega de contrato una sola vez, fichas utilizables, distancia y sesión cerrada.
- Dietas nativas, reproducción del mismo tipo, rechazo entre planetas/especies,
  descendencia correcta y enfriamiento de esquila.
- Las 28 dimensiones tienen el generador extremo y tres biomas; sus columnas y
  alturas coinciden. Se generan chunks completos en cuatro perfiles representativos.
- Las piezas de casa y huerto crean sus NPC/cofre/fauna y no repiten apariciones
  al procesarse otra vez.
- Escupidor con proyectil/enfriamiento, salto sobre suelo físico y quemadura del
  cazador de magma.
- Regresiones completas de supervivencia, equipo, misiones, vehículos, jefes,
  meteoritos, energía y navegación de las fases anteriores.

Los tests finales se colocaron en `(26000, 200, 26000)`. Un área anterior contenía
referencias a una estructura vanilla; correctamente rechazaba el impacto de
meteorito. Se repitió la prueba positiva en un área libre sin debilitar la
protección de estructuras.

## Asentamiento y generación naturales

Se ejecutó, sin colocar una plantilla de pueblo manualmente:

```text
execute in galaxycraft:moon positioned 0 100 0 run locate structure galaxycraft:colony_moon
```

Resultado: colonia próxima a `[-320, ~, 192]`. Al cargar temporalmente su área
aparecieron **cinco colonos**, cada uno con `Stock: 8`, y fauna lunar. Las cargas
forzadas temporales se retiraron después. También se generaron chunks nuevos en
`[1600,1600]` de Luna, Marte, Venus y Encélado, con estos marcadores de disponibilidad:

```text
GC_NEW_MOON_READY
GC_NEW_MARS_READY
GC_NEW_VENUS_READY
GC_NEW_ENCELADUS_READY
```

Estos checks prueban carga/generación y población real, no una inspección visual.

## Pendiente y límites

No se ejecutó un cliente gráfico ni una sesión multijugador real. Modelos,
skyboxes, niebla, animaciones, GUI y economía requieren revisión manual. Los
jugadores de los tests son simulados; los cambios de dimensión usan una conexión
de prueba sin red. No se promete migración automática del terreno antiguo ni
compatibilidad visual con todos los paquetes de shaders/mods externos.

El aviso DNS de claves públicas de Mojang del entorno offline no impidió el
arranque ni las pruebas. Los resultados no lo presentan como fallo del mod.
