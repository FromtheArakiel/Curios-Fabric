/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package dev.arakiel.curios.client;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import dev.arakiel.curios.api.SlotAttribute;

/**
 * Formats the curio attribute tooltip lines.
 *
 * <p>NeoForge's {@code AttributeUtil} did this in the NeoForge edition. Since that helper (and the
 * {@code neoforge.*} translation keys it used) is not available on Fabric, the lines are built the
 * same way vanilla renders item attribute modifiers, which is also fully localized.</p>
 */
public final class CuriosTooltips {

  private CuriosTooltips() {
  }

  /**
   * Builds a single tooltip line for an attribute modifier of a curio.
   *
   * @param attribute The attribute
   * @param modifier  The modifier
   * @return The tooltip line
   */
  public static Component attributeModifier(Holder<Attribute> attribute,
                                            AttributeModifier modifier) {

    if (attribute.value() instanceof SlotAttribute slotAttribute) {
      return slotAttribute.toComponent(modifier);
    }
    double value = modifier.amount();
    AttributeModifier.Operation operation = modifier.operation();
    double displayed = switch (operation) {
      case ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL -> value * 100.0D;
      case ADD_VALUE ->
          attribute.is(Attributes.KNOCKBACK_RESISTANCE) ? value * 10.0D : value;
    };
    String key = "attribute.modifier." + (value > 0 ? "plus." : "take.") + operation.id();
    Component amount = Component.literal(
        ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value > 0 ? displayed
            : -displayed));
    return Component.translatable(key, amount,
        Component.translatable(attribute.value().getDescriptionId()))
        .withStyle(attribute.value().getStyle(value > 0));
  }
}
