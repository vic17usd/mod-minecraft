# Validación de GalaxyCraft 0.6.0

Minecraft 1.21, NeoForge 21.0.167, Java 21.0.9, Gradle 9.2.1.

Artefacto: `downloads/galaxycraft-0.6.0.jar`, JAR verificado sin errores de ZIP.
SHA-256: `33b4b08ccd3ed64b137e4d04981574060210dfe2ee463f85c34dbf59cc8f56f8`.

## Pruebas automatizadas

`./gradlew --no-daemon build --console=plain`: **BUILD SUCCESSFUL**.

**217 pruebas JUnit sin fallos**: las 209 anteriores y ocho de Fronteras.
Se validan grafos con ciclos y más de 256 nodos, desconexiones, equilibrio sin
recirculación/desbordamientos, calendario persistente, las 28 variantes hostiles,
texturas, dos sellos en sus bóvedas NBT, drops/progresión de cohetes y la etiqueta
de terreno que los meteoritos pueden alterar. El contrato de progresión anterior
también comprueba los nuevos componentes de jefe.

Se analizaron los **1911 JSON** distribuidos y se comprobó que regenerar todos
los recursos es idempotente (mismo hash agregado antes/después). Entre las
plantillas NBT de estructuras, solo se actualizaron las dos bóvedas indicadas.

**16 GameTests aprobados secuencialmente** en servidor dedicado de desarrollo:

```text
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

Verificaciones efectivas de la nueva fase:

- Dos almacenes simultáneos por tipo abastecen a consumidores antes de
  equilibrarse entre sí, conservando la suma de FE, oxígeno y combustible.
- Un cable conectado transfiere FE; cortarlo impide la transferencia.
- El gas rellena un tanque de presurizador sin crear oxígeno ni inventar un tanque
  si la ranura está vacía.
- Las tuberías repostan el cohete a través de su plataforma, conservan combustible
  y el puerto no admite extraerlo del cohete.
- Refinería: 1 carbón + 2000 FE produce exactamente 1000 mB. Guardar/cargar y el
  menú conservan 999999 FE y 128000 unidades, sin truncarse a 16 bits. No admite agua.
- Ambos sellos crean un único jefe con atributos correctos; disparan proyectiles,
  dejan el componente exclusivo al morir, registran la derrota y no se recrean.
- Polvo y tormenta solar modifican la producción solar y la intensidad de
  radiación, y los eventos espaciales rechazan el Overworld.
- Un meteorito deposita mineral sin alterar las esquinas exteriores del cráter;
  no sustituye paneles industriales. Las referencias de estructura vacías no
  impiden indebidamente un impacto permitido.
- En Marte, una sala hermética alimentada protege de una tormenta solar; el plomo
  reduce pero no elimina la exposición exterior de ese evento.
- Se mantienen las regresiones de misiones, equipo, rover, órbitas y navegación.

## Límites de esta validación

Los tests emplean jugadores simulados y, para cambios de dimensión, un canal
Netty incrustado sin cliente gráfico. No sustituyen una revisión visual de los
dos modelos de jefe, animaciones, niebla, barras de vida, tuberías y GUI ni una
sesión multijugador real con latencia.

El balance, duración práctica de combates y la convivencia con otros mods
requieren pruebas manuales. Las capacidades se prueban contra máquinas de
GalaxyCraft, no contra todas las implementaciones externas posibles.

El servidor de pruebas solo escucha en localhost. El aviso DNS de las claves
públicas de Mojang del entorno offline no impidió el arranque ni las pruebas.
