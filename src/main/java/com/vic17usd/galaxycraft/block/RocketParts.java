package com.vic17usd.galaxycraft.block;

import com.vic17usd.galaxycraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Reserva física de 3 × 3 × 7. Las piezas no guardan inventarios ni generan objetos al romperse. */
public final class RocketParts {
    public static final int HEIGHT = 7;
    static final ThreadLocal<Boolean> CLEANING = ThreadLocal.withInitial(() -> false);
    public static boolean clear(Level level, BlockPos root) {
        if (!level.isInWorldBounds(root.above(HEIGHT - 1))) return false;
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = 0; y < HEIGHT; y++) {
            if (x == 0 && y == 0 && z == 0) continue;
            var pos = root.offset(x, y, z); var state = level.getBlockState(pos);
            if (!state.isAir() && !(state.is(ModBlocks.ROCKET_PART.get()) && RocketPartBlock.root(state, pos).equals(root))) return false;
        }
        return true;
    }
    public static void install(Level level, BlockPos root) {
        if (level.isClientSide() || !clear(level, root)) return;
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = 0; y < HEIGHT; y++) {
            if (x == 0 && y == 0 && z == 0) continue;
            var pos = root.offset(x, y, z);
            if (level.isEmptyBlock(pos)) level.setBlockAndUpdate(pos, ModBlocks.ROCKET_PART.get().defaultBlockState()
                    .setValue(RocketPartBlock.X, x + 1).setValue(RocketPartBlock.Y, y).setValue(RocketPartBlock.Z, z + 1));
        }
    }
    public static void remove(Level level, BlockPos root) {
        if (level.isClientSide()) return;
        CLEANING.set(true);
        try {
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = 0; y < HEIGHT; y++) {
                var pos = root.offset(x, y, z); var state = level.getBlockState(pos);
                if (state.is(ModBlocks.ROCKET_PART.get()) && RocketPartBlock.root(state, pos).equals(root)) level.removeBlock(pos, false);
            }
        } finally { CLEANING.set(false); }
    }
    private RocketParts() {}
}
