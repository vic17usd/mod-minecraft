package com.vic17usd.galaxycraft.item;

import com.vic17usd.galaxycraft.block.LaunchPadBlock;
import com.vic17usd.galaxycraft.block.RocketBlock;
import net.minecraft.world.item.BlockItem;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Solo permite colocar el cohete sobre una base completa y con espacio superior. */
public final class RocketItem extends BlockItem {
    public RocketItem(Block block, Properties properties) { super(block, properties.stacksTo(1)); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.galaxycraft.rocket_pad"));
        tooltip.add(Component.translatable("tooltip.galaxycraft.rocket_fuel"));
        tooltip.add(Component.translatable("tooltip.galaxycraft.rocket_dismantle"));
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        var pos = context.getClickedPos();
        var volume = new net.minecraft.world.phys.AABB(pos.getX() - 1, pos.getY(), pos.getZ() - 1,
                pos.getX() + 2, pos.getY() + com.vic17usd.galaxycraft.block.RocketParts.HEIGHT, pos.getZ() + 2);
        if (!context.getLevel().getEntities((net.minecraft.world.entity.Entity) null, volume,
                entity -> entity instanceof net.minecraft.world.entity.LivingEntity && !entity.isSpectator()).isEmpty()) return false;
        return LaunchPadBlock.isComplete(context.getLevel(), context.getClickedPos().below())
                && RocketBlock.hasClearance(context.getLevel(), context.getClickedPos())
                && super.canPlace(context, state);
    }
}
