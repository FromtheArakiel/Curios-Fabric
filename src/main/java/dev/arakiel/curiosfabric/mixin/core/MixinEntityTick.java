package dev.arakiel.curiosfabric.mixin.core;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.arakiel.curiosfabric.common.event.CuriosFabricEventHandler;

/**
 * Runs the curio tick logic at the end of every entity tick.
 *
 * <p>This replaces Neoforge's {@code EntityTickEvent.Post}. Fabric has no per entity tick event, and
 * the curio logic has to run for every living entity on both sides, so a mixin is the closest
 * equivalent.</p>
 */
@Mixin(Entity.class)
public class MixinEntityTick {

  @Inject(method = "tick", at = @At("TAIL"))
  private void curios$tick(CallbackInfo ci) {

    if ((Object) this instanceof LivingEntity livingEntity) {
      CuriosFabricEventHandler.tick(livingEntity);
    }
  }
}
