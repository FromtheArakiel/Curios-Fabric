package dev.arakiel.curiosfabric.compat.modules.trinkets.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the compatibility layer move a foreign slot out of the visible screen area. */
@Mixin(Slot.class)
public interface TrinketSlotAccessor {

  @Mutable
  @Accessor("x")
  void curiosfabric$setX(int x);

  @Mutable
  @Accessor("y")
  void curiosfabric$setY(int y);
}
