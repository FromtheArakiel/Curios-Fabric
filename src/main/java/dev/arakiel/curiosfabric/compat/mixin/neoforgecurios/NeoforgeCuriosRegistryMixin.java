package dev.arakiel.curiosfabric.compat.mixin.neoforgecurios;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips the NeoForge edition's registry bootstrap: entity attachment, capabilities, the Curios menu,
 * the loot function, the criterion trigger and its data component. Without it the foreign edition
 * registers no ids at all and never owns any accessory data.
 */
@Mixin(targets = "top.theillusivec4.curios.common.CuriosRegistry")
public class NeoforgeCuriosRegistryMixin {

  @Inject(method = "init", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$noRegistryInit(CallbackInfo ci) {
    ci.cancel();
  }
}
