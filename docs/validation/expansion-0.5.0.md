# Validación de la expansión 0.5.0

Entorno: Java 21.0.9, NeoForge 21.0.167, Minecraft 1.21, Gradle 9.2.1.

Artefacto: `downloads/galaxycraft-0.5.0.jar`, ZIP/JAR verificado sin errores.
SHA-256: `7c45fe78a6c0dde367048db28b7fe602246f3a9f47960e4bd33d441c932a0e7e`.

## Compilación y pruebas JUnit

`./gradlew --no-daemon build --console=plain`: **BUILD SUCCESSFUL**.
209 pruebas, sin fallos. Incluyen 7 pruebas nuevas de expansión sobre sellado,
límites del escaneo, progreso y catálogo, paquetes, 29 recursos orbitales y
persistencia de parcelas. Se conservan las 202 pruebas anteriores.

El generador completo se ejecutó repetidamente; los recursos resultantes son
deterministas y `git diff --check` no presenta errores de espacios.

## Integración dentro del servidor

Servidor de desarrollo con `-PgalaxycraftGameTests`. Las pruebas se ejecutaron
secuencialmente en el mundo de pruebas, esperando el marcador de éxito individual:

```text
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

Cobertura efectiva:

- Entregas insuficientes sin consumo, cantidades exactas, XP/recompensa una vez,
  progreso persistente y existencia de los ítems de todos los contratos.
- Migración de seis a ocho ranuras, filtros, Data Components de tanques y caída
  de las ocho piezas al morir respetando keepInventory.
- Sala cerrada con escotilla, oxígeno compartido sin casco, consumo de aire/FE,
  persistencia de más de 1000 FE y corte del suministro al abrir una fuga.
- Controles del rover limitados al piloto, rechazo de NaN/entradas fuera de
  rango, débito de batería, movimiento, progreso por distancia, persistencia de
  muestras y devolución de exactamente un vehículo al destruirlo.
- Módulo térmico que protege y se desgasta, inoperancia sin oxígeno, permiso de
  vuelo orbital transitorio, empuje vertical y retirada al activar un generador.
- Las 29 dimensiones orbitales existen; la estación generada es hermética;
  gravedad artificial y restauración de gravedad de objetos.
- Viaje planetario → estación → planeta dos veces: exige traje/kit inicialmente,
  consume un único kit, guarda el ancla y reutiliza la estación sin otra compra.
- Regresiones de plataforma/cohete, limpieza de piezas y autoridad de navegación.

El fixture de viaje utiliza ServerPlayer con canal Netty incrustado y una conexión
sin red. Esto ejecuta el cambio de dimensión y los eventos del servidor; **no**
valida pantallas ni el protocolo visual de un cliente real.

## Pendiente de validación manual

Revisar en un cliente gráfico la escala de las GUI/J/G/H, tooltips, modelo y
conducción del rover, movimientos orbitales con latencia y retorno visual de
dimensión. No se afirma haber realizado esa prueba ni una sesión multijugador
con clientes conectados. Se recomienda una copia del mundo antes de actualizar.

El servidor de desarrollo utiliza solo localhost y modo offline. Un aviso DNS al
consultar las claves públicas de Mojang no impidió el arranque ni estas pruebas.
