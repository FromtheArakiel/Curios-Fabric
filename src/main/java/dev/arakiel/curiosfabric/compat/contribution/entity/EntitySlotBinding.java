package dev.arakiel.curiosfabric.compat.contribution.entity;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.world.entity.EntityType;

/** One entity-to-slot assignment contributed by a bridge. */
public record EntitySlotBinding(EntityType<?> entityType, Set<String> slotIds) {

  public EntitySlotBinding {
    if (entityType == null || slotIds == null) {
      throw new IllegalArgumentException("entity binding fields must not be null");
    }
    slotIds = Set.copyOf(new LinkedHashSet<>(slotIds));
  }
}
