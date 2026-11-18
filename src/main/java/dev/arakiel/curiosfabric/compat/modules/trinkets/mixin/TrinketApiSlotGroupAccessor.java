package dev.arakiel.curiosfabric.compat.modules.trinkets.mixin;

import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "dev.emi.trinkets.api.SlotGroup", remap = false)
public interface TrinketApiSlotGroupAccessor {

  @Invoker("getSlots")
  Map<?, ?> curiosfabric$getSlots();
}
