package dev.arakiel.curiosfabric.compat.contribution.item;

import java.util.Set;
import net.minecraft.world.item.ItemStack;

/** Resolves foreign item tags or other upstream assignments to semantic slot ids. */
public interface ItemAssignmentContributor {

  Set<String> slotsFor(ItemStack stack, boolean client);

  boolean matches(String slotId, ItemStack stack, boolean client);
}
