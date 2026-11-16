package dev.arakiel.curiosfabric.compat.api;

import java.util.Set;
import net.minecraft.world.item.ItemStack;

/** Resolves foreign item tags to Curios slot identifiers. */
public interface ItemTagBridge {

  Set<String> slotsFor(ItemStack stack, boolean client);

  boolean matches(String slotId, ItemStack stack, boolean client);

  /**
   * {@code true} when the stack is directly assigned to the given mapping by this upstream API's
   * own slot-specific item tag. Universal tags are intentionally ignored.
   */
  boolean originatesFrom(String mappingId, ItemStack stack, boolean client);
}
