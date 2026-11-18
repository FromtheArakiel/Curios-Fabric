package dev.arakiel.curiosfabric.compat.modules.accessories.mixin;

import io.wispforest.accessories.client.AccessoriesClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Disables the Accessories inventory key/button entry point. */
@Mixin(AccessoriesClient.class)
public abstract class AccessoriesClientGuiMixin {

  @Inject(method = "initLayer", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$skipInventoryButton(CallbackInfo callback) {
    callback.cancel();
  }

  @Inject(method = "attemptToOpenScreen(Z)Z", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$disableAccessoriesScreen(
      boolean targetEntity, CallbackInfoReturnable<Boolean> callback) {
    callback.setReturnValue(false);
  }
}
