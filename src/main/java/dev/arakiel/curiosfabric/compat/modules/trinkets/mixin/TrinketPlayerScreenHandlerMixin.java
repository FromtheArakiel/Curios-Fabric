package dev.arakiel.curiosfabric.compat.modules.trinkets.mixin;

import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps Trinkets from adding its dynamic slots to the vanilla player menu.
 *
 * <p>CuriosFabric owns accessory storage and rendering through its own menu. Trinkets otherwise
 * adds slots to the player menu from its component view, which can make the server menu larger than
 * the client menu when slot data has not arrived yet and breaks {@code container_set_content}.
 * A priority above Trinkets' default ensures its generated method exists before this injection is
 * attached.</p>
 */
@Mixin(value = InventoryMenu.class, priority = 2000)
public abstract class TrinketPlayerScreenHandlerMixin {

  @Inject(method = "trinkets$updateTrinketSlots(Z)V", at = @At("HEAD"), cancellable = true,
      remap = false)
  private void curiosfabric$keepVanillaPlayerMenu(boolean slotsChanged, CallbackInfo ci) {
    ci.cancel();
  }
}
