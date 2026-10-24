# Ordenador celestial y materiales — GalaxyCraft 0.4.1

Minecraft **1.21 exacto**, NeoForge **21.0.167**, Java **21**. Sustituye el JAR
anterior tanto en cliente como en servidor; los IDs existentes y los mundos se conservan.

## Corrección 0.4.1: Sol no respondía al clic

La pantalla se construía con el nivel provisional N0, antes de recibir el nivel
del servidor. El texto de telemetría se actualizaba a N1/N2/N3, pero el botón
conservaba el estado inactivo y el candado iniciales. `LiveNavigationButton`
ahora reevalúa disponibilidad y candado en cada fotograma y antes de procesar
entrada de ratón o teclado. También se corrigen las marcas de nivel de planetas.

La compilación y las **202 pruebas JUnit** pasan, incluidas tres regresiones de
widgets cliente sin OpenGL que simulan datos tardíos N0→N3, actualización al
recibir entrada y previsualización de destinos con nivel insuficiente. No se
cambian los requisitos de viaje ni el protocolo. Reinicia el juego después de
sustituir el JAR; recargar texturas no actualiza las clases Java.

## Uso del mapa

Haz clic derecho en el cohete o en una de sus piezas para abrir su ordenador.
Desde los controles de combustible también puedes pulsar **Navegar**. La tecla
**H**, reasignable en Controles → GalaxyCraft, abre el mapa a un máximo de cuatro
bloques del ordenador o mientras tienes abiertos sus controles. **G** continúa
abriendo el traje. H funciona también dentro de la pantalla de combustible.

1. Selecciona el **Sistema Sol** en la vista de sistemas.
2. Selecciona un planeta en el plano orbital.
3. Pulsa el planeta central para su superficie o el icono de una de sus lunas.
4. Espera la confirmación del servidor y pulsa **Confirmar despegue**.

**Atrás** cambia de nivel; **Combustible** vuelve al inventario de la máquina.
Los iconos se pueden seleccionar con ratón o teclado; sus tooltips indican nombre,
nivel y coste. El panel muestra nivel del vehículo, requisito del destino,
gravedad, reserva y motivo del bloqueo. Los iconos bloqueados se pueden explorar
para consultar requisitos. La vista es un esquema de navegación, no una simulación
astronómica de distancias u órbitas.

Solo hay un sistema jugable, **Sol**, con ocho planetas y las lunas del catálogo.
`stellar_systems.json` define su jerarquía. Para añadir sistemas futuros se deben
crear cuerpos y dimensiones reales y asignar su nivel mínimo; añadir un icono no
crea un destino. No se muestran sistemas ficticios como opciones funcionales.

## Reglas y confirmación

Cada trayecto mantiene el coste anterior: **4000 mB de lava / cuatro cubos**.
No se introduce un coste por distancia que contradiga el depósito o el consumo
existente. El mapa bloquea el lanzamiento y explica por tooltip y estado:

- Nivel insuficiente de cohete o sistema.
- Combustible insuficiente, plataforma incompleta o volumen obstruido.
- Destino actual, dimensión ausente o sistema desactivado.
- Falta de casco y oxígeno para un entorno no respirable, salvo creativo.
- Piloto muerto/espectador, menú inválido o encendido ya iniciado.

El mapa respeta la progresión: N1 Luna, N2 destinos rocosos intermedios, N3
gigantes y exteriores. El servidor aplica también el nivel mínimo del sistema a
las rutas de lanzamiento anteriores, evitando saltarlo desde otra interfaz.

**La selección es compartida por el ordenador del cohete**, no una preferencia
privada por jugador. Si dos usuarios lo manejan, el último cambio aceptado se
sincroniza a los menús abiertos. La confirmación incluye el ID del destino mostrado;
si el servidor tiene ya otro destino, se rechaza el lanzamiento en vez de viajar
al lugar equivocado. Cambiar la selección queda bloqueado durante el encendido.

El sonido de ignición se reproduce solo cuando el servidor acepta el inicio.
Continúa la secuencia de cuatro segundos de 0.3.0; el combustible se consume tras
confirmar el traslado. Alejarse o desconectarse cancela el encendido. El piloto
se mueve entre dimensiones al terminar; el cohete no es una entidad de vuelo libre.

## Menú, paquetes y autoridad

`NavigationMenu` no tiene ranuras. Sincroniza combustible, preparación, nivel,
destino, motivo de bloqueo y estado de encendido mediante `ContainerData`. Esto
proporciona la respuesta S2C a la selección sin mantener una copia cliente del
estado de la máquina.

`NavigationNetworking` registra cuatro solicitudes C2S con StreamCodec:

| Paquete | Datos | Validación |
| --- | --- | --- |
| `Open` | Ninguno | Ordenador contextual válido; no acepta coordenadas del cliente |
| `Controls` | Ninguno | Solo vuelve desde el menú de navegación vigente |
| `Select` | ID de menú e ID estable de cuerpo | Sesión, distancia, namespace, catálogo y ausencia de encendido |
| `Launch` | ID de menú e ID confirmado de cuerpo | Sesión vigente, coincidencia con selección y todos los requisitos |

Las acciones se ejecutan en el hilo del servidor. El cliente no envía nivel,
combustible, coordenadas de llegada ni permisos. El atajo comprueba chunks ya
cargados; no explora ni carga dimensiones remotas para buscar un cohete.

## Materiales y arte

`scripts/generate-navigation-assets.mjs` introduce **28 máscaras minerales
distintas**, declaradas en `texture_styles.json`. Cada máscara tiene su propia
geometría, relieve y sombras; no usa un sprite vanilla recoloreado.

| Ejemplos | Incrustación |
| --- | --- |
| Luna | Filamentos ondulados paralelos |
| Mercurio | Agujas metálicas inclinadas |
| Venus | Vesículas y anillos de azufre |
| Marte | Estratos angulares y uniones de metal |
| Europa | Bolsas cristalinas hexagonales |
| Ganímedes | Cintas trenzadas |
| Calisto | Anillos de impactos mineralizados |
| Titán | Ramificaciones de ámbar |
| Titania | Fracturas poligonales |
| Miranda | Terrazas y canales escalonados |
| Neptuno | Espiral de tormenta |
| Tritón | Cristales estrellados de seis brazos |

Los otros patrones están enumerados en el manifiesto. El titanio terrestre tiene
una textura propia de plaquetas metálicas. Los recursos extraídos comparten la
paleta del mineral y tienen siluetas y facetas diferenciadas.

Las rocas y los suelos también se rediseñan: microcráteres y grano lunar, grietas
y óxido marciano, basalto poroso venusiano, estratificaciones e hielos exteriores.
Cada destino añade **ladrillos de su roca**, fabricados 2×2, con modelos, botín,
receta y traducciones propios. Se añaden tres bloques industriales:

- `reinforced_panel`: panel reforzado con cruceta y tornillería.
- `vent_grate`: rejilla metálica sólida decorativa.
- `energy_cell`: celda decorativa luminosa de nivel 10. No almacena energía ni
  reemplaza el combustible del cohete.

Bloques, minerales y ladrillos son PNG de **128×128**; los recursos extraídos,
64×64. El mapa incorpora 30 iconos de 32×32: Sol, Tierra y los 28 destinos nuevos.
El arte es procedural original con sombreado integrado, no un paquete PBR.
Los bloques nuevos están en la pestaña GalaxyCraft y requieren pico de hierro.
Los cambios de textura se ven también en bloques ya colocados; los ladrillos y
paneles se obtienen por crafteo y no reemplazan automáticamente construcciones.

## Archivos y validación

Rutas relativas al proyecto abierto en IntelliJ:

| Función | Archivo/carpeta |
| --- | --- |
| Pantalla | `src/main/java/com/vic17usd/galaxycraft/client/CelestialMapScreen.java` |
| Teclas | `client/EquipmentKey.java`, `client/ClientGameEvents.java` bajo el paquete Java |
| Menú | `src/main/java/com/vic17usd/galaxycraft/menu/NavigationMenu.java` |
| Reglas, jerarquía y paquetes | `src/main/java/com/vic17usd/galaxycraft/navigation/` |
| Sistema e identidades artísticas | `src/main/resources/stellar_systems.json`, `texture_styles.json` |
| Iconos | `src/main/resources/assets/galaxycraft/textures/gui/celestial/` |
| PNG y modelos | `src/main/resources/assets/galaxycraft/textures/block/`, `models/` |
| Generación reproducible | `scripts/generate-navigation-assets.mjs` |

```bash
node scripts/generate-solar-system.mjs
./gradlew --no-daemon build
```

El generador principal encadena todos los acabados; regenerar no restaura texturas
anteriores. Node/FFmpeg no son dependencias del JAR.

Las pruebas incluyen jerarquía completa, motivos de bloqueo, límites de combustible,
codecs de paquetes, iconos y patrones distintos. El GameTest `navigation_authority`
usa registros y un jugador simulado para comprobar selección inválida, nivel,
combustible, oxígeno, confirmación obsoleta, distancia y menú cerrado; también
verifica que cancelar un inicio válido no gasta combustible.

```bash
./gradlew --no-daemon runServer -PgalaxycraftGameTests
```

En consola, en un mundo de desarrollo:

```text
execute positioned 13000 200 13000 run test run navigation_authority
```

Espera `GALAXYCRAFT_INTEGRATION_OK navigation_authority` antes de otra prueba.
La revisión de la interfaz, el ratón, las teclas y el audio con un cliente gráfico
queda pendiente; los tests del servidor no verifican su apariencia en pantalla.

Validación de la entrega inicial 0.4.0: **199 pruebas JUnit aprobadas** y **tres GameTests**
aprobados (`navigation_authority`, `equipment_round_trip`, `rocket_multiblock_cleanup`).
Se analizaron 1701 JSON y 351 PNG. Los recursos se regeneraron sin cambios de hash.
El servidor cargó las nuevas recetas y registros y se detuvo con guardado normal.
Durante la preparación del área de GameTests aparecieron avisos de block entities
vanilla provisionales, y persiste el aviso de red de Yggdrasil del entorno; no
impidieron completar las pruebas. La generación de las 145 estructuras mantiene
el informe de validación de 0.3.0, ya que esta entrega no modifica sus plantillas.
