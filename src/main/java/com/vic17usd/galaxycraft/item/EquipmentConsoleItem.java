package com.vic17usd.galaxycraft.item;

import com.vic17usd.galaxycraft.menu.SpaceEquipmentMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Terminal portátil de equipo; abre el mismo inventario persistente aunque se pierda la terminal. */
public final class EquipmentConsoleItem extends Item {
    public EquipmentConsoleItem() { super(new Properties().stacksTo(1)); }
    public static void open(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) return;
        player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new SpaceEquipmentMenu(id, inventory),
                Component.translatable("container.galaxycraft.equipment")));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) open(serverPlayer);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
