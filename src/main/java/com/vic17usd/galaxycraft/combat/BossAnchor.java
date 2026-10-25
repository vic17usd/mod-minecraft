package com.vic17usd.galaxycraft.combat;

import com.vic17usd.galaxycraft.frontier.ModFrontiers;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** UUID y estado persistentes: descargar la zona o reiniciar no recrea ni duplica el jefe. */
public final class BossAnchor extends BlockEntity {
  public boolean awakened, defeated;
  private UUID bossId;
  // Vacío conserva exactamente el comportamiento de las anclas históricas.
  private String encounterBody = "";

  public String encounterBody() {
    return encounterBody;
  }

  public BossAnchor(BlockPos pos, BlockState state) {
    super(ModFrontiers.ANCHOR.get(), pos, state);
  }

  public boolean wake() {
    if (!(level instanceof ServerLevel server)
        || awakened
        || server.getDifficulty() == Difficulty.PEACEFUL) return false;
    if (!encounterBody.isEmpty()
        && !com.vic17usd.galaxycraft.expedition.ExpeditionDimensions.matches(
            encounterBody, server.dimension())) return false;
    var boss =
        (((BossAnchorBlock) getBlockState().getBlock()).queen
                ? ModFrontiers.QUEEN
                : ModFrontiers.GUARDIAN)
            .get()
            .create(server);
    if (boss == null) return false;
    if (!encounterBody.isEmpty()) boss.expedition(encounterBody);
    boss.setPos(worldPosition.getX() + .5, worldPosition.getY() + 1, worldPosition.getZ() + .5);
    boss.anchor(worldPosition);
    if (!server.noCollision(boss) || !server.addFreshEntity(boss)) return false;
    bossId = boss.getUUID();
    awakened = true;
    setChanged();
    return true;
  }

  public void defeat(UUID id) {
    if (id.equals(bossId)) {
      defeated = true;
      setChanged();
    }
  }

  public static void tick(Level level, BlockPos pos, BlockState state, BossAnchor anchor) {
    if (level.getGameTime() % 20 == 0
        && !anchor.awakened
        && level.players().stream()
            .anyMatch(
                player ->
                    player.isAlive()
                        && !player.isSpectator()
                        && player.distanceToSqr(pos.getCenter()) < 144)) anchor.wake();
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.saveAdditional(tag, lookup);
    tag.putBoolean("Awakened", awakened);
    tag.putBoolean("Defeated", defeated);
    if (bossId != null) tag.putUUID("Boss", bossId);
    if (!encounterBody.isEmpty()) tag.putString("EncounterBody", encounterBody);
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.loadAdditional(tag, lookup);
    awakened = tag.getBoolean("Awakened");
    defeated = tag.getBoolean("Defeated");
    bossId = tag.hasUUID("Boss") ? tag.getUUID("Boss") : null;
    encounterBody = tag.getString("EncounterBody");
    if (!encounterBody.isEmpty())
      com.vic17usd.galaxycraft.biosphere.LivingProfiles.get(encounterBody);
  }
}
