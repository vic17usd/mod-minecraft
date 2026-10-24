package com.vic17usd.galaxycraft.item;

import java.util.List;
import com.vic17usd.galaxycraft.component.OxygenData;
import com.vic17usd.galaxycraft.registry.ModDataComponents;
import com.vic17usd.galaxycraft.survival.SpaceSurvival;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Tanque individual: el nivel pertenece al ItemStack, nunca al singleton Item. */
public final class OxygenTankItem extends Item {
    public OxygenTankItem(Properties properties) {
        super(properties.stacksTo(1).component(ModDataComponents.OXYGEN_LEVEL.get(), OxygenData.FULL_TANK));
    }

    public static OxygenData oxygen(ItemStack stack) {
        // Un componente eliminado por comandos no debe convertirse en oxígeno gratis.
        return stack.getOrDefault(ModDataComponents.OXYGEN_LEVEL.get(), new OxygenData(0, 1200));
    }

    public static boolean consumeOxygen(ItemStack stack) {
        if (!(stack.getItem() instanceof OxygenTankItem)) return false;
        OxygenData data = oxygen(stack);
        if (data.current() == 0) return false;
        // Sustituir el valor inmutable permite detectar y sincronizar el cambio.
        stack.set(ModDataComponents.OXYGEN_LEVEL.get(), data.consume(1));
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        // Solo el servidor consulta su configuración y modifica el componente.
        if (!level.isClientSide()) {
            if (SpaceSurvival.isAirless(level)) {
                player.displayClientMessage(Component.translatable("message.galaxycraft.cannot_refill"), true);
            } else {
                stack.set(ModDataComponents.OXYGEN_LEVEL.get(), oxygen(stack).refill());
                player.displayClientMessage(Component.translatable("message.galaxycraft.refilled"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        OxygenData data = oxygen(stack);
        tooltip.add(Component.translatable("tooltip.galaxycraft.oxygen", data.current(), data.maximum()));
        tooltip.add(Component.translatable("tooltip.galaxycraft.refill"));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) { return oxygen(stack).current() < oxygen(stack).maximum(); }

    @Override
    public int getBarWidth(ItemStack stack) {
        OxygenData data = oxygen(stack);
        return Math.round(13.0F * data.current() / data.maximum());
    }

    @Override
    public int getBarColor(ItemStack stack) { return 0x40CFFF; }
}
