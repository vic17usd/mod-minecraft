# Fase 3: plataforma y cohete de nivel 1

Minecraft **1.21**, NeoForge **21.0.167**, Java **21**. Se utiliza la opción de
**bloque con BlockEntity**: el cohete permanece estacionario, contiene inventario
y fluido y presenta un menú. No hay una entidad voladora, pasajeros, cuenta atrás
ni transporte a la Luna: esa dimensión todavía no existe. La GUI indica la
preparación mecánica y señala explícitamente que el viaje lunar está pendiente.

## Construcción y uso

1. Fabrica nueve `galaxycraft:launch_pad` y colócalos como un cuadrado completo de
   **3×3**, todos a la misma altura. Se usan bloques completos con textura provisional.
2. Coloca `galaxycraft:tier1_rocket` **encima del bloque central**. El ítem comprueba
   la base y exige dos bloques de aire encima del cohete, dentro del límite del mundo.
   No fuerza la carga de chunks vecinos para encontrar la plataforma.
3. Con la mano vacía, haz clic derecho sobre el cohete para abrir la GUI. La
   plataforma es una base estructural; la interfaz pertenece al cohete.
4. Introduce cubos de lava en la ranura izquierda. Cada segundo de juego se
   procesa uno y su cubo vacío pasa a la ranura derecha. Retira esos envases cuando
   se llene la salida. Las dos ranuras admiten transferencia con Mayús + clic.
5. El depósito acepta **4000 mB**, equivalentes a **cuatro cubos**. La lava es un
   combustible provisional para probar almacenamiento y automatización, no un
   modelo físico de propulsión espacial.

La GUI muestra combustible en mB, barra y uno de estos estados, en este orden:
base 3×3 incompleta, espacio superior obstruido, combustible insuficiente o
preparado para lanzamiento. No existe botón de lanzamiento todavía. La preparación
no exige casco ni oxígeno porque aún no hay embarque ni viaje.

Si se retira parte de la base o se coloca un bloque encima, el cohete permanece
en su sitio pero deja de estar preparado. La comprobación se realiza en el servidor
y se sincroniza mientras está abierto el menú. El combustible **no se consume**
simplemente por alcanzar el estado preparado.

## Inventario, automatización y persistencia

- Entrada: solo `minecraft:lava_bucket`; salida: extracción de cubos vacíos.
- Una transferencia verifica primero el espacio del depósito y de la salida.
  Si solo quedan 500 mB, no consume ni vierte parcialmente un cubo de 1000 mB.
- NeoForge `Capabilities.FluidHandler.BLOCK` expone el depósito en todas las caras.
  Se puede llenar y vaciar mediante tuberías compatibles; rechaza agua y otros fluidos.
- `Capabilities.ItemHandler.BLOCK` expone ambas ranuras. Solo se permite insertar
  cubos de lava en la entrada. Los handlers mantienen identidad estable.
- Inventario y fluido se guardan y restauran con los métodos de BlockEntity de 1.21
  y `HolderLookup.Provider`. Este uso de NBT es correcto para máquinas: los Data
  Components de los tanques portátiles siguen siendo independientes.
- Al romper el cohete, se sueltan los objetos de su inventario y, con herramienta
  adecuada, su ítem vacío. **El fluido se pierde al desmontarlo**: drénalo primero
  con una tubería si quieres conservarlo. El ítem de cohete no almacena combustible.
- Las ranuras y el estado usan la sincronización normal de menús de Minecraft;
  los dos enteros enviados son menores de 32768. No hay paquetes personalizados.
- El menú valida la distancia y que siga existiendo el bloque del cohete.

## Recetas

Se utiliza el mineral de titanio de la fase 1, porque aún no hay titanio refinado.

```text
Plataforma (produce 9):     Cohete (produce 1):
TIT                        TGT
III                        TOT
TIT                        FIF
```

`T`: mineral de titanio; `I`: lingote de hierro en la plataforma y bloque de hierro
en el cohete; `G`: cristal; `O`: tanque de oxígeno de la fase 2; `F`: horno.
El tanque usado como ingrediente se convierte en parte del cohete: su componente
de oxígeno no se transfiere ni proporciona oxígeno al pasajero en esta fase.
Las recetas se pueden fabricar manualmente; aún no tienen advancements del libro.

## Archivos principales

Todas las rutas parten de `/workspace/mod-minecraft`:

```text
src/main/java/com/vic17usd/galaxycraft/
  block/LaunchPadBlock.java             Comprobación de estructura 3×3
  block/RocketBlock.java                Interacción, ticker y desmontaje
  blockentity/RocketBlockEntity.java    Inventario, depósito, estado y persistencia
  item/RocketItem.java                  Restricciones de colocación
  rocket/RocketFueling.java             Transacción de combustible y envase
  menu/RocketMenu.java                  Ranuras, sincronización y Mayús + clic
  client/RocketScreen.java              GUI provisional con barra y estados
  client/ClientModEvents.java           Registro de pantalla solo en Dist.CLIENT
  registry/ModRocketRegistries.java     Tipos de BlockEntity/menú y capabilities
  registry/ModBlocks.java               Registro de los dos bloques
  registry/ModItems.java                Ítems y pestañas creativas
src/main/resources/assets/galaxycraft/
  blockstates/{launch_pad,tier1_rocket}.json
  models/block/{launch_pad,tier1_rocket}.json
  models/item/{launch_pad,tier1_rocket}.json
  lang/{es_es,en_us}.json
src/main/resources/data/galaxycraft/
  recipe/{launch_pad,tier1_rocket}.json
  loot_table/blocks/{launch_pad,tier1_rocket}.json
src/main/resources/data/minecraft/tags/block/
  mineable/pickaxe.json
  needs_iron_tool.json
src/test/java/com/vic17usd/galaxycraft/rocket/RocketFuelingTest.java
```

El modelo del cohete es una maqueta de un bloque de altura, con cuerpo, nariz,
aletas, motor y ventanilla. Usa texturas vanilla de hierro, hormigón y piedra
negra. La colisión es la de un bloque completo. La GUI se dibuja con rectángulos
y no necesita PNG externos. Estas representaciones son provisionales.

`GalaxyCraft.java` conecta BlockEntities, menús y capabilities al bus del mod.
El cliente registra su pantalla mediante `RegisterMenuScreensEvent`, con
`Dist.CLIENT`, para que el servidor dedicado no cargue clases de interfaz.

## Pruebas

```bash
# En la nube, activa antes el entorno Java que ya utiliza el proyecto.
source /workspace/.toolchains/activate-galaxycraft.sh
./gradlew build
./gradlew runServer
```

Los cinco tests de combustible comprueban la conservación del envase, llenado
atómico cuando no cabe un cubo completo, bloqueo por salida llena o incompatible,
cuatro cubos sin duplicación y rechazo de agua/simulación sin cambios. Se conservan
los cinco tests de oxígeno de la fase 2.

Para comprobarlo como jugador, abre `runClient` y utiliza:

```mcfunction
/give @s galaxycraft:launch_pad 9
/give @s galaxycraft:tier1_rocket
/give @s minecraft:lava_bucket 4
```

En un mundo descartable, prueba colocación en base incompleta y completa, apertura
de GUI, entrada/salida y Mayús + clic, cuatro cargas y límite de capacidad. Rompe
un bloque de plataforma y obstruye el espacio superior para comprobar los estados.
Prueba también llenado y drenaje por tuberías cuando haya un mod compatible.
Guarda y vuelve a entrar para comprobar el depósito, y desmonta con un pico de
hierro para verificar que se devuelven objetos sin duplicarlos. Escribe `stop` para
guardar y cerrar un servidor de desarrollo.

## Resultado de validación en la nube

- Compilación con Gradle correcta y diez tests JUnit superados, sin fallos ni omisiones.
- Los 25 archivos JSON del proyecto son sintácticamente válidos.
- Servidor dedicado arrancado dos veces, con los bloques y la BlockEntity nuevos;
  1294 recetas cargadas, dos más que en la fase 2.
- En una base de prueba, el ticker convirtió un cubo de lava en 1000 mB de fluido
  y un cubo vacío. Tras guardar, detener y reiniciar, se conservaron ambos valores.
- Las clases de cliente no impidieron arrancar el servidor dedicado.

La comprobación con comandos no sustituye la colocación usando el ítem ni abrir
la GUI como jugador. El renderizado, interacción y transferencias Mayús + clic,
actualización visual de los estados y conexión a tuberías de otro mod quedan
pendientes de un cliente gráfico. El aviso externo de claves públicas de Mojang
de las fases anteriores continúa presente; no bloqueó estas pruebas.
