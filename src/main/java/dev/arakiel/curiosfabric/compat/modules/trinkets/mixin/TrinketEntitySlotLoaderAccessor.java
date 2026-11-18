package dev.arakiel.curiosfabric.compat.modules.trinkets.mixin;

import java.util.Map;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "dev.emi.trinkets.data.EntitySlotLoader", remap = false)
public interface TrinketEntitySlotLoaderAccessor {

  @Invoker("getEntitySlots")
  Map<?, ?> curiosfabric$getEntitySlots(EntityType<?> entityType);
}
