# Validación de GalaxyCraft 0.8.0

Minecraft 1.21, NeoForge 21.0.167, Java 21.0.9, Gradle 9.2.1.

Artefacto: `downloads/galaxycraft-0.8.0.jar`, comprobado con `unzip -t`.
SHA-256: `6902cabf51568ade63f1035337fdae623e7cb8bc6b4bac53812c931cc25f94d3`.
La metadata del JAR declara `galaxycraft`, versión `0.8.0`.

## Compilación y pruebas unitarias

```bash
source /workspace/.toolchains/activate-galaxycraft.sh
node scripts/generate-solar-system.mjs
./gradlew --no-daemon build --console=plain
```

Resultado final: **BUILD SUCCESSFUL**, **243 pruebas, cero fallos**.
Las 225 pruebas previas se conservan; los contratos de tamaño de atlas de
criaturas se actualizan de 64/128 a 256. Las 18 nuevas verifican:

- Catálogo del Sol y 29 cuerpos, identidad, padres y parámetros orbitales.
- Solución numérica de Kepler, periodos, distancias y posiciones finitas,
  incluyendo tiempos negativos y lejanos.
- Diámetro angular Tierra–Luna y Sol–Tierra; fases llena/nueva/cuarto.
- Transformación local con norma conservada y vertical coherente; hemisferio
  lunar cercano dirigido a la Tierra.
- Transmitancia de vacío y medios planetarios; fases Rayleigh/Mie y reloj S2C.
- Horneado real de once esqueletos Java con al menos 30 partes cada uno,
  diversidad de articulaciones y ausencia de mallas vanilla en renderizadores.
- Cobertura de los 179 registros con atlas únicos de 256 y máscaras emisivas.
- Envolventes de poses, duración válida y round-trip de ambos codecs de red.
- Mapas equirectangulares HD y seis caras diferentes del cubemap.

Se analizaron **3797 JSON y 36 archivos `.mcmeta`** sin errores.
La generación completa es idempotente, sin depender de la red: hash agregado
de todos los recursos antes/después:

```text
1367da68dbb782accbcdace7ffd0c3c7f63c481e2e33f48baa4f4b5191f008cd
```

Generar y compilar se ejecutan secuencialmente: las etapas intermedias del
generador no son un resource pack final y no deben copiarse a mitad de ejecución.

## Servidor dedicado e integración

```bash
./gradlew --no-daemon runServer -PgalaxycraftGameTests -PgalaxycraftTestWorld --console=plain
```

Arrancó en `127.0.0.1:25565`, reutilizando el mundo aislado de validación
`run/ecology-validation` de 0.7.0. Registró dimensiones y cargó las criaturas sin
dependencias gráficas en servidor. El error DNS al solicitar claves públicas
Yggdrasil del entorno no impidió iniciar este servidor local de pruebas.
Se detuvo con `stop` y guardó todos los niveles correctamente.

Se ejecutaron individualmente las 25 pruebas en `(26000, 200, 26000)`, esperando
el marcador correspondiente antes de iniciar la siguiente:

```text
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

Las nuevas prueban inicio autoritativo de poses y su no persistencia, embestida
sin daño de bloques, salto/planeo con caída acotada, pago/stock exactos de planos
y rechazo de sesiones cerradas. Las anteriores vuelven a comprobar pueblos,
cultivos, cría, cazadores, terreno, jefes, energía, meteoritos y supervivencia.

## Shader con OpenGL real, sin ventana

```bash
python3 scripts/check-atmosphere-gl.py
```

EGL surfaceless, **OpenGL 4.5 Core, Mesa 25.0.7, llvmpipe LLVM 19.1.7**.
Se compilan y enlazan los archivos `.vsh/.fsh` de distribución y se dibuja un
triángulo de prueba. Se verifica ausencia de errores GL y se leen píxeles RGBA:

```text
vacío: (3, 3, 8, 255)
día:   (17, 33, 67, 255)
noche: (3, 3, 7, 255)
```

El caso de vacío conserva el fondo; la atmósfera diurna dispersa más azul y la
nocturna es más oscura. Es una comprobación del shader en renderizador software,
**no una partida de Minecraft ni una medida de FPS o calidad en GPU del usuario**.

## Límites de lo comprobado

No se ejecutó un cliente de Minecraft gráfico ni una conexión multijugador
real. Falta revisar siluetas, UV, transparencias, interacciones de animaciones,
rendimiento, fases a lo largo de una sesión y compatibilidad con otros mods
gráficos. El arte es procedural, con costas Natural Earth de dominio público;
no es fotografía de satélite. Las órbitas medias y las fases iniciales de varios
satélites son aproximaciones, no una efeméride actual. No se certifica acabado
hiperrealista mediante pruebas automatizadas. Véase la guía para probarlo.
