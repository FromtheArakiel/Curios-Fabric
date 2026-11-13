package dev.arakiel.curiosfabric.compat.mixin.trinkets;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Creative inventory counterpart of {@link TrinketInventorySlotMixin}. */
@Mixin(targets = "dev.emi.trinkets.CreativeTrinketSlot")
public class TrinketCreativeSlotMixin {

  @Inject(method = "<init>", at = @At("TAIL"))
  private void curiosfabric$hideSlot(CallbackInfo ci) {
    TrinketSlotAccessor accessor = (TrinketSlotAccessor) (Slot) (Object) this;
    accessor.curiosfabric$setX(-10000);
    accessor.curiosfabric$setY(-10000);
  }
}
