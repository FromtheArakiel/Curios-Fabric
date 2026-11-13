package dev.arakiel.curiosfabric.mixin.core;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.arakiel.curiosfabric.mixin.CuriosUtilMixinHooks;

@Mixin(PiglinAi.class)
public class MixinPiglinAi {

  @Inject(at = @At("RETURN"), method = "isWearingGold", cancellable = true)
  private static void curios$isWearingGold(LivingEntity livingEntity,
                                           CallbackInfoReturnable<Boolean> cir) {

    if (CuriosUtilMixinHooks.canNeutralizePiglins(livingEntity)) {
      cir.setReturnValue(true);
    }
  }
}
