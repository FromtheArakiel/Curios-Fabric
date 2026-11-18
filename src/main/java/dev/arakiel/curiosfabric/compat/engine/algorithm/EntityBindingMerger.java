package dev.arakiel.curiosfabric.compat.engine.algorithm;

import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.compat.contribution.entity.EntitySlotBinding;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.EntityType;

/** Converts bridge entity contributions against the final slot table. */
public final class EntityBindingMerger {

  private EntityBindingMerger() {
  }

  public static Map<EntityType<?>, Map<String, ISlotType>> merge(
      Map<String, ISlotType> slots, List<EntitySlotBinding> bindings) {
    Map<EntityType<?>, Map<String, ISlotType>> result = new LinkedHashMap<>();
    for (EntitySlotBinding binding : bindings) {
      Map<String, ISlotType> resolved = new LinkedHashMap<>();
      for (String slotId : new LinkedHashSet<>(binding.slotIds())) {
        ISlotType slot = slots.get(slotId);
        if (slot != null) {
          resolved.put(slotId, slot);
        }
      }
      if (!resolved.isEmpty()) {
        result.computeIfAbsent(binding.entityType(), ignored -> new LinkedHashMap<>())
            .putAll(resolved);
      }
    }
    return result;
  }
}
