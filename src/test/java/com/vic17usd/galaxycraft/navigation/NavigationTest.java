package com.vic17usd.galaxycraft.navigation;

import com.google.gson.JsonParser;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.buffer.Unpooled;
import java.io.InputStreamReader;
import java.util.HashSet;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Contratos de jerarquía, autorización, protocolo e iconografía del ordenador de navegación. */
class NavigationTest {
    @Test void jerarquiaIncluyeTodosLosDestinosUnaSolaVez() {
        var seen = new HashSet<String>();
        for (var system : CelestialCatalog.SYSTEMS) {
            assertTrue(system.minimumTier() >= 1 && system.minimumTier() <= 3);
            for (var planet : CelestialCatalog.planets(system)) {
                assertTrue(seen.add(planet.id()));
                assertEquals(system,CelestialCatalog.system(planet).orElseThrow());
                for (var moon : CelestialCatalog.moons(planet)) {
                    assertTrue(seen.add(moon.id())); assertEquals(system,CelestialCatalog.system(moon).orElseThrow());
                }
            }
        }
        assertEquals(SolarSystem.BODIES.size(),seen.size());
        assertEquals(8,CelestialCatalog.planets(CelestialCatalog.SYSTEMS.getFirst()).size());
        assertEquals(1,CelestialCatalog.moons(CelestialCatalog.body("earth").orElseThrow()).size());
        assertTrue(CelestialCatalog.body("unknown").isEmpty());
    }
    private NavigationRules.BlockReason reason(int tier,int required,int fuel,int readiness,boolean same,boolean oxygen,boolean available,boolean igniting,boolean pilot) {
        return NavigationRules.evaluate(tier,required,fuel,4000,readiness,same,oxygen,available,igniting,pilot);
    }
    @Test void restriccionesDanMotivosPrecisosYNoAceptanCombustibleInsuficiente() {
        var ready=NavigationRules.BlockReason.READY;
        for (int tier=1;tier<=3;tier++) for (int required=1;required<=3;required++) for(int fuel:new int[]{0,1000,3999,4000}) {
            var actual=reason(tier,required,fuel,3,false,true,true,false,true);
            assertEquals(tier<required?NavigationRules.BlockReason.TIER:fuel<4000?NavigationRules.BlockReason.FUEL:ready,actual);
        }
        assertEquals(NavigationRules.BlockReason.OXYGEN,reason(3,1,4000,3,false,false,true,false,true));
        assertEquals(NavigationRules.BlockReason.PAD,reason(3,1,4000,0,false,true,true,false,true));
        assertEquals(NavigationRules.BlockReason.CLEARANCE,reason(3,1,4000,1,false,true,true,false,true));
        assertEquals(NavigationRules.BlockReason.SAME_WORLD,reason(3,1,4000,3,true,true,true,false,true));
        assertEquals(NavigationRules.BlockReason.UNAVAILABLE,reason(3,1,4000,3,false,true,false,false,true));
        assertEquals(NavigationRules.BlockReason.IGNITING,reason(3,1,4000,3,false,true,true,true,true));
        assertEquals(NavigationRules.BlockReason.PILOT,reason(3,1,4000,3,false,true,true,false,false));
        assertEquals(NavigationRules.BlockReason.TIER,reason(4,1,4000,3,false,true,true,false,true));
    }
    @Test void paquetesConservanSesionYDestinoEstable() {
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            var selected=new NavigationNetworking.Select(17,ModPresentation.id("moon"));
            NavigationNetworking.Select.CODEC.encode(buffer,selected);assertEquals(selected,NavigationNetworking.Select.CODEC.decode(buffer));
            var launch=new NavigationNetworking.Launch(19,ModPresentation.id("triton"));
            NavigationNetworking.Launch.CODEC.encode(buffer,launch);assertEquals(launch,NavigationNetworking.Launch.CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
    @Test void todosLosCuerposTienenIconoYLosMineralesPatronesDistintos() throws Exception {
        var names=new java.util.ArrayList<>(SolarSystem.BODIES.stream().map(body->body.id()).toList());names.add("sun");
        for (String name:names) try(var input=getClass().getResourceAsStream("/assets/galaxycraft/textures/gui/celestial/"+name+".png")) {
            assertNotNull(input,name);var image=ImageIO.read(input);assertEquals(32,image.getWidth());assertEquals(32,image.getHeight());
        }
        try(var stream=getClass().getResourceAsStream("/texture_styles.json")) {
            var array=JsonParser.parseReader(new InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();
            assertEquals(28,array.size());var patterns=new HashSet<String>();var hashes=new HashSet<String>();
            for(var entry:array) {
                var style=entry.getAsJsonObject();assertTrue(patterns.add(style.get("ore_pattern").getAsString()));
                try(var input=getClass().getResourceAsStream("/assets/galaxycraft/textures/block/"+style.get("body").getAsString()+"_ore.png")) {
                    assertNotNull(input);assertTrue(hashes.add(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(input.readAllBytes()))));
                }
            }
        }
    }
}
