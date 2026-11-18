package dev.arakiel.curiosfabric.compat.modules.neoforgecurios.item;

import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.SlotContext;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.capability.ICurioItem;
import dev.arakiel.curiosfabric.compat.modules.neoforgecurios.NeoforgeCuriosCompat;
import java.util.Set;
import net.minecraft.world.item.ItemStack;

/**
 * {@link ICurioItem} used for items that were written against the Neoforge edition and only carry the
 * neoforge {@code #curios:<slot>} item tag.
 *
 * <p>Everything but the slot validity keeps the Curios defaults, which is what the neoforge edition
 * did for items that did not implement {@code ICurioItem} themselves.</p>
 */
public class NeoforgeCuriosItem implements ICurioItem {

  private final Set<String> slotIds;

  public NeoforgeCuriosItem(String slotId) {
    this(Set.of(slotId));
  }

  public NeoforgeCuriosItem(Set<String> slotIds) {
    this.slotIds = Set.copyOf(slotIds);
  }

  public String slotId() {
    return slotIds.iterator().next();
  }

  @Override
  public boolean canEquip(SlotContext slotContext, ItemStack stack) {
    boolean client = slotContext.entity() != null && slotContext.entity().level() != null
        && slotContext.entity().level().isClientSide();
    ISlotType slot = CuriosApi.getSlot(slotContext.identifier(), client).orElse(null);
    String mappingId = slot == null ? slotContext.identifier() : slot.getMappingIdentifier();
    return slotIds.contains(mappingId)
        && NeoforgeCuriosCompat.isNeoforgeCuriosItemFor(mappingId, stack);
  }

  @Override
  public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
    return canEquip(slotContext, stack);
  }
}
