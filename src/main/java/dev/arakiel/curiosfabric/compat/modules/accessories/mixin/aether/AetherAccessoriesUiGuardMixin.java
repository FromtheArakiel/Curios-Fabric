package dev.arakiel.curiosfabric.compat.modules.accessories.mixin.aether;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps Aether on CuriosFabric's inventory UI by removing its accessory button and key entry.
 */
@Mixin(targets = "com.aetherteam.aether.client.event.hooks.GuiHooks")
public abstract class AetherAccessoriesUiGuardMixin {

  @Inject(method = "isAccessoryButtonEnabled", at = @At("RETURN"), cancellable = true)
  private static void curiosfabric$hideAccessoryButton(
      CallbackInfoReturnable<Boolean> callback) {
    callback.setReturnValue(false);
  }

  @Inject(method = "openAccessoryMenu", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$blockAccessoryScreen(CallbackInfo callback) {
    callback.cancel();
  }

  @Inject(method = "closeContainerMenu", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$blockAccessoryCloseKey(
      int key, int action, CallbackInfo callback) {
    callback.cancel();
  }
}
