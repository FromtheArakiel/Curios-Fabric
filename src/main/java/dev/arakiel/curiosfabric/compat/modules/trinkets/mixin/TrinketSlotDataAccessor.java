package dev.arakiel.curiosfabric.compat.modules.trinkets.mixin;

import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "dev.emi.trinkets.data.SlotLoader$SlotData", remap = false)
public interface TrinketSlotDataAccessor {

  @Accessor(value = "order", remap = false)
  int curiosfabric$getOrder();

  @Accessor(value = "amount", remap = false)
  int curiosfabric$getAmount();

  @Accessor(value = "icon", remap = false)
  String curiosfabric$getIcon();

  @Accessor(value = "dropRule", remap = false)
  String curiosfabric$getDropRule();

  @Accessor(value = "validatorPredicates", remap = false)
  Set<String> curiosfabric$getValidatorPredicates();
}
