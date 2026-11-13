package dev.arakiel.curiosfabric.compat.mixin.neoforge;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.arakiel.curiosfabric.compat.bridge.NeoforgeEffectHook;

/**
 * Restores the NeoForge "may this effect be applied" hook for cross loader installations.
 *
 * <p>NeoForge replaces the vanilla {@code canBeAffected} call inside {@code LivingEntity#addEffect}
 * with {@code CommonHooks.canMobEffectBeApplied}, which posts {@code MobEffectEvent.Applicable} and
 * falls back to the vanilla result. A cross loader layer that instead chains that hook onto the
 * vanilla result with {@code ||} never posts the event when vanilla allows the effect - which is
 * exactly the case accessory mods need - so every "immune to X" curio silently stops working.</p>
 *
 * <p>Asking the hook first and vetoing the application here reproduces the NeoForge behaviour: the
 * event still wins when it forces {@code Result.APPLY}, while {@code Result.DEFAULT} keeps the
 * vanilla outcome.</p>
 */
@Mixin(LivingEntity.class)
public class NeoforgeEffectApplicableMixin {

  @Inject(
      method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;"
          + "Lnet/minecraft/world/entity/Entity;)Z",
      at = @At("HEAD"), cancellable = true)
  private void curiosfabric$neoforgeApplicable(MobEffectInstance effectInstance, Entity source,
                                               CallbackInfoReturnable<Boolean> cir) {
    Boolean allowed = NeoforgeEffectHook.canMobEffectBeApplied((LivingEntity) (Object) this,
        effectInstance, source);

    if (allowed != null && !allowed) {
      cir.setReturnValue(false);
    }
  }
}
