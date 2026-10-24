package com.vic17usd.galaxycraft.presentation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Detecta referencias rotas, sprites corruptos y cofres sin botín antes de distribuir el JAR. */
class PresentationAssetsTest {
    private JsonObject json(String file) throws Exception {
        try (var input = getClass().getResourceAsStream("/" + file)) {
            assertNotNull(input, file);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    @Test void modelosSoloReferencianTexturasPropiasQueExisten() throws Exception {
        Path root = Path.of(getClass().getResource("/assets/galaxycraft/models").toURI());
        try (var files = Files.walk(root)) {
            for (var file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                var model = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                if (model.has("parent") && model.get("parent").getAsString().startsWith("galaxycraft:")) {
                    json("assets/galaxycraft/models/" + model.get("parent").getAsString().split(":")[1] + ".json");
                }
                if (!model.has("textures")) continue;
                for (var value : model.getAsJsonObject("textures").asMap().values()) {
                    String texture = value.getAsString();
                    if (texture.startsWith("#")) continue;
                    assertTrue(texture.startsWith("galaxycraft:"), file + ": textura provisional " + texture);
                    try (var input = getClass().getResourceAsStream("/assets/galaxycraft/textures/" + texture.split(":")[1] + ".png")) {
                        assertNotNull(input, texture);
                        var image = ImageIO.read(input);
                        assertNotNull(image, texture);
                        assertTrue(image.getWidth() >= 16 && image.getHeight() >= 16);
                    }
                }
            }
        }
    }
    @ParameterizedTest
    @ValueSource(strings = {"lunar_research_module", "martian_alien_ruins", "orbital_satellite"})
    void estructurasTienenPlantillasValidasYCofresConBotin(String name) throws Exception {
        var structure = json("data/galaxycraft/worldgen/structure/" + name + ".json");
        assertEquals("galaxycraft:" + name, structure.get("start_pool").getAsString());
        // En JigsawPlacement de 1.21, size=0 no añade ni la pieza inicial.
        assertTrue(structure.get("size").getAsInt() > 0);
        var pool = json("data/galaxycraft/worldgen/template_pool/" + name + ".json");
        assertEquals("galaxycraft:" + name, pool.getAsJsonArray("elements").get(0).getAsJsonObject()
                .getAsJsonObject("element").get("location").getAsString());
        try (var input = getClass().getResourceAsStream("/data/galaxycraft/structure/" + name + ".nbt")) {
            assertNotNull(input);
            var nbt = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
            var size = nbt.getList("size", 3);
            var palette = nbt.getList("palette", 10);
            var blocks = nbt.getList("blocks", 10);
            int chests = 0;
            for (var block : blocks) {
                var entry = (net.minecraft.nbt.CompoundTag) block;
                assertTrue(entry.getInt("state") >= 0 && entry.getInt("state") < palette.size());
                var pos = entry.getList("pos", 3);
                for (int axis = 0; axis < 3; axis++) assertTrue(pos.getInt(axis) >= 0 && pos.getInt(axis) < size.getInt(axis));
                if (entry.contains("nbt")) {
                    String loot = entry.getCompound("nbt").getString("LootTable");
                    assertEquals("galaxycraft:chests/" + name, loot);
                    json("data/galaxycraft/loot_table/chests/" + name + ".json");
                    chests++;
                }
            }
            assertEquals(1, chests);
        }
    }
    @Test void sonidosYParticulasTienenArchivosPropios() throws Exception {
        var sounds = json("assets/galaxycraft/sounds.json");
        for (var event : sounds.asMap().values()) for (var sound : event.getAsJsonObject().getAsJsonArray("sounds")) {
            String name = sound.getAsJsonObject().get("name").getAsString().split(":")[1];
            try (var input = getClass().getResourceAsStream("/assets/galaxycraft/sounds/" + name + ".ogg")) {
                assertNotNull(input);
                assertEquals("OggS", new String(input.readNBytes(4), StandardCharsets.US_ASCII));
            }
        }
        for (String name : new String[]{"rocket_exhaust", "rocket_smoke", "lunar_dust"}) {
            var particle = json("assets/galaxycraft/particles/" + name + ".json");
            assertEquals("galaxycraft:" + name, particle.getAsJsonArray("textures").get(0).getAsString());
            try (var input = getClass().getResourceAsStream("/assets/galaxycraft/textures/particle/" + name + ".png")) {
                assertNotNull(input); assertNotNull(ImageIO.read(input));
            }
        }
    }
    @Test void todosLosItemsTienenTraduccionYDescripcionEnAmbosIdiomas() throws Exception {
        try (var files = Files.list(Path.of(getClass().getResource("/assets/galaxycraft/models/item").toURI()))) {
            var names = files.map(file -> file.getFileName().toString().replace(".json", "")).toList();
            for (String language : new String[]{"es_es", "en_us"}) {
                var translations = json("assets/galaxycraft/lang/" + language + ".json");
                for (String name : names) {
                    assertTrue(translations.has("item.galaxycraft." + name) || translations.has("block.galaxycraft." + name), name);
                    assertTrue(translations.has("tooltip.galaxycraft.detail." + name), name);
                }
            }
        }
    }
}
