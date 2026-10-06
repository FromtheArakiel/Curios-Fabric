/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 */

package dev.arakiel.curios.compat.forge;

import dev.arakiel.curios.api.CuriosApi;
import dev.arakiel.curios.api.type.ISlotType;
import dev.arakiel.curios.api.type.capability.ICurioItem;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Bridge for content mods that were written against the Forge/NeoForge edition of Curios and are
 * brought over by a cross loader compatibility layer (for example Sinytra Connector).
 *
 * <p>Like {@code accessories-compat-layer} does for Trinkets, the bridge never links against the
 * other implementation: it only reads the data that the Forge edition publishes and feeds it into
 * this implementation.</p>
 *
 * <ul>
 *   <li><b>slot definitions</b> - {@code data/<namespace>/curios/slots/<id>.json} is the same path in
 *       both editions, so Forge data packs keep working without conversion.</li>
 *   <li><b>entity slots</b> - same for {@code data/<namespace>/curios/entities/<id>.json}.</li>
 *   <li><b>item assignment</b> - the pre-1.21 Forge edition assigned items to a slot through the
 *       {@code #curios:<slot>} item tag; those tags are read here and the items are registered as
 *       Curios items for the matching slot.</li>
 *   <li><b>attribute modifiers</b> - both editions read the vanilla
 *       {@code minecraft:attribute_modifiers} component, so worn attributes carry over as is.</li>
 * </ul>
 *
 * <p>Known limitation: an {@code ICurioItem} implementation from a Forge mod is a different class
 * than {@code dev.arakiel.curios.api.type.capability.ICurioItem}, so its <em>code</em> hooks
 * (curioTick, onEquip, ...) cannot run. Only the data driven half of the contract is bridged.</p>
 */
public final class ForgeCuriosCompat {

  /** Namespace of the Forge edition, also the data folder name ({@code data/<ns>/curios/...}). */
  public static final String LEGACY_MOD_ID = "curios";
  /** Tag namespace the legacy edition used for item to slot assignment ({@code #curios:<slot>}). */
  public static final String LEGACY_TAG_NAMESPACE = "curios";

  private ForgeCuriosCompat() {
  }

  /** {@code true} when a cross loader layer or the legacy edition is present. */
  public static boolean isLoaded() {
    FabricLoader loader = FabricLoader.getInstance();
    return dev.arakiel.curios.compat.CompatTargets.isForgeCuriosLoaded()
        || loader.isModLoaded("connectormod")
        || loader.isModLoaded("connector")
        || loader.isModLoaded("sinytra_connector");
  }

  /**
   * Mirrors the legacy {@code #curios:<slot>} item tags onto this implementation. Safe to call when
   * no legacy content is present: unknown tags resolve to an empty set.
   *
   * @param slotIds the identifiers of the slots known to the server
   */
  public static void register(Set<String> slotIds) {
    if (!isLoaded()) {
      return;
    }

    for (String slotId : new LinkedHashSet<>(slotIds)) {
      TagKey<Item> legacyTag = legacyTag(slotId);

      for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(legacyTag)) {
        // A Forge curio that implements the Forge edition's ICurioItem has code hooks; the plain
        // tag bridge would drop them, and the registry is consulted before the adapters.
        CuriosApi.registerCurio(holder.value(),
            dev.arakiel.curios.compat.bridge.ForgeCurioItemAdapter
                .isForgeCurioItem(holder.value())
                ? new dev.arakiel.curios.compat.bridge.ForgeCurioItemAdapter(holder.value())
                : new LegacyCurio(slotId));
      }
    }
  }

  /** {@code true} when the stack is assigned to the slot through the legacy item tag. */
  public static boolean isLegacyCurioFor(String slotId, ItemStack stack) {
    return isLoaded() && stack.is(legacyTag(slotId));
  }

  /**
   * All Curios slot identifiers this stack is assigned to through the legacy {@code #curios:<slot>}
   * item tags.
   *
   * @param stack The stack to check
   * @return The slot identifiers, empty when no legacy content is present
   */
  public static Set<String> slotsFor(ItemStack stack) {

    if (!isLoaded() || stack.isEmpty()) {
      return Set.of();
    }
    Set<String> result = new LinkedHashSet<>();

    for (String slotId : CuriosApi.getSlots(dev.arakiel.curios.CuriosConstants.IS_CLIENT)
        .keySet()) {

      if (stack.is(legacyTag(slotId))) {
        result.add(slotId);
      }
    }
    return result;
  }

  /**
   * Resolves the curio behavior of a legacy tagged item without requiring it to be pre-registered.
   *
   * @param stack The stack to check
   * @return The bridging {@link ICurioItem}
   */
  public static Optional<ICurioItem> lookup(ItemStack stack) {
    Set<String> slots = slotsFor(stack);
    return slots.isEmpty() ? Optional.empty() : Optional.of(new LegacyCurio(slots.iterator().next()));
  }

  /** The Curios slot type of a legacy slot, or {@code null} when it is not registered. */
  public static ISlotType slotOf(String slotId) {
    return CuriosApi.getSlot(slotId, false).orElse(null);
  }

  private static TagKey<Item> legacyTag(String slotId) {
    return TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(LEGACY_TAG_NAMESPACE, slotId));
  }
}
