package dev.arakiel.curiosfabric.mixin.core;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.arakiel.curiosfabric.common.event.CuriosFabricEventHandler;

/**
 * Adds the experience bonus of worn fortune curios to a broken block.
 *
 * <p>This replaces Neoforge's {@code BlockDropsEvent}, which allowed changing the experience a block
 * drops. Fabric has no such event, so the bonus is spawned as an extra experience orb.</p>
 */
@Mixin(Block.class)
public class MixinBlockExperience {

  @Inject(method = "playerDestroy", at = @At("TAIL"))
  private void curios$fortuneExperience(Level level, Player player, BlockPos pos,
                                        BlockState state, @Nullable BlockEntity blockEntity,
                                        ItemStack tool, CallbackInfo ci) {

    if (!(level instanceof ServerLevel serverLevel)) {
      return;
    }
    int experience = CuriosFabricEventHandler.getFortuneExperience(player, level, tool);

    if (experience > 0) {
      serverLevel.addFreshEntity(new ExperienceOrb(serverLevel, pos.getX() + 0.5D,
          pos.getY() + 0.5D, pos.getZ() + 0.5D, experience));
    }
  }
}
