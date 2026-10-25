# GalaxyCraft 0.7.0 — Biosferas y civilizaciones

Minecraft **1.21 exacto**, NeoForge **21.0.167**, Java **21**. Sustituye el JAR
anterior en cliente y servidor; no instales dos versiones. Haz una copia del mundo.

## Alcance y migración

La ampliación añade 145 materiales de estructura, 87 cultivos, tres especies
ganaderas por cuerpo celeste, colonos con comercio/contratos y depredadores de
hábitat. Se mantienen las cinco expediciones originales por mundo y se añade una
familia independiente de asentamientos, con 29 definiciones de colonia.

Las 28 dimensiones extraterrestres tienen un generador extremo y tres hábitats.
La Tierra conserva el generador y cielo vanilla: sus plantas, ganadería alienígena
y colonos aparecen en asentamientos puntuales, no por invasión de todos los biomas.

**Recomendación: probar el relieve en un mundo nuevo.** No se regeneran chunks ni
se reemplazan construcciones de una partida existente. Los mundos guardan su
configuración de generación; si conservan el generador antiguo, necesitan un mundo
nuevo para utilizar el nuevo. En límites entre terreno antiguo y nuevo pueden
aparecer cortes: no hay retrogen ni mezclador de terreno de versiones anteriores.

Los gigantes gaseosos conservan las superficies jugables de ficción del mod.
No se presenta su terreno, flora ni civilizaciones como astronomía realista.

## Materiales exclusivos de estructuras

Cada cuerpo tiene cinco bloques propios, con texturas originales de 128 px:

| Familia | Material |
| --- | --- |
| Observatorio | Paneles de aleación y circuitos |
| Pecio | Placas de blindaje recuperadas |
| Santuario | Sillares con glifos |
| Geoda | Cristal tallado |
| Bóveda | Ladrillos reforzados |

Son 29 × 5 = **145 bloques**, todos registrados con BlockItem, modelos, botín,
recetas y traducciones. Los paneles usan roca/recurso del planeta y titanio. Para
la Tierra se emplea piedra vanilla y titanio. Aparecen en la sección **Biosfera y
civilización** de la pestaña creativa.

Las plantillas de las 145 expediciones se actualizan para usar su material
estructural. Conservan IDs, cofres, esquemas, cinco familias y los sellos de jefe
de Luna/Marte. Solo las estructuras generadas después de actualizar incorporan
esas fachadas; las existentes no se recolorean automáticamente.

## Colonias habitadas

Los asentamientos se forman con una plaza, cuatro o cinco casas, huerto/pastura y
caminos de tres bloques. Cada módulo consulta la altura de su ubicación; se
rechazan superficies demasiado inclinadas o líquidas y se limita la profundidad
de los apoyos. No se aplana una región entera para colocar un pueblo.

Cada casa contiene un colono y un cofre con biomuestras, fichas y posibles esquemas.
El huerto incluye las tres plantas nativas y las tres especies ganaderas. Los
colonos observan, caminan cerca de su hogar y evitan monstruos; las escotillas se
pueden abrir manualmente. No se añade simulación de población, construcción
autónoma de casas, reproducción de NPCs ni protección antigrief.

Los estados de las piezas guardan sus apariciones para no duplicar habitantes al
procesar distintos chunks. Los colonos guardan hogar, existencias y reposición.
Los animales de las colonias son persistentes. Los colonos están adaptados a su
planeta, pero **una colonia no proporciona automáticamente aire respirable a un
humano**: sigue siendo necesario el traje o un presurizador operativo.

Para localizar colonias, con permisos de operador:

```mcfunction
/execute in galaxycraft:moon run locate structure galaxycraft:colony_moon
/execute in galaxycraft:mars run locate structure galaxycraft:colony_mars
/locate structure galaxycraft:colony_earth
```

La separación se basa en rejillas de 40 chunks con sal por planeta y filtros de
terreno; no hay una colonia en cada rejilla si las condiciones no son adecuadas.

### Comercio y contratos

Haz clic derecho en un colono para abrir su pantalla. Los botones se actualizan
con los valores recibidos del servidor:

- **Comprar tanque:** cuatro recursos nativos por un tanque de oxígeno lleno.
- **Vender biomuestras:** ocho biomuestras nativas por una ficha colonial.
- **Comprar recursos:** una ficha por cuatro recursos nativos.
- **Aceptar contrato:** habilita una entrega de 16 biomuestras del planeta.
- **Entregar contrato:** consume las 16 muestras y recompensa con dos fichas,
  cuatro recursos y 150 XP, **una sola vez por jugador y planeta**.

Los comercios comparten ocho existencias por NPC y se reponen cada 6000 ticks
(cinco minutos de juego). Aceptar o completar contratos no gasta existencias.
El progreso personal persiste y se conserva al morir. Con inventario lleno, el
excedente de recompensas cae al suelo.

Se acepta material de mochila/mano secundaria, no de cofres ni contenedores
anidados. Se requiere NPC vivo, jugador vivo/no espectador, dimensión nativa,
distancia de hasta seis bloques y sesión de menú válida. Los paquetes C2S solo
contienen ID de sesión y acción: no incluyen precios, pilas ni recompensas.

El libro **J** de las fases anteriores conserva sus diez misiones; los contratos
coloniales se gestionan aparte desde los NPCs. **G** y **H** siguen abriendo traje
y navegación. No se añaden controles en conflicto con estos atajos.

## Fauna y depredadores

Hay **87 registros ganaderos**, tres por cada uno de los 29 cuerpos:

- Bóvidos cristalinos: carne/proteína y placas cristalinas en el modelo.
- Ovejas luminosas: fibra bioluminiscente y aspecto brillante en la oscuridad.
- Jabalíes acorazados: caparazón y colmillos.

Tienen modelos Java propios, piel por registro, alimentación con la biomuestra de
su planeta, seguimiento del alimento, pánico y reproducción. Solo se reproducen
entre el mismo tipo y planeta. La esquila de una oveja adulta produce dos fibras
y tiene enfriamiento de 600 ticks guardado en disco. No se añade leche alienígena
ni transporte de animales en el cohete. El brillo del modelo de oveja no equivale
a una fuente de luz dinámica que ilumine otros bloques.

Los mundos extraterrestres añaden apariciones naturales; en la Tierra se generan
en colonias. Los límites y reglas vanilla de aparición siguen vigentes.

Se registran además **29 depredadores autóctonos**, con tres arquetipos de modelo e IA:

- Saltador cristalino: persecución y salto, especialmente peligroso con gravedad baja.
- Escupidor de carbono: disparo de plasma con línea de visión y enfriamiento.
- Cazador ígneo: combate cercano y quemaduras.

Comparten arquitectura pero tienen perfiles y pieles por mundo. No se afirman
29 IA independientes. Se mantienen los saqueadores, reptadores y jefes anteriores.
Los depredadores terrestres nuevos se reservan para invocación, no se añaden
globalmente a biomas vanilla.

Ejemplos para pruebas en creativo:

```mcfunction
/summon galaxycraft:moon_crystal_bovine
/summon galaxycraft:moon_lumen_sheep
/summon galaxycraft:mars_plated_boar
/summon galaxycraft:mars_stalker
/summon galaxycraft:venus_stalker
/summon galaxycraft:earth_colonist
```

## Flora y cultivos

Se añaden **tres plantas por cuerpo (87 bloques)** y una biomuestra nutritiva
específica por mundo. Los arquetipos incluyen hongos bioluminiscentes, enredaderas
de cristal cultivables, agujas criófilas, coral luminoso, helechos de carbono,
raíces ferruginosas, abanicos de esporas, espinas de azufre y fauces de magma.

Las plantas tienen modelos de tres edades, siluetas parametrizadas y textura
propia por registro. La Luna tiene hongos, enredaderas cristalinas y agujas;
Marte helechos de carbono, raíces y esporas; Venus fauces, espinas y juncos de ceniza.
Las plantas luminosas emiten luz de bloque según su edad. Las fauces y espinas
causan daño por contacto.

Solo se pueden cultivar en su **dimensión de origen** y sobre su **regolito
nativo**; las terrestres usan tierra/césped. La atmósfera se representa mediante
el perfil de dimensión, no mediante análisis químico por bloque. Un presurizador
o una maceta en otra dimensión no permiten saltarse el requisito, y no hay
invernaderos orbitales compatibles en esta entrega.

El crecimiento usa ticks aleatorios. El polvo de hueso adelanta una edad cuando
el hábitat es válido. Clic derecho sobre la planta madura entrega dos biomuestras
y reinicia el crecimiento; repetir el clic sin madurez no vuelve a recompensar.
Romperla devuelve el bloque plantable y, si está madura, biomuestras adicionales.
Las biomuestras sirven como alimento, pienso y mercancía.

Los respiraderos geotérmicos aparecen en las llanuras de géiseres. Emiten nubes y
empujan criaturas sobre ellos; los perfiles calientes también causan daño. Son
efectos locales acotados, sin explosiones ni generación de chunks remotos.

## Terreno y biomas

`ExtremePlanetGenerator` sustituye la generación de las 28 superficies
extraterrestres, con rango 0–255 y alturas coherentes entre columnas, chunks y
consultas de estructuras. La semilla proviene del RandomState del mundo y se
combina con la del perfil planetario; no es una semilla fija igual para todas las
partidas. No consulta otros chunks para calcular una columna.

Los perfiles combinan cinco familias de relieve:

| Familia | Ejemplos |
| --- | --- |
| Fisuras profundas con magma | Venus, Mercurio, Ío |
| Cráteres de 116–200 bloques de diámetro y bordes escarpados | Luna, Fobos, Calisto |
| Crestas montañosas | Ganímedes, Titania, Miranda |
| Dunas | Marte, Titán, superficies jugables de Júpiter/Saturno |
| Llanuras de géiseres | Europa, Encélado, Tritón |

Hay cavernas de ruido, roca/regolito propios y generación mineral de las fases
anteriores. Los géiseres no requieren simular tectónica ni fluidos criogénicos.

Cada planeta tiene tres biomas, seleccionados mediante ruido climático de la
semilla: superficie, fisura y vergel. Los vergeles tienen mayor densidad vegetal,
las fisuras menor; también difieren en ambiente. Los biomas quedan guardados en
el chunk: no se reescriben durante una tormenta. Lo dinámico del clima/cielo sigue
el sistema de eventos de 0.6.0, no un cambio de bioma en tiempo real.

## Cielos y atmósferas

Cada superficie extraterrestre registra `galaxycraft:sky/<cuerpo>` como efecto de
dimensión. El render propio incluye fondo por perfil, Sol, estrellas, nebulosa y
un disco del planeta padre o una luna vecina cuando corresponde. Venus tiene
niebla densa; Marte cielo anaranjado con estrellas visibles de día; las lunas sin
atmósfera mantienen un fondo oscuro y muestran su planeta padre prominente.

Se elimina el mediodía fijo de las dimensiones anteriores: ahora hay variación
diurna/nocturna, y los paneles solares deben contar con baterías para la noche.
Tiempo, entidades y estados se sincronizan por Minecraft; los cuerpos del cielo
son decorativos, no dimensiones adicionales ni una simulación de órbitas físicas.

La niebla respeta agua/lava y no sustituye la tormenta marciana de polvo. Los
cielos orbitales y el Overworld conservan sus renderizados anteriores. No se
necesita un paquete de shaders externo.

## Código, recursos y pruebas

```text
src/main/java/com/vic17usd/galaxycraft/
  biosphere/       Perfiles, registros, matemáticas de relieve, generador y BiomeSource
  ecology/         Plantas, respiraderos, colocación de flora, fauna y cazadores
  civilization/    Colonias/piezas, NPCs, contratos, menú y paquetes validados
  client/          ColonyScreen, modelos de vida, skybox y niebla
  testing/         BiosphereGameTests y regresiones
src/main/resources/living_worlds.json
scripts/generate-biosphere.mjs
src/test/java/com/vic17usd/galaxycraft/biosphere/BiosphereTest.java
```

Los generadores, fuentes de biomas y estructuras se registran con MapCodec en
DeferredRegister; las entidades tienen atributos y restricciones de aparición.
Los contratos son attachments persistentes. El NBT de entidades/piezas es
persistencia de mundo de 1.21, no NBT antiguo aplicado a los ItemStack.

Los recursos ya vienen en el JAR. Para regenerarlos en desarrollo:

```bash
node scripts/generate-solar-system.mjs
./gradlew --no-daemon build --console=plain
./gradlew --no-daemon runServer -PgalaxycraftGameTests -PgalaxycraftTestWorld --console=plain
```

La última opción usa el mundo aislado `run/ecology-validation`; las pruebas
colocan bloques y entidades y no deben ejecutarse en una partida valiosa. Consulta
el [registro de validación](validation/biosphere-0.7.0.md) para el alcance real.

Validado con **225 pruebas JUnit y 22 GameTests aprobados**, generación real de
chunks de cuatro perfiles y una colonia lunar natural con cinco colonos y fauna.

Queda pendiente revisar visualmente con un cliente gráfico los modelos, la niebla,
los cielos, las GUI y una sesión multijugador real. La compilación y las pruebas
de servidor no sustituyen esa revisión ni un balance prolongado de la economía.
