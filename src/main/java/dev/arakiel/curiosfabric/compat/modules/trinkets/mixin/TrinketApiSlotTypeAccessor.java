package dev.arakiel.curiosfabric.compat.modules.trinkets.mixin;

import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "dev.emi.trinkets.api.SlotType", remap = false)
public interface TrinketApiSlotTypeAccessor {

  @Invoker("getOrder")
  int curiosfabric$getOrder();

  @Invoker("getAmount")
  int curiosfabric$getAmount();

  @Invoker("getIcon")
  ResourceLocation curiosfabric$getIcon();

  @Invoker("getValidatorPredicates")
  Set<ResourceLocation> curiosfabric$getValidatorPredicates();

  @Invoker("getQuickMovePredicates")
  Set<ResourceLocation> curiosfabric$getQuickMovePredicates();

  @Invoker("getTooltipPredicates")
  Set<ResourceLocation> curiosfabric$getTooltipPredicates();

  @Invoker("write")
  void curiosfabric$write(CompoundTag tag);
}
