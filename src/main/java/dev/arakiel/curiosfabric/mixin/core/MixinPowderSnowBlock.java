package dev.arakiel.curiosfabric.mixin.core;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.arakiel.curiosfabric.mixin.CuriosUtilMixinHooks;

@Mixin(PowderSnowBlock.class)
public class MixinPowderSnowBlock {

  @Inject(at = @At("RETURN"), method = "canEntityWalkOnPowderSnow", cancellable = true)
  private static void curios$canEntityWalkOnPowderSnow(Entity entity,
                                                       CallbackInfoReturnable<Boolean> cir) {

    if (entity instanceof LivingEntity livingEntity &&
        CuriosUtilMixinHooks.canWalkOnPowderSnow(livingEntity)) {
      cir.setReturnValue(true);
    }
  }
}
