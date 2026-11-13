package dev.arakiel.curiosfabric.compat.neoforgecurios;

import dev.arakiel.curiosfabric.api.SlotContext;
import dev.arakiel.curiosfabric.api.type.capability.ICurioItem;
import net.minecraft.world.item.ItemStack;

/**
 * {@link ICurioItem} used for items that were written against the Neoforge edition and only carry the
 * neoforge {@code #curios:<slot>} item tag.
 *
 * <p>Everything but the slot validity keeps the Curios defaults, which is what the neoforge edition
 * did for items that did not implement {@code ICurioItem} themselves.</p>
 */
public class NeoforgeCuriosItem implements ICurioItem {

  private final String slotId;

  public NeoforgeCuriosItem(String slotId) {
    this.slotId = slotId;
  }

  public String slotId() {
    return slotId;
  }

  @Override
  public boolean canEquip(SlotContext slotContext, ItemStack stack) {
    return slotId.equals(slotContext.identifier())
        && NeoforgeCuriosCompat.isNeoforgeCuriosItemFor(slotId, stack);
  }

  @Override
  public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
    return canEquip(slotContext, stack);
  }
}
