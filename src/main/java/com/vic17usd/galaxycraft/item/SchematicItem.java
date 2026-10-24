package com.vic17usd.galaxycraft.item;

import com.vic17usd.galaxycraft.registry.ModPresentation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Documento recuperable: descubre una receta en el libro sin consumir el esquema. */
public final class SchematicItem extends Item {
    private final String recipe;
    public SchematicItem(String recipe) { super(new Properties().stacksTo(1)); this.recipe = recipe; }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.awardRecipesByKey(java.util.List.of(ModPresentation.id(recipe)));
            player.displayClientMessage(Component.translatable("message.galaxycraft.schematic"), true);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
