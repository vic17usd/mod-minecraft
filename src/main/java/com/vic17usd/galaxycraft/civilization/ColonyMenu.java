package com.vic17usd.galaxycraft.civilization;

import com.vic17usd.galaxycraft.biosphere.*;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Menú sin ranuras: no acepta pilas del cliente y liga cada acción a la identidad del NPC. */
public final class ColonyMenu extends AbstractContainerMenu {
  private final ColonyCitizen citizen;
  private final ContainerData data;

  public ColonyMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
    this(
        id,
        inventory,
        inventory.player.level().getEntity(buffer.readInt()) instanceof ColonyCitizen npc
            ? npc
            : null);
  }

  public ColonyMenu(int id, Inventory inventory, ColonyCitizen citizen) {
    super(ModBiosphere.COLONY_MENU.get(), id);
    this.citizen = citizen;
    data =
        citizen == null || !(inventory.player instanceof ServerPlayer server)
            ? new SimpleContainerData(6)
            : new ContainerData() {
              public int get(int index) {
                var contracts = server.getData(ModBiosphere.CONTRACTS.get());
                return switch (index) {
                  case 0 -> citizen.stock();
                  case 1 -> contracts.accepted.contains(citizen.body()) ? 1 : 0;
                  case 2 -> contracts.completed.contains(citizen.body()) ? 1 : 0;
                  case 3 ->
                      ColonyCitizen.count(server, ModBiosphere.HARVEST.get(citizen.body()).get());
                  case 4 ->
                      ColonyCitizen.count(
                          server,
                          BuiltInRegistries.ITEM.get(
                              ModPresentation.id(LivingProfiles.get(citizen.body()).resource())));
                  default -> ColonyCitizen.count(server, ModBiosphere.TOKEN.get());
                };
              }

              public void set(int index, int value) {}

              public int getCount() {
                return 6;
              }
            };
    addDataSlots(data);
  }

  public int value(int index) {
    return data.get(index);
  }

  public String body() {
    return citizen == null ? "earth" : citizen.body();
  }

  public boolean act(ServerPlayer player, int action) {
    return player.containerMenu == this && stillValid(player) && citizen.act(player, action);
  }

  @Override
  public boolean stillValid(Player player) {
    return citizen != null
        && player.isAlive()
        && !player.isSpectator()
        && citizen.isAlive()
        && !citizen.isRemoved()
        && citizen.level() == player.level()
        && player.level().getEntity(citizen.getId()) == citizen
        && player.distanceToSqr(citizen) <= 36;
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    return ItemStack.EMPTY;
  }
}
