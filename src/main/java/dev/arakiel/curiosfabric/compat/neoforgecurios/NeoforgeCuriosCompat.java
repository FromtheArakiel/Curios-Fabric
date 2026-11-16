package dev.arakiel.curiosfabric.compat.neoforgecurios;

import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.CuriosTags;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.capability.ICurioItem;
import java.util.LinkedHashSet;
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
 * <p>Known limitation: an {@code ICurioItem} implementation from a Neoforge mod is a different class
 * than {@code dev.arakiel.curiosfabric.api.type.capability.ICurioItem}, so its <em>code</em> hooks
 * (curioTick, onEquip, ...) cannot run. Only the data driven half of the contract is bridged.</p>
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
    return dev.arakiel.curiosfabric.compat.CompatTargets.isNeoforgeCuriosLoaded();
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

    for (String slotId : new LinkedHashSet<>(slotIds)) {
      ISlotType slot = CuriosApi.getSlot(slotId, false).orElse(null);
      String mappingId = slot == null ? slotId : slot.getMappingIdentifier();
      TagKey<Item> neoforgeTag = neoforgeTag(mappingId);

      for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(neoforgeTag)) {
        // A Neoforge curio that implements the Neoforge edition's ICurioItem has code hooks; the plain
        // tag bridge would drop them, and the registry is consulted before the adapters.
        CuriosApi.registerCurio(holder.value(),
            dev.arakiel.curiosfabric.compat.bridge.NeoforgeCuriosItemAdapter
                .isNeoforgeCuriosItemItem(holder.value())
                ? new dev.arakiel.curiosfabric.compat.bridge.NeoforgeCuriosItemAdapter(holder.value())
                : new NeoforgeCuriosItem(mappingId));
      }
    }
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

  /** {@code true} when the stack uses the upstream NeoForge tag for this exact mapping. */
  public static boolean hasNeoforgeItemTag(String mappingId, ItemStack stack, boolean client) {
    return isLoaded() && !stack.isEmpty() && mappingId != null
        && stack.is(neoforgeTag(mappingId));
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
    return slots.isEmpty() ? Optional.empty() : Optional.of(new NeoforgeCuriosItem(slots.iterator().next()));
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
