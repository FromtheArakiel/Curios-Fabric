package dev.arakiel.curiosfabric.compat.contribution.item;

import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Resolves foreign item tags or other upstream assignments to semantic slot ids. */
public interface ItemAssignmentContributor {

  Set<String> slotsFor(ItemStack stack, boolean client);

  default Set<String> slotsFor(
      ItemStack stack, @Nullable LivingEntity entity, boolean client) {
    return slotsFor(stack, client);
  }

  boolean matches(String slotId, ItemStack stack, boolean client);

  default boolean matches(
      String slotId, ItemStack stack, @Nullable LivingEntity entity, boolean client) {
    return matches(slotId, stack, client);
  }
}
