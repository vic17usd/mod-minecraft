# GalaxyCraft 0.6.0 — Combate, clima y redes

Requiere Minecraft **1.21 exacto**, NeoForge **21.0.167** y Java **21**.
Reemplaza el JAR anterior tanto en cliente como en servidor y guarda una copia
del mundo. No necesitas otros mods. El contenido aparece en la sección
**Combate y redes** de la pestaña GalaxyCraft, con recetas y traducciones es/en.

## Jefes en las bóvedas profundas

Las estructuras siguen siendo cinco por cuerpo celeste. Se han actualizado
`moon_vault` y `mars_vault` para incluir un sello de encuentro:

| Lugar | Jefe | Vida | Recompensa exclusiva |
| --- | --- | --- | --- |
| Bóveda lunar | Guardián Mecánico Lunar | 300 | Núcleo del Guardián Lunar |
| Bóveda marciana | Reina Alienígena Marciana | 360 | Corazón de la Reina Marciana |

Al acercarse un jugador a menos de 12 bloques, el sello despierta al jefe si hay
espacio y la dificultad no es Pacífico. Cada sello genera **un único encuentro**:
guarda UUID, activación y derrota. Descargar el chunk o reiniciar no duplica al
jefe. Los sellos no se pueden minar en supervivencia ni sueltan objetos.

Ambos enemigos tienen barra de jefe, ataques cuerpo a cuerpo, proyectiles de
plasma y una onda cercana de daño/empuje. Por debajo de media vida disparan con
mayor frecuencia. El guardián tiene más armadura y un modelo bípedo industrial;
la reina tiene un modelo acorazado de ocho patas, más vida y mayor daño cercano.
Los proyectiles no explotan ni rompen bloques. El botín usa loot tables normales
y respeta `doMobLoot`.

La progresión normal cambia así:

- N1 → Luna → Guardián → **propulsor estabilizado** → cohete N2.
- N2 → Marte → Reina → **núcleo de navegación avanzado** → cohete N3.

El propulsor estabilizado consume un núcleo lunar, un propulsor anterior y un
fragmento meteorítico. El núcleo avanzado consume un corazón marciano, un núcleo
de navegación anterior y metal marciano. Los jefes también dejan fragmentos
meteoríticos: no es obligatorio esperar al primer evento para progresar.
Los cohetes ya fabricados en mundos antiguos siguen funcionando.

Los sellos aparecen en **bóvedas generadas en chunks nuevos**. No se modifican ni
se regeneran construcciones de un mundo antiguo. Para localizar una bóveda:

```mcfunction
/execute in galaxycraft:moon run locate structure galaxycraft:moon_vault
/execute in galaxycraft:mars run locate structure galaxycraft:mars_vault
```

Desde creativo u operador se pueden probar los jefes directamente:

```mcfunction
/summon galaxycraft:lunar_guardian
/summon galaxycraft:martian_queen
```

Un jefe invocado por comando no tiene un sello propietario. Si se elimina por
comando o Pacífico un jefe de mazmorra ya despertado, su sello no lo recrea: evita
duplicaciones al cambiar dificultad o descargar entidades.

## Hostiles planetarios

Hay **28 registros de saqueador**, uno por cada planeta/luna extraterrestre del
catálogo; la Tierra conserva sus hostiles vanilla. Se añaden por modificadores
de bioma, en grupos de uno o dos y con peso 12. No necesitan oscuridad, pero sí
condiciones de aparición válidas y dificultad distinta de Pacífico. Por tanto,
registrar su aparición no garantiza encontrar uno inmediatamente: siguen sujetos
al límite de mobs y las reglas del servidor.

Cada registro tiene piel propia, salud/daño ajustados al nivel planetario, no arde
al sol ni se convierte al sumergirse. La gravedad usa el sistema existente y no
necesita oxígeno de jugador. Los saqueadores exteriores añaden ralentización al
golpear. Sueltan pequeñas cantidades del recurso de su planeta. Se mantienen los
reptadores de las fases anteriores.

Ejemplo: `/summon galaxycraft:mars_raider`.

## Clima espacial dinámico

El calendario se guarda mediante SavedData **por dimensión**. Solo se producen
eventos en mundos extraterrestres y órbitas con jugadores; no afectan al
Overworld, Nether ni End. Se usa tiempo de juego, no la hora del día: cambiar
`/time` no reinicia ni repite un evento.

El intervalo predeterminado es **12000 ticks (10 minutos)** después del evento
anterior. El primer evento espera ese intervalo al entrar en el mundo.

- **Tormenta de polvo marciana:** 2400 ticks (2 minutos), partículas, niebla rojiza
  limitada a 36 bloques y ralentización al exterior con cielo visible. Los
  paneles solares producen 8 FE/tick en vez de 40.
- **Lluvia de meteoritos:** 1200 ticks (1 minuto). Se crean como máximo dos
  meteoritos por dimensión cada cinco segundos, cerca de jugadores y solo en
  terreno ya cargado. Son entidades visibles con estela; no se crean chunks
  nuevos para encontrar un punto de impacto.
- **Tormenta solar:** 1800 ticks (90 segundos). Añade 8 unidades de intensidad a
  la radiación ambiental y detiene la generación solar. El plomo reduce la
  exposición, pero no anula una tormenta fuerte: conviene refugiarse en una sala
  sellada con FE y oxígeno o disponer de reservas de energía.

Marte alterna polvo/meteoritos/radiación; los demás mundos alternan meteoritos y
radiación. Las órbitas solo reciben tormentas solares. El HUD muestra el evento
actual y los segundos restantes. La exposición exterior y la protección interior
se sincronizan con el mismo paquete S2C del HUD; el cliente no decide el clima.

### Impactos y protección del terreno

Un impacto puede alterar **como máximo cinco celdas** de una sola capa: cuatro
vecinos y el centro, donde queda un meteorito mineralizado. Al minarlo con la
herramienta correcta entrega fragmentos raros de meteorito.

Solo se reemplazan roca y regolito planetarios de la etiqueta
`galaxycraft:meteor_replaceable`. No se incluyen paneles, máquinas, cofres,
minerales planetarios ni bloques vanilla. Se evitan chunks con referencias a
estructuras y zonas cercanas a máquinas de bases o depósitos. No se usa una
explosión destructiva; el efecto de explosión es únicamente sonido/partículas.

**La etiqueta no distingue roca natural de la misma roca colocada por un jugador.**
Una construcción hecha únicamente con roca planetaria, lejos de las protecciones
anteriores, puede verse afectada. Desactiva impactos si no deseas ese riesgo.
Con `meteorTerrainImpacts=false` o `mobGriefing=false`, el impacto deja fragmentos
como objetos, sin modificar el terreno.

### Configuración y comandos

En el archivo `galaxycraft-server.toml` generado por NeoForge (en este entorno de
desarrollo, `run/server/config/galaxycraft-server.toml`):

```toml
spaceWeatherEvents = true
meteorTerrainImpacts = true
spaceWeatherIntervalTicks = 12000
```

Se conservan `simulateVacuum` y `airlessDimensions`. Un operador con permiso 2
puede forzar eventos en la dimensión desde la que ejecuta el comando:

```mcfunction
/galaxyweather dust
/galaxyweather meteors
/galaxyweather solar
/galaxyweather clear
```

Polvo solo es válido en Marte; meteoritos no son válidos en órbita. Los comandos
también rechazan la Tierra y otras dimensiones sin perfil espacial. Si los eventos
están deshabilitados, no generan efectos aunque exista un calendario guardado.

## Tres redes independientes

Las redes se conectan por caras, no diagonales, y solo entre conductos del mismo
tipo. Los modelos muestran el centro y sus brazos conectados. No hacen falta
paquetes de configuración del cliente: la topología se calcula en servidor.

| Red | Capacidad/API | Transferencia máxima por origen y pasada |
| --- | --- | --- |
| Cable eléctrico | `EnergyStorage.BLOCK` de NeoForge | 512 FE |
| Conducto de oxígeno | `galaxycraft:oxygen_storage` / `IOxygenStorage` | 60 unidades |
| Tubería de combustible | `FluidHandler.BLOCK` de NeoForge | 250 mB de lava |

Hay una pasada cada cinco ticks. Se priorizan presurizadores, máquinas consumidoras
y cohetes frente a equilibrar depósitos. Los almacenes de igual porcentaje de
llenado no recirculan contenido continuamente. Cortar un tramo corta la conexión.

Cada componente admite **hasta 256 conductos**. Una red mayor no transfiere
parcialmente: divídela mediante depósitos/intercambiadores. Por dimensión se
limitan las pasadas a 4096 nodos y 32 componentes con rotación del punto de inicio.
Solo se consultan bloques cargados; no hay carga forzada ni búsqueda ilimitada.

Los conductos no son paredes herméticas. Para atravesar una base, usa una máquina
de pared como terminal: el detector es un bloque completo y acepta oxígeno por la
cara exterior. Abrir un agujero en la pared para pasar un conducto produce fuga.

### Energía

Conecta paneles solares o acoplamientos a cables y de ahí a detectores,
generadores de gravedad, bancos y refinerías. Las máquinas consumidoras existentes
no se usan como fuentes para vaciar sus reservas. Las capacidades permiten
integración con otros mods compatibles; no se prometen compatibilidades con mods
que no se hayan probado.

El **banco de energía** guarda **1000000 FE**, empieza vacío y carga baterías
extraíbles en su ranura. La refinería también tiene reserva de FE. Sus GUI separan
los enteros grandes en dos palabras de 16 bits, evitando que la cantidad se
trunque a 32767 al sincronizarse por el menú vanilla.

### Oxígeno

El **depósito de oxígeno** guarda **128000 unidades**. Inserta un tanque portátil
lleno para transferir su contenido; retíralo vacío y recárgalo en un mundo
respirable. No produce aire de la nada ni extrae oxígeno del ambiente espacial.

Conecta depósito → conductos → detector/presurizador. El detector necesita tener
un tanque válido en su ranura, aunque esté vacío: la red lo rellena sin superar
su capacidad. Sin tanque receptor no se crea uno ni se debita oxígeno innecesario.
El detector sigue necesitando FE y una habitación sellada; recibir oxígeno por
tubería no elimina esos requisitos.

### Combustible y refinería

El **tanque masivo** guarda **128000 mB** y admite cubos de lava/vacíos y capacidades.
La **refinería térmica** procesa un carbón o carbón vegetal y **2000 FE** durante
100 ticks para producir **1000 mB de lava**. Se pausa si no tiene recurso,
energía o espacio. Es una conversión de jugabilidad, no una simulación industrial.

Se conserva la lava como combustible de las fases anteriores: **no** se cambia el
fluido de los cohetes ni se registra petróleo/queroseno. Las tuberías rechazan
agua y no vacían un cohete para llenar otros depósitos.

Conecta refinería/tanque → tubería → **parte inferior o lateral de la plataforma
3×3**. La plataforma es el puerto dinámico de repostaje y localiza el cohete que
soporta; no es necesario romper sus piezas 3D para acceder al bloque central. El
puerto solo admite entrada, deja de aceptar al quitar el cohete y vuelve a
funcionar al colocar otro. La plataforma debe estar completa.

Las reservas de FE, gas y fluido se guardan en las BlockEntities al guardar/cargar
el mundo. **Vacía los depósitos antes de romperlos**: el ítem de bloque no conserva
esas reservas. El contenido de la ranura sí cae como objeto al retirar la máquina.

## Archivos y validación

```text
src/main/java/com/vic17usd/galaxycraft/
  frontier/    Registros diferidos de entidades, bloques, ítems y menús
  combat/      Jefes, sellos, hostiles y proyectiles
  weather/     Calendario persistente, eventos e impactos limitados
  logistics/   Grafos, capacidades, transferencia, depósitos, GUI y puerto de plataforma
  client/      Modelos de jefes, pieles, niebla, HUD y pantalla de depósitos
  testing/     GameTests nuevos y regresiones
scripts/generate-frontiers.mjs
scripts/generate-advanced.mjs       Inclusión de sellos en bóvedas nuevas
src/test/java/com/vic17usd/galaxycraft/frontier/FrontiersTest.java
```

`node scripts/generate-solar-system.mjs` regenera los recursos, incluidas recetas,
texturas, estructuras, botín y traducciones. Los recursos se distribuyen ya
generados; instalar el JAR no necesita Node.

```bash
./gradlew --no-daemon build --console=plain
./gradlew --no-daemon runServer -PgalaxycraftGameTests --console=plain
```

Los GameTests solo se habilitan explícitamente en desarrollo. Usar un mundo de
pruebas, no una partida valiosa, y ejecutar las pruebas secuencialmente esperando
su marcador `GALAXYCRAFT_INTEGRATION_OK`.

Queda pendiente probar en un cliente gráfico real las animaciones, la niebla,
las barras de jefe y la experiencia multijugador con latencia. Las pruebas de
servidor no equivalen a esa revisión visual ni a una garantía de balance final.

Validación: **217 pruebas JUnit y 16 GameTests aprobados**. Detalles y alcance en
el [registro de validación](validation/frontiers-0.6.0.md).
