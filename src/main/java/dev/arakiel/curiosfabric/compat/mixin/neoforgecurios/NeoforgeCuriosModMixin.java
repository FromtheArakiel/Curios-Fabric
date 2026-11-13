package dev.arakiel.curiosfabric.compat.mixin.neoforgecurios;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.common.data.CuriosSlotManager;
import dev.arakiel.curiosfabric.common.slottype.NeoforgeCuriosSlotManager;

/**
 * Neutralizes the NeoForge edition of Curios.
 *
 * <p>Its mod constructor cannot be cancelled (Mixin rejects cancellable constructor injections, and
 * that mistake used to crash Kilt setups), so every registration step it performs is cancelled
 * individually instead: registry bootstrap, capabilities, network, setup, server hooks and
 * commands. The client side is handled by {@link NeoforgeCuriosClientMixin}. What remains is its API
 * stub layer, which is exactly what we want: NeoForge mods keep resolving CuriosApi and
 * {@link NeoforgeCuriosApiHooksMixin} makes those calls answer with this mod's data.</p>
 */
@Mixin(targets = "top.theillusivec4.curios.Curios")
public class NeoforgeCuriosModMixin {

  @Inject(method = "registerCaps", at = @At("HEAD"), cancellable = true)
  private void curiosfabric$noCaps(CallbackInfo ci) {
    CuriosFabricConstants.LOG.info("Curios (Fabric) took over the accessory system: disabled the "
        + "NeoForge Curios capability registration");
    ci.cancel();
  }

  @Inject(method = "registerPayloadHandler", at = @At("HEAD"), cancellable = true)
  private void curiosfabric$noNetwork(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "setup", at = @At("HEAD"), cancellable = true)
  private void curiosfabric$noSetup(CallbackInfo ci) {
    CuriosFabricConstants.LOG.info("Curios (Fabric) took over the accessory system: disabled the "
        + "NeoForge Curios setup");
    ci.cancel();
  }

  @Inject(method = "process", at = @At("TAIL"))
  private void curiosfabric$captureImcSlots(CallbackInfo ci) {
    NeoforgeCuriosSlotManager.buildImcSlotTypes();
    CuriosSlotManager.SERVER.mergeSlots(NeoforgeCuriosSlotManager.getImcSlotTypes());
    CuriosSlotManager.SERVER.mergeSlotSources(NeoforgeCuriosSlotManager.getIdsToMods());
  }

  @Inject(method = "serverAboutToStart", at = @At("HEAD"), cancellable = true)
  private void curiosfabric$noServerStart(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "serverStopped", at = @At("HEAD"), cancellable = true)
  private void curiosfabric$noServerStop(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "registerCommands", at = @At("HEAD"), cancellable = true)
  private void curiosfabric$noCommands(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "reload", at = @At("HEAD"), cancellable = true)
  private void curiosfabric$noReload(CallbackInfo ci) {
    ci.cancel();
  }
}
