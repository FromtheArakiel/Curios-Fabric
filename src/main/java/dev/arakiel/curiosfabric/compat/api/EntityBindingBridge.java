package dev.arakiel.curiosfabric.compat.api;

import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.EntityType;

/** Supplies foreign entity-to-slot bindings without mutating Curios state. */
public interface EntityBindingBridge {

  Map<EntityType<?>, Set<String>> slotsByEntity(boolean client);
}
