# Fase 4: arquitectura del Sistema Solar

Implementación para Minecraft **1.21 exacto**, NeoForge **21.0.167** y Java **21**.
El catálogo contiene **29 cuerpos**: Tierra, los otros siete planetas, la Luna y
20 lunas adicionales seleccionadas. La Tierra es `minecraft:overworld`: se conserva
el mundo existente, sin duplicarlo ni volver a generar sus chunks.

Se incluyen las lunas mayores enumeradas abajo, no los cientos de satélites
menores conocidos. Júpiter, Saturno, Urano y Neptuno tienen terrenos **ficticios
explorables**, porque no poseen una superficie sólida accesible como la terrestre.
Los materiales, colores y criaturas son adaptaciones de juego. Esta arquitectura
no es una simulación astronómica de órbitas, presión, meteorología ni química.

## Catálogo, jerarquía y física

La raíz del sistema es `sun` (el Sol no es un destino). `parent` organiza planetas
y satélites. Los identificadores de la tabla, salvo `earth`, corresponden a
`galaxycraft:<id>`. La gravedad es relativa a la Tierra. Calor/radiación son
puntos de daño cada cinco segundos de juego, no grados Celsius ni dosis reales.

| Cuerpo / identificador | Orbita alrededor de | Gravedad relativa | Atmósfera | Nivel mínimo | Calor/radiación |
| --- | --- | ---: | --- | ---: | --- |
| Tierra (`earth`) | sun | 1 | Respirable | 0 | 0/0 |
| Luna (`moon`) | earth | 0.165 | Sin atmósfera | 1 | 0/1 |
| Mercurio (`mercury`) | sun | 0.378 | Sin atmósfera | 2 | 3/2 |
| Venus (`venus`) | sun | 0.904 | CO₂ no respirable | 2 | 4/0 |
| Marte (`mars`) | sun | 0.38 | CO₂ no respirable | 2 | 0/1 |
| Fobos (`phobos`) | mars | 0.0006 | Sin atmósfera | 2 | 0/1 |
| Deimos (`deimos`) | mars | 0.0003 | Sin atmósfera | 2 | 0/1 |
| Júpiter (`jupiter`) | sun | 2.528 | Hidrógeno no respirable | 3 | 0/4 |
| Ío (`io`) | jupiter | 0.183 | Dióxido de azufre | 3 | 3/3 |
| Europa (`europa`) | jupiter | 0.134 | Sin atmósfera | 3 | 0/3 |
| Ganímedes (`ganymede`) | jupiter | 0.146 | Sin atmósfera | 3 | 0/2 |
| Calisto (`callisto`) | jupiter | 0.126 | Sin atmósfera | 3 | 0/1 |
| Saturno (`saturn`) | sun | 1.065 | Hidrógeno no respirable | 3 | 0/2 |
| Titán (`titan`) | saturn | 0.138 | Nitrógeno/metano | 3 | 0/0 |
| Encélado (`enceladus`) | saturn | 0.011 | Sin atmósfera | 3 | 0/0 |
| Mimas (`mimas`) | saturn | 0.0064 | Sin atmósfera | 3 | 0/1 |
| Tetis (`tethys`) | saturn | 0.0148 | Sin atmósfera | 3 | 0/0 |
| Dione (`dione`) | saturn | 0.0237 | Sin atmósfera | 3 | 0/0 |
| Rea (`rhea`) | saturn | 0.0269 | Sin atmósfera | 3 | 0/0 |
| Jápeto (`iapetus`) | saturn | 0.0227 | Sin atmósfera | 3 | 0/0 |
| Urano (`uranus`) | sun | 0.886 | Hidrógeno no respirable | 3 | 0/1 |
| Ariel (`ariel`) | uranus | 0.0275 | Sin atmósfera | 3 | 0/0 |
| Umbriel (`umbriel`) | uranus | 0.023 | Sin atmósfera | 3 | 0/0 |
| Titania (`titania`) | uranus | 0.0385 | Sin atmósfera | 3 | 0/0 |
| Oberón (`oberon`) | uranus | 0.0353 | Sin atmósfera | 3 | 0/0 |
| Miranda (`miranda`) | uranus | 0.0081 | Sin atmósfera | 3 | 0/0 |
| Neptuno (`neptune`) | sun | 1.137 | Hidrógeno no respirable | 3 | 0/2 |
| Tritón (`triton`) | neptune | 0.079 | Nitrógeno tenue | 3 | 0/0 |
| Proteo (`proteus`) | neptune | 0.007 | Sin atmósfera | 3 | 0/0 |

La gravedad efectiva tiene un mínimo jugable de **0,05 g** en las lunas diminutas.
Se utiliza el atributo vanilla `Attributes.GRAVITY`, no correcciones manuales de
velocidad. También se escala `FALL_DAMAGE_MULTIPLIER`. Son modificadores
transitorios identificados por el mod, que se retiran al regresar al Overworld
o salir del catálogo; no se sobrescriben las bases ni modificadores de otros mods.
Se aplica a entidades vivas en ambos lados y al ticker específico del jugador.

Solo la Tierra tiene aire respirable. Tener una atmósfera no significa poder
respirarla: el sistema de la fase 2 consume oxígeno también en Venus, Marte,
Titán y los gigantes. La simulación y la lista configurable de dimensiones sin
aire siguen funcionando como restricciones adicionales. No pueden habilitar
aire respirable en un cuerpo definido como tóxico o vacío.

El calor y la radiación se procesan en servidor, separados de la respiración y
en ticks alternos para no anularse mutuamente con la invulnerabilidad al daño.
Creativo y espectador son inmunes. El casco espacial **solo** resuelve la
respiración; no es un traje térmico o escudo de radiación. Se reutilizan las
fuentes vanilla de daño por superficie caliente y magia, respectivamente.

## Terreno, biomas y minerales

Cada dimensión nueva tiene un tipo, noise settings, un bioma fijo propio, roca,
regolito, mineral y recurso exclusivos: **84 bloques y 28 recursos nuevos**.
Hay relieve por gradiente vertical y ruido con altura/escala por cuerpo; cielo y
niebla con colores propios; rocas superficiales y vetas con parámetros por mundo.
La geometría comparte una familia procedural: no son paisajes artesanales ni
mapas científicos de cada planeta.

Las vetas reemplazan exclusivamente la roca de su propio planeta mediante
`minecraft:block_match`. El bioma referencia directamente sus placed features
en los pasos de rocas y minerales, sin un biome modifier global que mezcle
recursos planetarios con el Overworld. La Luna tiene además titanio de la fase 1.
Existe bedrock inferior y no hay acuíferos ni océanos vanilla. Los mundos tienen
altura 256 y mínimo Y=0.

Los minerales sueltan el recurso directo; no hace falta fundición. El recurso de
la Luna es `lunar_crystal`; los tres planetas rocosos aportan `martian_metal`,
`venusian_sulfur` y `mercurian_cobalt`. El resto tiene sus cristales propios,
que quedan disponibles para progresiones futuras. No se han añadido recetas
artificiales que permitan fabricar estos recursos desde materiales terrestres.

Las texturas provisionales reutilizan recursos vanilla. El regolito no hereda
la física del bloque cuya textura utiliza (por ejemplo, no es hielo resbaladizo).
Los tipos de dimensión utilizan efectos vanilla con colores de bioma propios.
No hay anillos renderizados, mapas celestes ni shaders personalizados todavía.
Fuera del Overworld no funcionan camas ni anclas: **no uses camas allí**, pues
se aplica su comportamiento explosivo de Minecraft.

## Progresión y viaje

| Cohete | Receta / requisitos nuevos | Destinos |
| --- | --- | --- |
| Nivel 1 | Receta de la fase 3 | Tierra y Luna |
| Nivel 2 | N1 + 6 cristales lunares + 2 minerales de titanio | Anteriores + Mercurio, Venus, Marte, Fobos y Deimos |
| Nivel 3 | N2 + 4 metales marcianos + 2 azufres venusianos + 2 cobaltos mercurianos | Todos los cuerpos del catálogo |

Los tres usan una plataforma 3×3, dos bloques de espacio superior, depósito de
4000 mB y la infraestructura de inventario/capabilities de la fase 3.
El menú muestra nivel y destino. **Destino siguiente** recorre solo destinos
permitidos por el nivel, omitiendo la dimensión actual; **Viajar** solicita
al servidor el traslado. No hay paquete que permita al cliente imponer nivel,
destino arbitrario, combustible o coordenadas.

Cada trayecto exige y consume el depósito completo de lava. Un jugador de
supervivencia necesita casco equipado y al menos un tanque cargado si el destino
no permite respirar, incluso cuando la configuración simula vacío en la Tierra.
Antes de viajar, lleva combustible para volver y oxígeno suficiente para la
expedición. No hay estaciones automáticas de combustible en los planetas.

El viaje es **instantáneo y para un jugador**, sin vuelo animado ni pasajeros.
El servidor busca una región de aire por encima del relieve, comprueba el borde
del mundo y la altura, y coloca una plataforma y el mismo nivel de cohete.
El jugador aparece al lado del cohete, sobre la plataforma. No se reemplazan
terreno, contenedores ni construcciones existentes. La base puede quedar algo
elevada sobre el terreno.

El inventario del cohete se traslada; el depósito de llegada queda vacío y el
cohete de origen se retira sin duplicar objetos. La plataforma de origen queda
en su sitio. Si no se confirma el cambio de dimensión, se revierten solo los
bloques recién colocados y no se consume combustible. Las ubicaciones de llegada
son deterministas por jugador; si una está ocupada se busca otra zona de aire.
No se conserva una red de estaciones o rutas.

El destino se guarda por identificador estable (`DestinationId`), no solo por
posición en el catálogo; se admite el entero antiguo para compatibilidad.
Al desmontar un cohete sigue siendo necesario drenar el fluido primero.

## Criaturas

Hay cuatro tipos registrados: `moon_crawler`, `mars_crawler`,
`venus_crawler` y `mercury_crawler`. Usan la clase `PlanetaryCrawler`,
IA y geometría de silverfish provisionales, inmunidad al fuego, atributos propios
y loot del recurso de su mundo. Venus tiene más vida y Mercurio más ataque.
Sus biomas los incluyen en la categoría hostil, con reglas de aparición sobre
suelo sin exigir oscuridad. Necesitan dificultad distinta de Pacífico para
su aparición natural; no necesitan un tanque y no reciben los peligros definidos
específicamente para jugadores.

No se han implementado animaciones o modelos alienígenas originales, facciones
ni ecosistemas completos. Estas son variantes funcionales de una criatura
terrestre pequeña adaptada a las reglas ambientales del mod.

## Estructura y generación reproducible

Todas las rutas parten de `/workspace/mod-minecraft`:

```text
src/main/resources/solar_system.json                    Catálogo canónico
scripts/generate-solar-system.mjs                      Generador de recursos (Node)
src/main/java/com/vic17usd/galaxycraft/
  solar/CelestialBody.java                             Perfil y clave de dimensión
  solar/SolarSystem.java                               Catálogo y búsqueda por dimensión
  solar/SolarEnvironment.java                          Gravedad, calor y radiación
  registry/ModPlanetContent.java                       Bloques e ítems por cuerpo
  registry/ModAlienEntities.java                       Criaturas, atributos y spawns
  entity/PlanetaryCrawler.java                         Variante de criatura
  rocket/RocketTravel.java                             Prevalidación, llegada y traslado
  blockentity/RocketBlockEntity.java                   Niveles y destino persistente
  menu/RocketMenu.java                                 Peticiones de navegación verificadas
  client/RocketScreen.java                             Selección y botón de viaje
src/main/resources/data/galaxycraft/
  dimension/<cuerpo>.json
  dimension_type/<cuerpo>.json
  worldgen/noise_settings/<cuerpo>.json
  worldgen/biome/<cuerpo>.json
  worldgen/configured_feature/<cuerpo>_{ore,boulders}.json
  worldgen/placed_feature/<cuerpo>_{ore,boulders}.json
  loot_table/blocks/<cuerpo>_{stone,regolith,ore}.json
  loot_table/entities/<cuerpo>_crawler.json
  recipe/tier{2,3}_rocket.json
src/main/resources/assets/galaxycraft/
  blockstates/<cuerpo>_{stone,regolith,ore}.json
  models/block/<cuerpo>_{stone,regolith,ore}.json
  models/item/<cuerpo>_{stone,regolith,ore}.json
  models/item/<recurso>.json
  lang/{es_es,en_us}.json
src/test/java/com/vic17usd/galaxycraft/solar/SolarSystemTest.java
```

Los dos niveles adicionales tienen blockstates, modelos, ítems y loot propios.
Las etiquetas de minería contienen los bloques nuevos. El generador conserva
las traducciones previas y actualiza las claves que administra. Para regenerar:

```bash
node scripts/generate-solar-system.mjs
./gradlew build
```

Los JSON generados están incluidos en el repositorio: **Node no es necesario
para compilar**, solo para regenerarlos. Modifica el catálogo o el generador,
no únicamente un JSON derivado. Los comentarios están en el código y esta guía;
los JSON estándar permanecen sin comentarios.

Conserva los identificadores de dimensiones, bloques e ítems en partidas existentes.
Quitar cuerpos o cambiar alturas requiere una migración de mundos; el generador
no borra archivos antiguos automáticamente. Cambiar relieve afecta a los chunks
nuevos, no transforma los existentes.

## Validación y límites de las pruebas

La suite contiene las pruebas de fases anteriores y 31 casos nuevos: 28 contratos
de dimensión/bioma/feature/modelo/botín, catálogo y jerarquía, límites por nivel y
accesibilidad de los ingredientes de los cohetes. El servidor real validó los
codecs de todos los datapacks y cargó 1296 recetas.

Se generaron y guardaron 25 chunks por dimensión nueva: **700 chunks** en total.
Cada muestra contenía suelo y mineral del cuerpo correspondiente; los 28 biomas
se verificaron mediante comandos. Se invocaron las cuatro criaturas y sus
atributos de gravedad fueron Luna=0,0132, Marte=0,0304, Venus=0,07232 y
Mercurio=0,03024; al trasladar la criatura lunar a Júpiter pasó a 0,20224 y al
regresar al Overworld recuperó 0,08. Se retiraron las entidades de prueba y las
cargas forzadas de chunks antes de guardar y cerrar el servidor.

Los 632 JSON son sintácticamente válidos y se verificaron 125 referencias a
texturas vanilla, además de los modelos padre. La regeneración produjo los
mismos hashes de recursos. Sigue presente el aviso
externo de claves de Mojang del entorno anterior, sin impedir cargar mundos.
Las pruebas no incluyen una partida con un jugador conectado: quedan pendientes
la interacción de la GUI, el viaje completo de ida/vuelta por cohete, la
experiencia visual, el consumo de oxígeno y daño ambiental durante ese viaje,
la aparición natural sostenida de criaturas y el rendimiento multijugador.

Para la comprobación con cliente:

```mcfunction
/give @s galaxycraft:tier1_rocket
/give @s galaxycraft:launch_pad 9
/give @s galaxycraft:space_helmet
/give @s galaxycraft:oxygen_tank
/give @s minecraft:lava_bucket 8
```

Carga cuatro cubos, equipa el casco, selecciona la Luna y viaja. Usa los otros
cuatro para regresar. Repite con N2 y N3, verifica las restricciones y prueba
un destino sin el equipo requerido: debe rechazar el viaje sin gastar combustible.
Para inspección administrativa puedes usar, por ejemplo:

```mcfunction
/execute in galaxycraft:mars run tp @s 0 180 0
/summon galaxycraft:mars_crawler
```

El teletransporte administrativo evita las reglas del cohete y no prepara una
plataforma: úsalo en creativo en un mundo de pruebas.
