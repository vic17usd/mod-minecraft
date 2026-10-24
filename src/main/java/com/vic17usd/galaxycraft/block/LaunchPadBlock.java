package com.vic17usd.galaxycraft.block;

import com.vic17usd.galaxycraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Base modular: nueve bloques completos sostienen el centro del cohete. */
public final class LaunchPadBlock extends Block {
    public LaunchPadBlock(Properties properties) { super(properties); }

    public static boolean isComplete(Level level, BlockPos center) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = center.offset(x, 0, z);
                // No fuerza la carga de chunks vecinos durante una comprobación.
                if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                        || !level.getBlockState(pos).is(ModBlocks.LAUNCH_PAD.get())) return false;
            }
        }
        return true;
    }
}
