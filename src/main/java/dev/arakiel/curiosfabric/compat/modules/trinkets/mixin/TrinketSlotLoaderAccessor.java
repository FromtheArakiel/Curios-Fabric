package dev.arakiel.curiosfabric.compat.modules.trinkets.mixin;

import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "dev.emi.trinkets.data.SlotLoader", remap = false)
public interface TrinketSlotLoaderAccessor {

  @Accessor(value = "slots", remap = false)
  Map<String, Object> curiosfabric$getLoadedSlots();
}
