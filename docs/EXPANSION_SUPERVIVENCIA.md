# GalaxyCraft 0.5.0 — Misiones, logística y estaciones

Minecraft **1.21 exacto**, NeoForge **21.0.167**, Java **21**. Haz una copia de tu
mundo y reemplaza el JAR anterior; no dejes dos versiones en `mods`. Las ocho
ranuras personales migran automáticamente los seis espacios de 0.3/0.4 sin borrar
armadura, tanques ni sus Data Components. No hace falta instalar Curios.

## Controles y primeros pasos

| Acción | Control predeterminado |
| --- | --- |
| Misiones | **J**, o clic derecho con la libreta de misiones |
| Traje independiente y módulos | **G**, o botón Traje del inventario |
| Navegación del cohete | **H**, cerca del ordenador del cohete |
| Montar rover | Clic derecho |
| Inventario del rover | Agacharse + clic derecho, desde fuera |
| Conducir / bajar del rover | WASD / agacharse |
| Flotar en órbita sin generador | WASD, saltar para subir, agacharse para bajar |
| Estación propia / volver al planeta | Usar el terminal orbital |

J, G y H se reasignan en Opciones → Controles → GalaxyCraft. Todos los objetos
nuevos aparecen al final de la pestaña creativa del mod y tienen recetas.

## Libro de contratos

Diez misiones repartidas en cuatro fases: Primeros Pasos, Hacia la Luna,
Conquista de Marte y Exploración Galáctica. La GUI permite consultar fases,
dependencias, progreso y recompensas pasando el cursor por cada botón.

Las entregas requieren pulsar **Entregar**; consumen exclusivamente la cantidad
solicitada de la mochila/mano secundaria. No se toman objetos de cofres, shulkers
ni del equipo. Visitar dimensiones, construir la plataforma 3×3, entrar en una
sala presurizada y conducir 32 metros se acreditan automáticamente tras cumplir
la misión anterior. Las evidencias se conservan aunque se obtengan antes de
desbloquear el contrato. Las recompensas se entregan una única vez, con experiencia
y objetos; si la mochila está llena, el excedente cae al suelo.

El progreso vive en un attachment serializable del jugador, sobrevive a la muerte
y se sincroniza al abrir la GUI. `src/main/resources/quests.json` define el catálogo
incluido en el mod; no es un sistema de contratos recargable mediante datapacks.
Si se regenera el contenido, editar también `scripts/generate-expansion.mjs`.

## Temperatura, radiación y traje

El HUD espacial añade temperatura, dosis de radiación, presión nominal y estado
del refugio. Dentro de una base con aire muestra la **reserva compartida**, no
avisa de falta de casco y usa 101,3 kPa. La temperatura interior es 22 °C.

La exposición se evalúa una vez por segundo. Venus llega a 465 °C, Mercurio a
430 °C; Marte, la Luna y las órbitas son fríos. Las temperaturas son parámetros de
jugabilidad, no una simulación diurna ni de transferencia de calor. La radiación
planetaria utiliza los perfiles del catálogo; en órbita su intensidad es 3.

El panel G incorpora dos ranuras adicionales:

- **T:** módulo térmico contra temperaturas superiores a 60 °C o inferiores a −35 °C.
- **Pb:** blindaje de plomo contra radiación solar.

Solo funcionan con las **cuatro piezas espaciales y oxígeno disponible**. La
armadura tradicional no se reemplaza. Los módulos tienen 600 usos y pierden uno
cada cinco segundos cuando protegen de su peligro; creativo no los desgasta.
Sin módulo, la exposición aumenta gradualmente y causa daño al alcanzar el umbral.
El traje completo alimentado reduce ese daño un 75 %. La dosis acumulada decae
al refugiarse, por lo que no desaparece instantáneamente. Se reinicia al morir o
reconectar. El plomo se obtiene de vetas del Overworld y se funde en horno.

## Construir una habitación hermética

1. Cierra suelo, paredes y techo con bloques de colisión completa. Vidrio completo
   y paneles reforzados funcionan; losas, rejas, cristales finos y puertas vanilla
   **no** se consideran herméticos en esta versión.
2. Usa la **escotilla de aire**, de dos bloques, para entrar. Cerrada sella; abierta
   genera una fuga. Para circular sin ventilar toda la base, construye una cámara
   intermedia con dos escotillas y cierra una antes de abrir la otra.
3. Coloca un **detector de presión** en una pared, con la cara frontal mirando
   hacia el interior. En su GUI introduce un tanque de oxígeno y proporciona FE.
4. Comprueba que indique sala hermética y tenga FE y oxígeno. Entonces puedes
   quitarte el casco. Si se abre una fuga o se agotan los suministros, vuelve a
   necesitarse el traje.

El escaneo recorre como máximo **1024 celdas** y **12 bloques por eje desde su
origen**. Un chunk no cargado o superar estos límites cuenta como fuga. Los cambios
invalidan el resultado; además se comprueba cada diez ticks. No se cargan chunks
remotos para detectar habitaciones. El detector gasta **1 FE/tick** mientras opera
y una unidad de oxígeno por jugador y segundo; el tanque habitual dura 1200
segundos para un ocupante. Varios jugadores comparten y agotan la misma reserva.
No hay bombeo ilimitado, fluidos de oxígeno ni recarga gratuita dentro de la base.

## Energía y logística

Las cuatro máquinas exponen las capacidades NeoForge `EnergyStorage.BLOCK` e
`ItemHandler.BLOCK`; cada una almacena hasta **30000 FE** y tiene una ranura.

- **Panel solar:** genera 40 FE/tick. En superficie necesita día, cielo despejado
  y ausencia de lluvia; en órbita necesita no tener bloques encima. Reparte hasta
  80 FE por vecino y tick a las otras máquinas.
- **Módulo de acoplamiento:** distribuye energía a detectores/generadores
  adyacentes y recarga rovers a menos de cuatro bloques. Es una estación de carga,
  no un sistema de ensamblaje de naves o tuberías.
- **Generador gravitatorio:** consume 1 FE/tick y devuelve gravedad terrestre en
  un radio de 12 bloques dentro de una órbita. Al agotarse cesa el campo.
- **Detector de presión:** recibe exclusivamente tanques en su ranura.

Las demás máquinas cargan baterías insertadas hasta 100 FE/tick. Las baterías
almacenan **6000 FE** en el Data Component `galaxycraft:battery_charge`. No se
comparten valores entre pilas. La receta produce una batería cargada; una vacía
puede recargarse sin reemplazarla. Otros mods pueden insertar o extraer FE por
las capacidades expuestas; no se añaden cables propios.

## Rover planetario

Coloca el ítem sobre una superficie despejada. Transporta un piloto, nueve pilas
de muestras y una batería extraíble. La reserva interna es de 6000 FE; al insertar
una batería se transfieren hasta 200 FE/tick, debitados de su componente. También
se puede cargar cerca de un acoplamiento o mediante la capacidad de energía de
entidad. Montado, WASD acelera y gira: velocidad objetivo de 0,42 bloques/tick,
escalón máximo de un bloque, colisiones y caída ajustada a la gravedad local.
No tiene tracción en gravedad cero: úsalo en superficie o dentro de un campo.

Solo el piloto puede enviar controles; se rechazan valores no finitos o fuera de
rango y se detiene la aceleración tras ocho ticks sin entrada. Circular consume
1 FE por tick motorizado con desplazamiento. El inventario y la carga sobreviven
a guardar y cargar el mundo. Al destruirlo devuelve sus muestras, su batería y
**un** ítem de rover; la energía del depósito interno no se conserva en el kit.
No es una nave, no vuela ni tiene torreta. El modelo Java muestra chasis, asiento
y cuatro ruedas con rotación proporcional al movimiento.

## Estaciones alrededor de cada cuerpo

Se añaden **29 dimensiones orbitales vacías**, una por cuerpo del catálogo,
incluida la Tierra: `galaxycraft:orbit/earth`, `galaxycraft:orbit/moon`, etc. Son
registros declarativos estables, compatibles con guardados: no se crean registros
de dimensiones arbitrarios mientras el servidor está funcionando.

Usa un **terminal orbital** desde el planeta con un **kit de estación** en la
mochila. En supervivencia necesitas casco y oxígeno para acceder al vacío. El
primer uso consume un kit y construye una base personal 9×9×6 con detector,
tanque lleno, panel solar, acoplamiento y generador con carga inicial. Cada dueño
tiene una parcela separada por 96 bloques; las estaciones del mismo planeta
comparten dimensión. El kit incluye el coste de sus máquinas y suministros.

Después, el terminal visita esa estación sin consumir otro kit. Desde la órbita
regresa al punto del planeta desde el que saliste. Las anclas y el contador de
parcelas se guardan mediante SavedData global del servidor. Se exige una llegada
libre de dos bloques con suelo cerca del punto de retorno: evita crear la estación
mientras vuelas o estás sobre un lugar sin suelo. Si la llegada está bloqueada,
el terminal avisa y no excava ni destruye construcciones para despejarla.

La estación inicial está cerrada: puedes entrar/salir con el terminal y añadir tu
propia cámara de escotillas. Al salir del radio del generador, jugadores y objetos
flotan. Los controles de empuje usan aceleración y amortiguación limitadas; el
mod concede un atributo de vuelo **transitorio y propio** para evitar que el
servidor interprete la flotación como vuelo ilegal, sin cambiar creativo ni el
vuelo otorgado por otros mods. Al entrar en un campo o salir de órbita se retira.
La gravedad previa de los objetos se restaura al abandonar el vacío.

No se simulan órbitas físicas, propiedad de parcelas ni protección antigrief.
El cohete conserva sus destinos planetarios: el acceso orbital de esta entrega
es mediante terminal, no una nueva opción de su mapa celestial.

## Código y verificación

Rutas relativas a la raíz del proyecto en IntelliJ:

```text
src/main/java/com/vic17usd/galaxycraft/
  quest/        Catálogo, attachment, entregas y QuestMenu
  expansion/    Registros, paquetes, aire, FE, módulos y exposición
  rover/        Entidad, colocación, inventario y controles
  station/      Órbitas, construcción, terminal y SavedData
  client/       QuestScreen, LogisticsScreens, RoverRenderer y HUD
  testing/      GameTests de integración, deshabilitados en producción
src/main/resources/quests.json
src/main/resources/data/galaxycraft/dimension/orbit/
src/main/resources/data/galaxycraft/dimension_type/orbital.json
src/main/resources/assets/galaxycraft/{models,textures,lang}/
scripts/generate-expansion.mjs
src/test/java/com/vic17usd/galaxycraft/expansion/ExpansionTest.java
```

Los handlers C2S reciben solo intenciones: abrir, entregar con ID de menú y
controlar el vehículo montado. El servidor calcula requisitos, saldo, recompensas,
exposición, energía y movimiento. No acepta progreso ni cantidades de recompensa
desde el cliente. Los NBT usados para attachments, SavedData y BlockEntities son
persistencia de entidades/mundo; los datos de oxígeno y batería de los ítems usan
**Data Components** de 1.21.

```bash
./gradlew --no-daemon build
# Solo para regenerar los recursos ya incluidos; no hace falta al instalar el JAR:
node scripts/generate-solar-system.mjs
# Pruebas dentro de servidor de desarrollo:
./gradlew --no-daemon runServer -PgalaxycraftGameTests --console=plain
```

En la consola del servidor ejecutar cada prueba por separado y esperar su
marcador `GALAXYCRAFT_INTEGRATION_OK` antes de lanzar la siguiente:

```text
execute positioned 15000 200 15000 run test run quests_and_modules
execute positioned 15000 200 15000 run test run sealed_room_resources
execute positioned 15000 200 15000 run test run rover_authority
execute positioned 15000 200 15000 run test run orbital_station
execute positioned 15000 200 15000 run test run exposure_and_flight
execute positioned 15000 200 15000 run test run station_round_trip
```

Usar un mundo de desarrollo para GameTests. Las pruebas colocan sus propios
recintos y máquinas; no ejecutarlas en una partida valiosa. La validación
automatizada no sustituye revisar manualmente la GUI, conducción y transición de
gravedad con un cliente gráfico, algo que requiere probarse dentro de Minecraft.

Validado: `build` con **209 pruebas JUnit** y **nueve GameTests** aprobados, incluidos
los tres de regresión de equipo, cohete y navegación. El viaje orbital de ida y
vuelta se ejecutó con un jugador de servidor y conexión simulada, no con un cliente
gráfico. Véase el [registro de validación](validation/expansion-0.5.0.md).
