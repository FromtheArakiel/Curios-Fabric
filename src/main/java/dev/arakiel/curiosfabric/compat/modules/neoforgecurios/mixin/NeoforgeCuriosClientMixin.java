package dev.arakiel.curiosfabric.compat.modules.neoforgecurios.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Disables the NeoForge edition's client side: key binding, menu screen and render layers. */
@Mixin(targets = "top.theillusivec4.curios.Curios$ClientProxy")
public class NeoforgeCuriosClientMixin {

  @Inject(method = "registerKeys", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$noKeys(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "setupClient", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$noClientSetup(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "registerMenuScreens", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$noScreens(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "addLayers", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$noLayers(CallbackInfo ci) {
    ci.cancel();
  }
}
