package com.vic17usd.galaxycraft.client;

import com.vic17usd.galaxycraft.menu.NavigationMenu;
import com.vic17usd.galaxycraft.navigation.CelestialCatalog;
import com.vic17usd.galaxycraft.navigation.NavigationNetworking;
import com.vic17usd.galaxycraft.navigation.NavigationRules;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import com.vic17usd.galaxycraft.solar.CelestialBody;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Locale;

/** Cartografía holográfica en tres niveles; ninguna acción cliente modifica combustible ni teletransporta. */
public final class CelestialMapScreen extends AbstractContainerScreen<NavigationMenu> {
    private int depth = 0;
    private CelestialCatalog.StellarSystem system = CelestialCatalog.SYSTEMS.getFirst();
    private CelestialBody planet;
    private CelestialBody focused;
    private Button confirm;
    private String lastReason = "";
    public CelestialMapScreen(NavigationMenu menu, Inventory inventory, Component title) {
        super(menu,inventory,title); focused = SolarSystem.destination(menu.destination());
    }
    @Override protected void init() {
        imageWidth = Math.min(420,width - 12); imageHeight = Math.min(278,height - 12);
        super.init(); rebuild();
    }
    private int cx() { return leftPos + 112 + (imageWidth - 122) / 2; }
    private int cy() { return topPos + 42 + (imageHeight - 92) / 2; }
    private int rx() { return (imageWidth - 142) / 2; }
    private int ry() { return (imageHeight - 122) / 2; }
    private void select(CelestialBody body) {
        focused = body;
        PacketDistributor.sendToServer(new NavigationNetworking.Select(menu.containerId,ModPresentation.id(body.id())));
        lastReason = "";
    }
    private void rebuild() {
        clearWidgets(); lastReason = "";
        addRenderableWidget(Button.builder(text("back"),button -> { if (depth > 0) { depth--; rebuild(); } })
                .bounds(leftPos + 8,topPos + imageHeight - 26,55,18).build()).active = depth > 0;
        addRenderableWidget(Button.builder(text("controls"),button -> PacketDistributor.sendToServer(new NavigationNetworking.Controls()))
                .bounds(leftPos + 67,topPos + imageHeight - 26,88,18).build());
        confirm = addRenderableWidget(Button.builder(text("land"),button -> {
            // El ID confirmado protege frente a clics de una selección anterior todavía en tránsito.
            if (focused != null) PacketDistributor.sendToServer(new NavigationNetworking.Launch(menu.containerId,ModPresentation.id(focused.id())));
        }).bounds(leftPos + imageWidth - 132,topPos + imageHeight - 26,124,18).build());
        if (depth == 0) {
            int index = 0;
            for (var entry : CelestialCatalog.SYSTEMS) {
                var node = new MapNode(cx() - 24 + index++ * 62,cy() - 24,48,"sun",Component.translatable(entry.translation()),
                        () -> { system = entry; depth = 1; rebuild(); },
                        () -> entry.enabled() && menu.tier() >= entry.minimumTier(),
                        () -> !entry.enabled() || menu.tier() < entry.minimumTier());
                node.setTooltip(Tooltip.create(Component.translatable(entry.enabled() ? "navigation.galaxycraft.system_tier" : "navigation.galaxycraft.reason.unavailable",entry.minimumTier())));
                addRenderableWidget(node);
            }
        } else if (depth == 1) {
            var planets = CelestialCatalog.planets(system);
            for (int i = 0; i < planets.size(); i++) {
                var body = planets.get(i); double angle = -Math.PI / 2 + i * Math.PI * 2 / planets.size();
                addBody(body,cx() + (int)(Math.cos(angle) * rx()),cy() + (int)(Math.sin(angle) * ry()),24,
                        () -> { planet = body; select(body); depth = 2; rebuild(); });
            }
        } else {
            addBody(planet,cx(),cy(),40,() -> select(planet));
            var moons = CelestialCatalog.moons(planet);
            for (int i = 0; i < moons.size(); i++) {
                var moon = moons.get(i); double angle = -Math.PI / 2 + i * Math.PI * 2 / moons.size();
                addBody(moon,cx() + (int)(Math.cos(angle) * rx()),cy() + (int)(Math.sin(angle) * ry()),24,() -> select(moon));
            }
        }
    }
    private void addBody(CelestialBody body,int x,int y,int size,Runnable action) {
        var node = new MapNode(x - size/2,y - size/2,size,body.id(),bodyName(body),action,
                () -> true,() -> menu.tier() < body.tier());
        node.setTooltip(Tooltip.create(Component.translatable("navigation.galaxycraft.body_tooltip",bodyName(body),Math.max(1,body.tier()),NavigationRules.fuelCost(body))));
        addRenderableWidget(node);
    }
    private String blockingReason() {
        if (depth != 2 || focused == null) return "choose";
        if (!SolarSystem.destination(menu.destination()).id().equals(focused.id())) return "sync";
        return menu.reason().name().toLowerCase(Locale.ROOT);
    }
    @Override public void render(GuiGraphics gui,int mouseX,int mouseY,float partialTick) {
        String reason = blockingReason(); confirm.active = reason.equals("ready");
        if (!lastReason.equals(reason)) { confirm.setTooltip(Tooltip.create(Component.translatable("navigation.galaxycraft.reason." + reason))); lastReason = reason; }
        super.render(gui,mouseX,mouseY,partialTick); renderTooltip(gui,mouseX,mouseY);
    }
    @Override protected void renderLabels(GuiGraphics gui,int mouseX,int mouseY) { /* El plano se dibuja en coordenadas de pantalla. */ }
    @Override protected void renderBg(GuiGraphics gui,float partialTick,int mouseX,int mouseY) {
        gui.fill(leftPos,topPos,leftPos + imageWidth,topPos + imageHeight,0xFA07131F);
        gui.renderOutline(leftPos,topPos,imageWidth,imageHeight,0xFF418DA6);
        gui.fill(leftPos + 1,topPos + 1,leftPos + imageWidth - 1,topPos + 25,0xFF122F43);
        gui.drawString(font,title,leftPos + 8,topPos + 8,0xA4F0FF,false);
        Component breadcrumb = depth == 0 ? text("galaxy") : Component.translatable(system.translation())
                .append(" › ").append(depth == 1 ? text("planets") : bodyName(planet));
        gui.drawString(font,font.plainSubstrByWidth(breadcrumb.getString(),imageWidth-122),leftPos + 112,topPos + 30,0x79C7D9,false);
        gui.fill(leftPos + 104,topPos + 28,leftPos + 105,topPos + imageHeight - 35,0xFF285065);
        // Campo de estrellas determinista: no genera partículas ni cambia el estado del mundo.
        for (int i = 0; i < 50; i++) {
            int x = leftPos + 111 + Math.floorMod(i * 73 + 19,imageWidth - 120), y = topPos + 42 + Math.floorMod(i * 47,imageHeight - 86);
            gui.fill(x,y,x+1,y+1,0xFF567888);
        }
        if (depth > 0) {
            orbit(gui,rx(),ry()); orbit(gui,rx() * .68,ry() * .68);
            if (depth == 1) icon(gui,"sun",cx() - 11,cy() - 11,22);
        }
        gui.drawString(font,Component.translatable("navigation.galaxycraft.rocket_tier",menu.tier()),leftPos + 8,topPos + 32,0xDFEFFF,false);
        if (focused != null) {
            icon(gui,focused.id(),leftPos + 36,topPos + 47,32);
            gui.drawString(font,font.plainSubstrByWidth(bodyName(focused).getString(),94),leftPos + 8,topPos + 84,0xA4F0FF,false);
            gui.drawString(font,Component.translatable("navigation.galaxycraft.required",Math.max(1,focused.tier())),leftPos + 8,topPos + 99,0xB9CDD5,false);
            gui.drawString(font,Component.translatable("navigation.galaxycraft.cost",NavigationRules.fuelCost(focused)),leftPos + 8,topPos + 112,0xB9CDD5,false);
            gui.drawString(font,Component.translatable("navigation.galaxycraft.gravity",String.format(Locale.ROOT,"%.3f",focused.gravity())),leftPos + 8,topPos + 125,0xB9CDD5,false);
        }
        gui.drawString(font,Component.translatable("navigation.galaxycraft.reserve",menu.fuel()),leftPos + 8,topPos + imageHeight - 63,0xFFC66B,false);
        String reason = blockingReason();
        var status = Component.translatable("navigation.galaxycraft.reason." + reason);
        gui.drawString(font,font.plainSubstrByWidth(status.getString(),imageWidth - 16),leftPos + 8,topPos + imageHeight - 43,
                reason.equals("ready") ? 0x72E5B0 : 0xE3AD83,false);
    }
    private void orbit(GuiGraphics gui,double rx,double ry) {
        for (int i = 0; i < 120; i++) {
            double angle = i * Math.PI * 2 / 120;
            int x = cx() + (int)(Math.cos(angle)*rx),y = cy() + (int)(Math.sin(angle)*ry);
            gui.fill(x,y,x+1,y+1,0xFF204C62);
        }
    }
    private static Component text(String key) { return Component.translatable("navigation.galaxycraft." + key); }
    private static Component bodyName(CelestialBody body) { return Component.translatable("celestial.galaxycraft." + body.id()); }
    private void icon(GuiGraphics gui,String body,int x,int y,int size) {
        gui.blit(ModPresentation.id("textures/gui/celestial/" + body + ".png"),x,y,0,0,size,size,size,size);
    }
    /** Botón accesible por teclado, con icono, selección visible y requisitos en tooltip. */
    private final class MapNode extends LiveNavigationButton {
        private final String body;
        private MapNode(int x,int y,int size,String body,Component label,Runnable action,
                java.util.function.BooleanSupplier enabled,java.util.function.BooleanSupplier locked) {
            super(x,y,size,label,button -> action.run(),enabled,locked); this.body = body;
        }
        @Override protected void renderWidget(GuiGraphics gui,int x,int y,float partialTick) {
            refreshState();
            boolean selected = focused != null && focused.id().equals(body);
            if (isHoveredOrFocused() || selected) gui.renderOutline(getX()-2,getY()-2,getWidth()+4,getHeight()+4,isLocked() ? 0xFFCF795D : 0xFF71DDF0);
            icon(gui,body,getX(),getY(),getWidth());
            if (isLocked()) gui.drawString(font,"×",getX()+getWidth()-5,getY(),0xFF9578,false);
        }
    }
}
