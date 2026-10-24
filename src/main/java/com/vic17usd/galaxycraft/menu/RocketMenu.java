package com.vic17usd.galaxycraft.menu;

import com.vic17usd.galaxycraft.registry.ModBlocks;
import com.vic17usd.galaxycraft.registry.ModRocketRegistries;
import com.vic17usd.galaxycraft.block.RocketBlock;
import com.vic17usd.galaxycraft.blockentity.RocketBlockEntity;
import com.vic17usd.galaxycraft.rocket.RocketTravel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Sincroniza inventario, combustible y preparación sin paquetes personalizados. */
public final class RocketMenu extends AbstractContainerMenu {
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public RocketMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, new ItemStackHandler(2), new SimpleContainerData(4),
                ContainerLevelAccess.create(playerInventory.player.level(), buffer.readBlockPos()));
    }

    public RocketMenu(int id, Inventory playerInventory, ItemStackHandler inventory,
            ContainerData data, ContainerLevelAccess access) {
        super(ModRocketRegistries.ROCKET_MENU.get(), id);
        checkContainerDataCount(data, 4);
        this.data = data;
        this.access = access;
        addSlot(new SlotItemHandler(inventory, 0, 44, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(Items.LAVA_BUCKET); }
        });
        addSlot(new SlotItemHandler(inventory, 1, 116, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, 9 + row * 9 + col, 8 + col * 18, 114 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col, 8 + col * 18, 172));
        addDataSlots(data);
    }

    public int fuelAmount() { return data.get(0); }
    public int readiness() { return data.get(1); }
    public int tier() { return data.get(2); }
    public int destination() { return data.get(3); }

    @Override public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof RocketBlock
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64, true);
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!(player instanceof ServerPlayer serverPlayer) || !stillValid(player) || (button != 0 && button != 1)) return false;
        access.execute((level, pos) -> {
            if (!(level.getBlockEntity(pos) instanceof RocketBlockEntity rocket)) return;
            if (button == 0) rocket.nextDestination();
            else if (!RocketTravel.launch(serverPlayer, rocket)) player.displayClientMessage(
                    Component.translatable("message.galaxycraft.travel_failed"), true);
        });
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
        } else if (stack.is(Items.LAVA_BUCKET)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (index < 29) {
            if (!moveItemStackTo(stack, 29, 38, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 2, 29, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
