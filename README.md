# GalaxyCraft — Sistema Solar y supervivencia avanzada

La versión actual es **0.5.0**: [misiones, supervivencia, rover y estaciones](docs/EXPANSION_SUPERVIVENCIA.md).
Añade diez contratos con GUI (**J**), módulos térmico y de radiación en el traje
(**G**), bases herméticas con oxígeno y FE finitos, rover con muestras/batería y
29 órbitas con estaciones personales y gravedad cero. Descarga
[`galaxycraft-0.5.0.jar`](downloads/galaxycraft-0.5.0.jar) desde GitHub con
**Download raw file** y sustituye la versión anterior en `mods`.

El [ordenador celestial y nuevo arte](docs/NAVEGACION_Y_ARTE.md) conserva el mapa
jerárquico con **H**, validación de viajes y patrones minerales propios, incluida
la corrección de sincronización del botón Sol de 0.4.1.

La [fase 5 ampliada](docs/FASE_5_AVANZADA.md) documenta el equipamiento independiente,
el cohete a escala y las 145 estructuras de 0.3.0, que siguen disponibles.

La [fase 5 inicial](docs/FASE_5.md) documenta la entrega histórica 0.2.0.

La [guía de la fase 4](docs/FASE_4.md) documenta los 29 cuerpos del catálogo,
28 dimensiones nuevas, gravedad, atmósferas, recursos, criaturas y navegación
con tres niveles de cohete. La Tierra utiliza el Overworld existente. Las guías
anteriores se conservan como historial: sus límites sobre el viaje lunar quedan
reemplazados por la implementación de esta fase.

La [guía de la fase 3](docs/FASE_3.md) describe la plataforma 3×3, el cohete de
nivel 1 con depósito NeoForge e inventario, su GUI, recetas y límites actuales.

La fase 2 añade un componente de oxígeno persistente y sincronizado, un tanque
recargable y un casco espacial. Consulta [la guía de supervivencia](docs/FASE_2.md)
para los archivos, recetas, simulación de vacío y pruebas. La información de
validación de la fase 1 que aparece más abajo se conserva como registro histórico;
ahora `build` también ejecuta las pruebas JUnit del componente de oxígeno.

Base modular para un mod de exploración espacial de **Minecraft 1.21 exacto**, con
**NeoForge 21.0.167**, **Java 21**, **ModDevGradle 2.0.148** y **Gradle 9.2.1**.
`GalaxyCraft` / `galaxycraft` es el nombre inicial del proyecto.

Esta fase registra `galaxycraft:titanium_ore`, su `BlockItem`, su entrada en la
pestaña de bloques de construcción y su generación en el Overworld. Las fases 2
y 3 añaden el casco, los tanques y el cohete estacionario. La fase 4 incorpora las
dimensiones, criaturas y viaje instantáneo; no simula vuelo orbital animado.

## Abrir en IntelliJ IDEA

1. Abre la carpeta raíz `mod-minecraft` como proyecto Gradle. En este workspace:
   `/workspace/mod-minecraft`. No abras solamente `src`.
2. Selecciona un **JDK 21 completo** como Project SDK y Gradle JVM. Un JRE no basta.
   Gradle resuelve las dependencias; no agregues los JAR de NeoForge a mano.
3. Sincroniza Gradle. ModDevGradle creará las configuraciones de ejecución.
4. Ejecuta `./gradlew build` y luego `./gradlew runClient`; en Windows usa
   `gradlew.bat build` y `gradlew.bat runClient`. No necesitas instalar Gradle global.

En esta máquina de nube, el JDK está en `/workspace/.toolchains/jdk-21.0.9+10`:

```bash
# Selecciona el JDK, las cachés y el proxy de Java de esta máquina de nube.
source /workspace/.toolchains/activate-galaxycraft.sh
cd /workspace/mod-minecraft
./gradlew build
```

`runClient` necesita un entorno gráfico; la nube puede utilizar `runServer` para
validar el lado común. Escribe `stop` en su consola para guardar y cerrar el mundo.
NeoForge permite arrancar su servidor de desarrollo sin editar `eula.txt`; para un
servidor distribuido fuera del entorno de desarrollo, consulta y acepta la
[EULA de Minecraft](https://aka.ms/MinecraftEULA) antes de habilitar `eula=true`.
No se requieren claves ni credenciales de Minecraft para compilar.

## Archivos y responsabilidades

Todas las rutas siguientes son relativas a `/workspace/mod-minecraft`:

```text
build.gradle                                      Configuración de Java y NeoForge
gradle.properties                                 Versiones e identidad del mod
settings.gradle                                   Resolución del plugin de Gradle
gradlew / gradlew.bat                              Lanzadores de Gradle
gradle/wrapper/                                   Wrapper y checksum de Gradle
src/main/java/com/vic17usd/galaxycraft/
├── GalaxyCraft.java                              Entrada @Mod y bus del mod
└── registry/
    ├── ModBlocks.java                            Registro diferido del mineral
    └── ModItems.java                             BlockItem y pestaña creativa
src/main/templates/META-INF/neoforge.mods.toml     Metadatos expandidos al compilar
src/main/resources/
├── assets/galaxycraft/
│   ├── blockstates/titanium_ore.json              Estado → modelo
│   ├── models/block/titanium_ore.json             Modelo cúbico y textura
│   ├── models/item/titanium_ore.json              Modelo del ítem en 1.21
│   └── lang/
│       ├── es_es.json                            Traducción al español
│       └── en_us.json                            Traducción al inglés
└── data/
    ├── minecraft/tags/block/
    │   ├── mineable/pickaxe.json                  Se mina con pico
    │   └── needs_iron_tool.json                   Hierro o superior
    └── galaxycraft/
        ├── loot_table/blocks/titanium_ore.json    Botín del bloque
        ├── worldgen/configured_feature/ore_titanium.json
        ├── worldgen/placed_feature/ore_titanium.json
        └── neoforge/biome_modifier/add_titanium_ore.json
```

ModDevGradle convierte la plantilla de metadatos en
`build/generated/sources/modMetadata/META-INF/neoforge.mods.toml`, que se incorpora
al JAR. Edita la plantilla original, no su copia generada. El artefacto final es
`build/libs/galaxycraft-0.1.0.jar`.

## Registro y buenas prácticas de NeoForge 1.21

- `@Mod` recibe el `IEventBus` del mod por inyección en el constructor. Los
  `DeferredRegister` se conectan a **ese bus**, que gestiona el registro y el ciclo
  de carga. `NeoForge.EVENT_BUS` se reserva para eventos de juego cuando hagan falta.
- `DeferredRegister.Blocks` y `.Items`, con `DeferredBlock` y `DeferredItem`,
  evitan registros manuales y conservan el tipo concreto. El bloque y su ítem
  pueden compartir `galaxycraft:titanium_ore`: son registros diferentes.
- Los proveedores retrasan la construcción hasta el momento correcto. No llames
  a `.get()` de un holder en un inicializador estático para construir otro registro.
  `registerSimpleBlockItem` resuelve esa dependencia de forma diferida.
- `BuildCreativeModeTabContentsEvent` pertenece al bus del mod. Registrar el
  `BlockItem` permite `/give`; añadirlo a la pestaña permite encontrarlo en creativo.
- `.requiresCorrectToolForDrops()` y las etiquetas trabajan juntas: el bloque
  exige un pico de hierro, diamante o netherita para soltar botín. Esta fase suelta
  el propio mineral; no incluye titanio crudo, fundición ni Fortuna.
- El código común no importa clases de `net.minecraft.client`, por lo que puede
  cargarse también en un servidor dedicado.
- Usa `assets` para modelos, texturas e idiomas y `data` para botín, etiquetas y
  generación. Los JSON estándar no admiten comentarios: las explicaciones están
  aquí para mantener los recursos válidos.
- En **1.21**, son `loot_table` y `tags/block` en singular. El ítem utiliza
  `models/item/*.json`; no mezcles el sistema `assets/<modid>/items` de 1.21.4+.
- El mineral no necesita estado individual en el `ItemStack`. Para oxígeno y
  combustible de ítems, la fase correspondiente registrará un
  `DataComponentType<T>` en `Registries.DATA_COMPONENT_TYPE`, con `Codec` para
  persistencia y `StreamCodec` para sincronización. Se usará `stack.get(...)` y
  `stack.set(...)`, no los antiguos `getOrCreateTag()` / `setTag()`.
  Los componentes de ítems no sustituyen por sí solos el almacenamiento de una
  dimensión, una entidad o un `BlockEntity`.

## Modelos y textura inicial

`blockstates/titanium_ore.json` enlaza todos los estados con
`galaxycraft:block/titanium_ore`. El modelo del bloque hereda `minecraft:block/cube_all`
y usa **temporalmente `minecraft:block/iron_ore`**: el bloque se ve completo usando
una textura existente de Minecraft. El modelo del ítem hereda el del bloque.

Cuando tengas una textura propia, guárdala en
`src/main/resources/assets/galaxycraft/textures/block/titanium_ore.png` (por ejemplo,
16×16 píxeles) y cambia `textures.all` a `galaxycraft:block/titanium_ore`, sin `.png`.

## Cómo funciona la generación

1. `configured_feature/ore_titanium.json` configura `minecraft:ore`: veta de hasta
   **7 bloques**, reemplazando exclusivamente `#minecraft:stone_ore_replaceables`
   con `galaxycraft:titanium_ore`.
2. `placed_feature/ore_titanium.json` referencia esa configuración y aplica, en
   orden, **4 intentos por chunk**, dispersión horizontal, altura uniforme entre
   **Y=0 e Y=64**, y el filtro de bioma. Son intentos, no una garantía de cuatro vetas;
   los bloques de una veta pueden extenderse algo más allá de la altura del intento.
3. `biome_modifier/add_titanium_ore.json` usa `neoforge:add_features` para incorporar
   la feature a `#minecraft:is_overworld` en el paso `underground_ores`.
   Definir solo los dos primeros JSON no inserta el mineral en los biomas.

El ejemplo no reemplaza deepslate ni afecta a dimensiones futuras; cada planeta
podrá tener sus propias etiquetas, features y reglas. La generación natural
aparece en **chunks nuevos**, no modifica los ya generados.

## Verificación en el juego

Después de `./gradlew build`, abre `runClient`, crea un mundo creativo con trucos y:

```mcfunction
/give @s galaxycraft:titanium_ore 64
/setblock ~ ~ ~ galaxycraft:titanium_ore
/place feature galaxycraft:ore_titanium ~ ~ ~
```

El comando `/place feature` prueba la **configured feature**, no su distribución
natural; úsalo dentro de suficiente piedra reemplazable, lejos de construcciones.
Comprueba además:

- Nombre traducido, bloque sin textura ausente e ítem visible en construcción.
- En supervivencia, sin encantamientos: pico de piedra no da botín; pico de hierro
  o superior da un bloque de mineral.
- Mundo nuevo/chunks nuevos en el Overworld: vetas entre Y=0 e Y=64, en piedra.
- Logs sin errores de registro, modelos, loot tables ni features al cargar el mundo.

La carga real del juego es la comprobación de los codecs de los recursos. Un JSON
bien formado o una compilación correcta no prueban por sí solos la generación
natural ni el renderizado. Las comprobaciones visuales y de minería por un jugador
siguen pendientes de un cliente gráfico.

## Validación realizada en la nube

- `./gradlew --no-daemon build --console=plain`: compilación y empaquetado correctos;
  los tres archivos Java compilan contra Minecraft 1.21 y NeoForge 21.0.167 reales.
- Los 11 JSON son sintácticamente válidos; el JAR contiene las clases, los recursos
  y el TOML expandido. El wrapper oficial y su distribución tienen SHA-256 verificado.
- `runServer`: dos arranques correctos, carga de registros y datapacks, creación del
  mundo y respuesta de estado con versión `1.21`, protocolo `767`.
- Generación natural: antes de colocar bloques mediante comandos, las paletas de
  **80 de 841 chunks guardados** del mundo de prueba contenían `galaxycraft:titanium_ore`.
- Consola: `setblock` y `place feature galaxycraft:ore_titanium` correctos; el bloque
  pertenece a `#minecraft:mineable/pickaxe` y `#minecraft:needs_iron_tool` en ejecución.
- `loot spawn ... mine ... minecraft:iron_pickaxe` produjo un `Titanium Ore` usando
  `galaxycraft:blocks/titanium_ore`. Esto comprueba el botín, no simula la minería
  completa de un jugador con distintas herramientas.
- El segundo servidor se cerró con `stop`, guardando el mundo. Los mundos y ajustes
  locales permanecen ignorados bajo `run/`.

No hay una suite de tests automatizados todavía: Gradle informa `test NO-SOURCE`.
Las verificaciones funcionales anteriores se realizaron por consola y protocolo.
El renderizado, la pestaña creativa y la experiencia de un jugador requieren
comprobarse con `runClient`.

El entorno de nube emitió un aviso al consultar las claves de autenticación de
Mojang (`api.minecraftservices.com`); la autenticación de jugadores no se validó.
También apareció `Unprimed heightmap: OCEAN_FLOOR_WG` al colocar una feature de
mineral por comando en chunks ya terminados: se reprodujo con `minecraft:ore_iron`,
no solo con titanio. Ambas features se colocaron y la generación natural cargó
correctamente.

## Referencias de versión

- [MDK oficial de Minecraft 1.21 con ModDevGradle](https://github.com/NeoForgeMDKs/MDK-1.21-ModDevGradle)
- [ModDevGradle](https://github.com/neoforged/ModDevGradle)
- [Documentación de NeoForge](https://docs.neoforged.net/)

Esta base usa los mappings oficiales de Mojang que proporciona ModDevGradle; no
necesita Parchment ni un repositorio adicional para esta fase. Mantén versiones
fijas y cambia Minecraft y NeoForge juntos cuando planifiques una migración.
