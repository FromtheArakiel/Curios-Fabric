package dev.arakiel.curiosfabric.api;

import java.util.HashMap;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;

import net.minecraft.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import javax.annotation.Nonnull;

/**
 * A wrapper class for representing slot types as attributes for use in attribute modifiers
 */
public class SlotAttribute extends Attribute {

  private static final DecimalFormat FORMAT =
      Util.make(new DecimalFormat("#.##"),
          format -> format.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT)));

  private static final Map<String, Holder<? extends Attribute>> SLOT_ATTRIBUTES = new HashMap<>();

  private final String identifier;

  @SuppressWarnings("unchecked")
  public static Holder<Attribute> getOrCreate(String id) {
    return (Holder<Attribute>) SLOT_ATTRIBUTES.computeIfAbsent(id,
        (k) -> new Holder.Direct<>(new SlotAttribute(id)));
  }

  protected SlotAttribute(String identifier) {
    super("curiosfabric.identifier." + identifier, 0);
    this.identifier = identifier;
  }

  public String getIdentifier() {
    return this.identifier;
  }

  /**
   * Builds the tooltip line of a slot modifier, for example {@code +1 Ring Slot}.
   *
   * <p>Neoforge turns this into an override of {@code Attribute#toComponent}; Fabric has no such
   * hook, so it is a plain method that the client tooltip code calls explicitly.</p>
   *
   * @param modifier The slot modifier applied to an item
   * @return The tooltip component
   */
  @Nonnull
  public MutableComponent toComponent(@Nonnull AttributeModifier modifier) {
    double value = modifier.amount();
    String key = value > 0 ? "curiosfabric.modifiers.slots.plus" : "curiosfabric.modifiers.slots.take";

    if (value > 1) {
      key = key + ".multiple";
    }
    ChatFormatting color = this.getStyle(value > 0);
    Component attrDesc = Component.translatable(this.getDescriptionId());
    Component valueComp = Component.literal(this.toValue(modifier.operation(), value));
    return Component.translatable(key, valueComp, attrDesc).withStyle(color);
  }

  private String toValue(AttributeModifier.Operation operation, double value) {
    double displayed = operation == AttributeModifier.Operation.ADD_VALUE ? value : value * 100;
    String formatted = FORMAT.format(displayed);

    if (operation != AttributeModifier.Operation.ADD_VALUE) {
      return formatted + "%";
    }
    return formatted;
  }
}
