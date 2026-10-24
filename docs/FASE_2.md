# Fase 2: oxígeno y casco espacial

Implementación para Minecraft **1.21**, NeoForge **21.0.167** y Java **21**. Esta
fase utiliza la opción de **casco con soporte para tanque**: no requiere pechera,
pantalones ni botas especiales todavía. No añade dimensiones planetarias.

## Archivos

Rutas relativas a `/workspace/mod-minecraft` (la raíz que se abre en IntelliJ):

| Archivo | Responsabilidad |
| --- | --- |
| `src/main/java/com/vic17usd/galaxycraft/component/OxygenData.java` | Valor inmutable con cantidad actual, capacidad, consumo, recarga y codecs |
| `src/main/java/com/vic17usd/galaxycraft/registry/ModDataComponents.java` | Registro de `galaxycraft:oxygen_level` en el bus del mod |
| `src/main/java/com/vic17usd/galaxycraft/item/OxygenTankItem.java` | Componente inicial, recarga manual, tooltip y barra de reserva |
| `src/main/java/com/vic17usd/galaxycraft/config/SpaceConfig.java` | Simulación y dimensiones sin atmósfera, configuradas por mundo |
| `src/main/java/com/vic17usd/galaxycraft/survival/SpaceSurvival.java` | Detección del casco, selección del tanque, consumo y daño en el servidor |
| `src/main/java/com/vic17usd/galaxycraft/registry/ModItems.java` | Registro diferido del casco y del tanque, pestañas creativas |
| `src/main/java/com/vic17usd/galaxycraft/GalaxyCraft.java` | Conexión de registros, configuración SERVER y ticker al bus del juego |
| `src/main/resources/assets/galaxycraft/models/item/oxygen_tank.json` | Modelo de inventario del tanque |
| `src/main/resources/assets/galaxycraft/models/item/space_helmet.json` | Modelo de inventario del casco |
| `src/main/resources/assets/galaxycraft/lang/{es_es,en_us}.json` | Nombres, instrucciones y avisos traducidos |
| `src/main/resources/data/galaxycraft/recipe/{oxygen_tank,space_helmet}.json` | Recetas de crafteo, en la carpeta singular de 1.21 |
| `src/test/java/com/vic17usd/galaxycraft/component/OxygenDataTest.java` | Pruebas de transiciones, persistencia y sincronización |

Los JSON no contienen comentarios porque el formato no los admite. Los modelos
reutilizan recursos vanilla provisionales: lingote de hierro para el tanque y
casco de hierro para el casco espacial. El casco equipado también utiliza el
material y la textura vanilla de hierro: tiene 165 de durabilidad y su defensa
y reparación son las de ese material. Solo `galaxycraft:space_helmet` habilita
respirar; un casco vanilla de hierro no sirve.

## Componente y reglas de supervivencia

`OXYGEN_LEVEL` es un `DataComponentType<OxygenData>`. `Codec` valida y guarda
`current` y `maximum`; `StreamCodec` los sincroniza con el cliente mediante la
sincronización normal de `ItemStack`. No se almacenan etiquetas NBT manualmente.
El estado siempre satisface `0 <= current <= maximum` y `maximum > 0`.
Cada cambio sustituye el record completo con `stack.set(...)`.

El tanque no se apila y nace con **1200/1200 unidades**. Cada unidad proporciona
un segundo de juego a 20 TPS: aproximadamente **20 minutos** por tanque. El
consumo se hace exclusivamente en `PlayerTickEvent.Post`, cada 20 ticks, en el
servidor. No se usa `inventoryTick`, evitando que varios tanques se vacíen a la vez.
Con menos TPS, la duración en tiempo real aumenta.

En vacío, un jugador vivo de supervivencia o aventura necesita el casco equipado
y un tanque con reserva en su inventario principal o mano secundaria. Se consume
del primer tanque disponible; al vaciarse se pasa al siguiente. No se buscan
tanques dentro de mochilas, shulkers, contenedores ni cofres de Ender. El último
segundo de oxígeno sigue protegiendo al jugador.

Sin casco o sin reserva, recibe **2 puntos de daño por segundo** (un corazón,
antes de las modificaciones de daño de Minecraft u otros mods) con la fuente de
ahogamiento y un aviso en la barra de acción. Creativo y espectador están exentos.
Esto es una reserva independiente de la barra de aire vanilla: no implementa
respiración subacuática ni inmunidad general al daño.

Para recargar, sostén el tanque y usa **agacharse + clic derecho** en una dimensión
con atmósfera respirable. La recarga es completa, manual y gratuita en este
prototipo; en vacío se rechaza incluso en creativo. No hay recarga automática.
Más adelante una estación de oxígeno podrá sustituir esta mecánica sin cambiar
el formato del componente. Un tanque al que se haya eliminado el componente por
comandos se considera vacío, y puede recuperarse mediante recarga normal.

## Recetas iniciales

`T` = `galaxycraft:titanium_ore`, `G` = cristal vanilla. Se usa el mineral de la
fase 1 porque todavía no existe un lingote refinado de titanio.

```text
Tanque:       Casco:
 T            TTT
TGT           TGT
 T
```

Cada receta produce una unidad. Los ítems aparecen en las pestañas de herramientas
(tanque) y combate (casco). Se pueden fabricar directamente en la mesa aunque
todavía no haya un advancement de descubrimiento para el libro de recetas.

## Simular una dimensión sin atmósfera

Arranca un mundo una vez. Detén el servidor antes de editar su archivo base,
cuya ubicación se ha comprobado con NeoForge 21.0.167:

```text
run/server/config/galaxycraft-server.toml
```

Para configurar solo un mundo, copia el archivo base a
`run/server/world/serverconfig/galaxycraft-server.toml` y modifica esa copia: sus
valores sobrescriben los del archivo base al cargar ese mundo. En un cliente
integrado, la base está en `config/galaxycraft-server.toml` de la instancia y la
sobrescritura en `saves/<mundo>/serverconfig/galaxycraft-server.toml`.

```toml
# Prueba global: todas las dimensiones pasan a carecer de atmósfera.
simulateVacuum = true
# Esta lista se usa también cuando la simulación global está desactivada.
airlessDimensions = ["galaxycraft:moon"]
```

Reinicia y entra en supervivencia. Para hacer una prueba con refugio respirable,
puedes dejar `simulateVacuum = false` y usar
`airlessDimensions = ["minecraft:the_end"]`. La entrada `galaxycraft:moon` es
preparatoria: no registra ni crea una Luna. Por defecto, la simulación está
desactivada y las dimensiones vanilla siguen siendo respirables.

## Comprobación manual con un jugador

En un mundo de prueba con comandos habilitados:

```mcfunction
/give @s galaxycraft:space_helmet
/give @s galaxycraft:oxygen_tank[galaxycraft:oxygen_level={current:3,maximum:1200}]
/gamemode survival
```

Equipa el casco y activa el vacío como se indica arriba. Comprueba que la reserva
baja cada segundo, protege durante tres consumos y luego comienza el daño. Quita
el casco con un tanque lleno: debe haber daño sin consumir reserva. Añade un
segundo tanque para comprobar el relevo. En una atmósfera respirable no debe
consumirse reserva y agacharse + usar debe rellenarla. En vacío, la misma acción
debe rechazarse. Guarda y vuelve a entrar para comprobar la persistencia; prueba
también creativo y espectador para comprobar su exención.

La barra del tanque indica la reserva cuando no está lleno. El tooltip indica
cantidad actual/capacidad. El componente puede inspeccionarse con comandos:

```mcfunction
/data get entity @s Inventory
```

## Compilar y ejecutar las pruebas

En la nube, activa primero `/workspace/.toolchains/activate-galaxycraft.sh`. En
IntelliJ selecciona Gradle JVM = JDK 21.

```bash
# Compila el mod y ejecuta las pruebas JUnit con el soporte de ModDevGradle.
./gradlew build
# Ejecuta solo los tests y consulta después build/reports/tests/test/index.html.
./gradlew test
```

Las cinco pruebas cubren saturación del consumo e inmutabilidad, recarga a la
capacidad individual, ida y vuelta del codec persistente, rechazo de datos
inválidos e ida y vuelta del codec de red. No sustituyen la prueba de un jugador
equipado, de la interfaz gráfica o de un cliente multijugador.

Validación realizada: compilación correcta con los cinco tests sin fallos ni
omisiones; servidor dedicado iniciado y detenido correctamente; 1292 recetas
cargadas (dos más que la fase 1); creación del casco y tanques en un contenedor
de prueba; serialización del componente con `current:7,maximum:20`; rechazo en
ejecución de un componente con `current:21,maximum:20`. El contenedor temporal se
vació y retiró al terminar. Las comprobaciones con un jugador descritas arriba
permanecen pendientes, incluida la experiencia visual y el daño/consumo durante
sus ticks. El aviso de red de claves públicas de Mojang de la fase 1 sigue
presente y no impidió cargar el mod.
