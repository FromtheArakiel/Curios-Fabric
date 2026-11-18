package dev.arakiel.curiosfabric.compat.modules.neoforgecurios;

import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.CuriosTags;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.capability.ICurioItem;
import dev.arakiel.curiosfabric.compat.modules.neoforgecurios.item.NeoforgeCuriosItem;
import dev.arakiel.curiosfabric.compat.modules.neoforgecurios.item.NeoforgeCuriosItemAdapter;
import dev.arakiel.curiosfabric.compat.shared.detection.CompatTargets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Bridge for content mods written against the NeoForge Curios edition.
 *
 * <p>The bridge never links against the other implementation: it only reads the data that the
 * NeoForge edition publishes and feeds it into this implementation.</p>
 *
 * <ul>
 *   <li><b>slot definitions</b> - {@code data/<namespace>/curios/slots/<id>.json} is the same path in
 *       both editions, so Neoforge data packs keep working without conversion.</li>
 *   <li><b>entity slots</b> - same for {@code data/<namespace>/curios/entities/<id>.json}.</li>
 *   <li><b>item assignment</b> - the pre-1.21 Neoforge edition assigned items to a slot through the
 *       {@code #curios:<slot>} item tag; those tags are read here and the items are registered as
 *       Curios items for the matching slot.</li>
 *   <li><b>attribute modifiers</b> - both editions read the vanilla
 *       {@code minecraft:attribute_modifiers} component, so worn attributes carry over as is.</li>
 * </ul>
 *
 * <p>The data layer is complemented by the runtime bridge: {@code ICurioItem} implementations,
 * item capabilities registered under {@code CuriosCapability.ITEM}, item tags and slot data all
 * resolve to the same CuriosFabric inventory.</p>
 */
public final class NeoforgeCuriosCompat {

  /** Namespace of the Neoforge edition, also the data folder name ({@code data/<ns>/curios/...}). */
  public static final String NEOFORGE_MOD_ID = "curios";
  /** Tag namespace the neoforge edition used for item to slot assignment ({@code #curios:<slot>}). */
  public static final String NEOFORGE_TAG_NAMESPACE = "curios";

  private NeoforgeCuriosCompat() {
  }

  /** {@code true} when the NeoForge Curios edition is present. */
  public static boolean isLoaded() {
    return CompatTargets.isNeoforgeCuriosLoaded();
  }

  /**
   * Mirrors the neoforge {@code #curios:<slot>} item tags onto this implementation. Safe to call when
   * no neoforge content is present: unknown tags resolve to an empty set.
   *
   * @param slotIds the identifiers of the slots known to the server
   */
  public static void register(Set<String> slotIds) {
    if (!isLoaded()) {
      return;
    }

    Map<Item, Set<String>> assignments = new LinkedHashMap<>();
    for (String slotId : new LinkedHashSet<>(slotIds)) {
      ISlotType slot = CuriosApi.getSlot(slotId, false)
          .orElse(CuriosApi.getSlot(slotId, true).orElse(null));
      String mappingId = slot == null ? slotId : slot.getMappingIdentifier();
      TagKey<Item> neoforgeTag = neoforgeTag(mappingId);

      for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(neoforgeTag)) {
        assignments.computeIfAbsent(holder.value(), ignored -> new LinkedHashSet<>()).add(mappingId);
      }
    }
    assignments.forEach((item, slots) -> CuriosApi.registerCurio(item,
        NeoforgeCuriosItemAdapter.isNeoforgeCuriosItemItem(item)
            ? new NeoforgeCuriosItemAdapter(item)
            : new NeoforgeCuriosItem(slots)));
  }

  /** {@code true} when the stack is assigned to the slot through the neoforge item tag. */
  public static boolean isNeoforgeCuriosItemFor(String slotId, ItemStack stack) {
    return isLoaded() && (stack.is(neoforgeTag(slotId))
        || stack.is(CuriosTags.createItemTag(slotId)));
  }

  /**
   * All Curios slot identifiers this stack is assigned to through the neoforge {@code #curios:<slot>}
   * item tags.
   *
   * @param stack The stack to check
   * @return The slot identifiers, empty when no neoforge content is present
   */
  public static Set<String> slotsFor(ItemStack stack) {
    return slotsFor(stack, dev.arakiel.curiosfabric.CuriosFabricConstants.IS_CLIENT);
  }

  public static Set<String> slotsFor(ItemStack stack, boolean client) {

    if (!isLoaded() || stack.isEmpty()) {
      return Set.of();
    }
    Set<String> result = new LinkedHashSet<>();

    for (ISlotType slot : CuriosApi.getSlots(client).values()) {
      String mappingId = slot.getMappingIdentifier();
      if (stack.is(neoforgeTag(mappingId)) || stack.is(CuriosTags.createItemTag(mappingId))) {
        result.add(mappingId);
      }
    }
    return result;
  }

  /**
   * Resolves the curio behavior of a neoforge tagged item without requiring it to be pre-registered.
   *
   * @param stack The stack to check
   * @return The bridging {@link ICurioItem}
   */
  public static Optional<ICurioItem> lookup(ItemStack stack) {
    Set<String> slots = slotsFor(stack, false);
    if (slots.isEmpty()) {
      slots = slotsFor(stack, true);
    }
    return slots.isEmpty() ? Optional.empty() : Optional.of(new NeoforgeCuriosItem(slots));
  }

  /** The Curios slot type of a neoforge slot, or {@code null} when it is not registered. */
  public static ISlotType slotOf(String slotId) {
    return CuriosApi.getSlot(slotId, false).orElse(null);
  }

  private static TagKey<Item> neoforgeTag(String slotId) {
    return TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(NEOFORGE_TAG_NAMESPACE, slotId));
  }
}
