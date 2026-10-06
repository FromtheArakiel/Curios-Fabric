/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 */

package dev.arakiel.curios.compat.forge;

import dev.arakiel.curios.api.SlotContext;
import dev.arakiel.curios.api.type.capability.ICurioItem;
import net.minecraft.world.item.ItemStack;

/**
 * {@link ICurioItem} used for items that were written against the Forge edition and only carry the
 * legacy {@code #curios:<slot>} item tag.
 *
 * <p>Everything but the slot validity keeps the Curios defaults, which is what the legacy edition
 * did for items that did not implement {@code ICurioItem} themselves.</p>
 */
public class LegacyCurio implements ICurioItem {

  private final String slotId;

  public LegacyCurio(String slotId) {
    this.slotId = slotId;
  }

  public String slotId() {
    return slotId;
  }

  @Override
  public boolean canEquip(SlotContext slotContext, ItemStack stack) {
    return slotId.equals(slotContext.identifier())
        && ForgeCuriosCompat.isLegacyCurioFor(slotId, stack);
  }

  @Override
  public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
    return canEquip(slotContext, stack);
  }
}
