# GalaxyCraft 0.8.0 — morfologías y cielos

Minecraft **1.21 exacto**, NeoForge **21.0.167**, Java **21**. Instalar la misma
versión en cliente y servidor; retirar el JAR anterior y respaldar el mundo.
No necesita GeckoLib, Curios ni paquetes externos de shaders.

## Criaturas originales

Se eliminan los renderizadores basados en Silverfish/Husk. Tampoco se usan
modelos de vaca, oveja, cerdo, zombi o aldeano para ninguna criatura del mod.
`OriginalAlienModel` construye las geometrías con el sistema Java nativo 1.21,
`MeshDefinition`, `PartDefinition`, `LayerDefinition` y `ModelPart`.
Son **once esqueletos**, no 179 esqueletos enteramente independientes:

| Familia | Morfología y movimiento |
| --- | --- |
| Hexápodo cristalino | Seis patas con cadera/rodilla/tobillo/dedos; placas y sensores. |
| Biomasa luminiscente | Cinco estratos pulsantes, núcleo y ocho tentáculos articulados. |
| Caminante de placas | Seis patas inversas, coraza segmentada y aletas dorsales. |
| Gusano de regolito | Doce segmentos en cadena, cilios y cuatro mandíbulas. |
| Raptor de membrana | Costillas alares, membranas translúcidas, cola y patas prensiles. |
| Lancero cristalino | Ocho patas, espinas, cola y mandíbulas de ataque. |
| Escupidor glandular | Seis patas, cuatro sacos luminosos y aparato bucal articulado. |
| Coloso de magma | Cuatro patas robustas, placas fisuradas y cola de roca. |
| Sabio trípode | Tres patas, cuatro brazos con dedos y cinco sensores oculares. |
| Guardián lunar | Cuatro apoyos, reactor estriado, brazos de cañón y bobinas. |
| Reina marciana | Doce patas, abdomen en cadena, corona y seis ojos pedunculados. |

Los 179 registros conservan sus identificadores. Los sufijos históricos
`crystal_bovine`, `lumen_sheep`, `plated_boar` son compatibilidad de guardado,
no indican que se utilice geometría de esos animales vanilla. Hay variaciones
de placas/espinas y atlas individuales de **256×256**, con máscaras emisivas.
La emisión ilumina la criatura, no añade luces dinámicas al mundo.

La marcha se interpola con el movimiento nativo. Las poses de ataque, disparo,
embestida, planeo, rugido, alimentación y comercio las inicia el servidor y se
envían a los jugadores que siguen la entidad. Se reinician al cargar: son
visuales, no progreso persistente. No hay un paquete C2S para elegir ataques.

El gusano embiste sobre el suelo sin convertir bloques en piedra infestada ni
romper construcciones. Su animación de enterramiento **no excava túneles**.
El raptor salta hacia objetivos visibles y limita la caída al planear; conserva
colisión y navegación terrestre. **No es un volador con navegación aérea 3D**.
Los cazadores conservan sus saltos, proyectiles/enfriamientos y ataques ígneos;
los jefes mantienen barra de vida, ataques, anclas y recompensas existentes.

## Colonias y flora

Se mantienen las 29 familias de pueblos procedurales, sus casas, huertos,
habitantes, cofres y población finita por pieza. Se encuentran en terreno nuevo:

```mcfunction
/execute in galaxycraft:moon run locate structure galaxycraft:colony_moon
/execute in galaxycraft:mars run locate structure galaxycraft:colony_mars
```

Clic derecho en un sabio abre comercio y contrato secundario nativo. La nueva
opción **Plano tecnológico** cuesta cuatro fichas y entrega el esquema del
observatorio del cuerpo correspondiente; consume una existencia. El servidor
comprueba sesión, distancia, dimensión, inventario y stock. No se pueden cobrar
de nuevo contratos completados. Se conserva el stock compartido, guardado y
reposición de las fases anteriores.

Los 87 cultivos siguen necesitando dimensión y suelo nativos y tienen tres
edades. Los hongos, abanicos de esporas y corales maduros emiten ahora esporas
emisivas con movimiento suave y desvanecimiento. Las partículas no dan cosechas
ni cambian el crecimiento. La guía de [biosferas](BIOSFERA_Y_CIVILIZACIONES.md)
detalla dietas, recolección, ganadería y generación.

## Cielos y astronomía

`PlanetSkyEffects` reemplaza los discos decorativos anteriores en las 28
superficies extraterrestres. `OrbitalEffects` utiliza el mismo catálogo en las
29 estaciones, sin atmósfera y con el cuerpo anfitrión bajo la estación.
El Overworld conserva su cielo vanilla para evitar interferir con otros mods.

- Mapas equirectangulares de **1024×512** por cuerpo y para el Sol; Tierra
  **2048×1024**, con océanos, costas geográficas, continentes, hielos, nubes y
  mapa nocturno artístico. Son texturas procedurales, **no fotos de satélite**.
- Seis caras **1024×1024** de espacio profundo con banda de Vía Láctea,
  nebulosas y agrupaciones de estrellas, muestreadas por dirección.
- Radios, distancias, excentricidades, inclinaciones y periodos en km/días.
  Se resuelve Kepler y se componen órbitas del planeta y sus satélites.
- Cada disco se ilumina según su orientación respecto al Sol y al observador:
  creciente, cuarto, llena y cara nocturna, con giro de la textura. Se dibujan
  todos los cuerpos resolubles por tamaño angular, no solo el primer vecino.
  Fobos y Deimos tienen posiciones y periodos diferentes.
- Anillos de Saturno inclinados, estelas cosméticas periódicas y destellos
  durante tormentas solares. Los impactos reales siguen siendo del servidor.

El diámetro angular se obtiene de `2 asin(radio/distancia)`. La Tierra desde
la Luna mide aproximadamente **1,9°**, no la mitad del cielo. Un catalejo permite
apreciar los detalles; si se desea una presentación exagerada, existe una opción
gráfica explícita. Las lunas tienen una aproximación de acoplamiento de marea;
el origen lunar se sitúa en el hemisferio que ve la Tierra.

La referencia temporal es J2000 (JD 2451545), con un día terrestre por día de
Minecraft por defecto. Se extrapola un snapshot periódico del reloj del
Overworld enviado por el servidor; se actualiza al entrar/cambiar de dimensión
y cada 100 ticks. `/time set` se refleja en el siguiente snapshot.
No consulta la fecha del PC ni servicios externos durante la partida.

**Precisión:** son órbitas keplerianas medias aproximadas. Las fases iniciales
de varios satélites menores son referencias artísticas, no observaciones.
No es una efeméride para la fecha actual, no integra perturbaciones, precesión,
libración real, tiempo de viaje de la luz o dispersión múltiple. La orientación
de ejes es simplificada. El ciclo de luz y la aparición de mobs de Minecraft
**no se recalculan** según el periodo astronómico; el reloj físico es visual.
Las atmósferas, mundos habitables y fauna de gigantes gaseosos siguen siendo
licencias de juego del catálogo existente.

Referencias de magnitudes: [JPL, elementos aproximados](https://ssd.jpl.nasa.gov/planets/approx_pos.html)
y [NASA, fichas planetarias](https://nssdc.gsfc.nasa.gov/planetary/factsheet/).
Las costas de dominio público Natural Earth se incluyen con
[procedencia y términos](../scripts/assets/NATURAL_EARTH.md).

## Dispersión y rendimiento

El shader GLSL 150 integra dispersión simple en una atmósfera esférica con
densidad exponencial, fase Rayleigh y fase Mie/Henyey–Greenstein. Hay medios
distintos para Marte, Venus y Titán, vacío para cuerpos sin atmósfera y un perfil
base para las restantes atmósferas del juego. La Luna no recibe bruma azul:
solo hay desvanecimiento negro cerca del límite de distancia de dibujado.
Se respetan inmersión, ceguera/oscuridad y la niebla de tormentas existente.
Los coeficientes se calibran visualmente, no son una simulación climática medida.

En `config/galaxycraft-client.toml`:

```toml
# 0 desactiva dispersión; 2 bajo, 4 normal, 8 más muestras.
atmosphereSamples = 4
# 1.0 mantiene tamaños físicos; hasta 8.0 es ampliación artística.
celestialMagnification = 1.0
```

En `<mundo>/serverconfig/galaxycraft-orbits-server.toml`:

```toml
orbitalDaysPerMinecraftDay = 1.0
```

Si falla la carga del shader, se registra el error y queda el cubemap sin
dispersión. F3+T recarga recursos y shader. No se ha comprobado compatibilidad
con Iris/Oculus ni paquetes de shaders; probar primero sin ellos. Las capas
translúcidas pueden necesitar ajuste artístico tras revisión dentro del juego.

## Archivos y validación

Todos los caminos son relativos a la raíz abierta en IntelliJ:

- `src/main/java/com/vic17usd/galaxycraft/morphology/`: familias, poses y red S2C.
- `src/main/java/com/vic17usd/galaxycraft/astronomy/`: órbitas, reloj y óptica.
- `src/main/java/com/vic17usd/galaxycraft/client/OriginalAlien*`: mallas y capas.
- `src/main/java/com/vic17usd/galaxycraft/client/PlanetSkyEffects.java`,
  `OrbitalEffects.java`, `AtmosphereShader.java`: cielo y enlace del shader.
- `src/main/resources/assets/galaxycraft/shaders/core/atmosphere.*`: GLSL/JSON.
- `src/main/resources/assets/galaxycraft/textures/entity/`: atlas y emisión.
- `src/main/resources/assets/galaxycraft/textures/astronomy/`: mapas celestes.
- `src/main/resources/celestial_orbits.json`, `morphology_manifest.json`: catálogo
  generado y cobertura de recursos. No editar salidas generadas a mano.
- `scripts/generate-morphology.mjs`: generación determinista integrada al final
  de `node scripts/generate-solar-system.mjs`, sin descargas durante la generación.

Terminar la generación antes de ejecutar Gradle: las etapas del generador
reescriben recursos intermedios que todavía no representan la salida final.

`./gradlew build` ejecuta **243 pruebas unitarias**. Se comprobaron también
**25 GameTests** de servidor y compilación/enlace/dibujo del shader con Mesa
mediante `python3 scripts/check-atmosphere-gl.py`.
Véase el [informe reproducible](validation/morphology-0.8.0.md).

**Pendiente:** revisión visual con un cliente real, animaciones en multijugador,
FPS en hardware del jugador y compatibilidad gráfica externa. Las pruebas
automáticas no certifican un acabado hiperrealista ni equivalencia artística con
Alex's Mobs, Mowzie's Mobs o Twilight Forest; no se han copiado sus recursos.

### Comprobación manual recomendada

En creativo, con trucos y copia de seguridad:

```mcfunction
/execute in galaxycraft:moon run tp @s 0 180 0
/summon galaxycraft:moon_crystal_bovine ~3 ~ ~
/summon galaxycraft:moon_lumen_sheep ~6 ~ ~
/summon galaxycraft:moon_colonist ~9 ~ ~
/summon galaxycraft:moon_crawler ~12 ~ ~
/execute in galaxycraft:mars run tp @s 0 180 0
/summon galaxycraft:mars_raider ~5 ~ ~
```

Elegir una zona segura del suelo para las criaturas: los comandos de ejemplo
no buscan automáticamente altura de superficie. Revisar patas/rodillas,
transparencia, brillo y comercio con clic derecho. Mirar la Tierra desde la
Luna con catalejo; en Marte observar ambos satélites a distintas horas.
Revisar el anfitrión desde una estación, la bruma de Venus y los cielos negros
en vacío. En supervivencia los depredadores son peligrosos: no usar el mundo
principal para comprobar combate y jefes.
