/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 */

package dev.arakiel.curios.compat.trinkets;

import dev.arakiel.curios.api.CuriosApi;
import dev.arakiel.curios.api.type.capability.ICurioItem;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
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
 * Trinkets to Curios mapping layer.
 *
 * <p>The layer is deliberately data driven: it never links against Trinkets' classes, it only reads
 * the item tags Trinkets publishes ({@code #trinkets:<group>}, e.g. {@code #trinkets:chest/necklace}).
 * That keeps this class loadable when Trinkets is missing (it simply does nothing) and it keeps
 * working across Trinkets' minor updates, the same idea the {@code accessories-compat-layer} uses for
 * its Trinkets to Accessories mapping, just pointed at Curios instead.</p>
 *
 * <p>Mapping rules:</p>
 * <ul>
 *   <li>slot groups are translated by {@link TrinketSlots};</li>
 *   <li>every item of a mapped tag is registered as a Curios item through
 *       {@link CuriosApi#registerCurio(Item, dev.arakiel.curios.api.type.capability.ICurioItem)};</li>
 *   <li>attribute modifiers are not copied, because both mods read the vanilla
 *       {@code minecraft:attribute_modifiers} component of the item stack.</li>
 * </ul>
 */
public final class TrinketsCompat {

  public static final String MOD_ID = "trinkets";

  /** Cache of the tags per Trinkets group, so the tag lookup is done once per group. */
  private static final Map<String, TagKey<Item>> TAG_CACHE = new HashMap<>();

  private TrinketsCompat() {
  }

  /** {@code true} when Trinkets is installed. */
  public static boolean isLoaded() {
    return dev.arakiel.curios.compat.CompatTargets.isTrinketsLoaded();
  }

  /**
   * Mirrors Trinkets' item tags onto Curios. Called when the server starts, i.e. after the item
   * registries are frozen; it is a no-op when Trinkets is not installed.
   */
  public static void register() {
    if (!isLoaded()) {
      return;
    }
    for (String group : TrinketSlots.groups().keySet()) {
      for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tagFor(group))) {
        // Items that implement Trinkets' own Trinket interface carry code hooks (Potion Ring and
        // friends), so they must be registered with the hook adapter. Registering the plain data
        // bridge for them would silently drop tick/equip behavior, because the registry is the
        // first thing the lookups consult.
        CuriosApi.registerCurio(holder.value(),
            dev.arakiel.curios.compat.bridge.TrinketItemAdapter
                .isTrinket(holder.value())
                ? new dev.arakiel.curios.compat.bridge.TrinketItemAdapter(holder.value())
                : TrinketCurio.INSTANCE);
      }
    }
  }

  /** {@code true} when the stack belongs to the given Trinkets slot group tag. */
  public static boolean isTrinketFor(ItemStack stack, String trinketsGroup) {
    return isLoaded() && stack.is(tagFor(trinketsGroup));
  }

  /**
   * All Curios slot identifiers this stack is mapped to through Trinkets' item tags.
   *
   * <p>This is what makes the bridge work without touching any data pack: the items are simply not
   * listed in the {@code #curios:<slot>} tags, so their slot assignment has to be inferred from the
   * Trinkets tags at runtime.</p>
   *
   * @param stack The stack to check
   * @return The Curios slot identifiers, empty when Trinkets is missing
   */
  public static Set<String> slotsFor(ItemStack stack) {

    if (!isLoaded() || stack.isEmpty()) {
      return Set.of();
    }
    Set<String> result = new HashSet<>();

    for (Map.Entry<String, String> entry : TrinketSlots.groups().entrySet()) {

      if (stack.is(tagFor(entry.getKey()))) {
        result.add(entry.getValue());
      }
    }
    return result;
  }

  /**
   * Resolves the curio behavior of a Trinkets item without requiring it to be pre-registered.
   *
   * @param stack The stack to check
   * @return The bridging {@link ICurioItem}
   */
  public static Optional<ICurioItem> lookup(ItemStack stack) {
    return slotsFor(stack).isEmpty() ? Optional.empty() : Optional.of(TrinketCurio.INSTANCE);
  }

  private static TagKey<Item> tagFor(String trinketsGroup) {
    return TAG_CACHE.computeIfAbsent(trinketsGroup, group -> TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(MOD_ID, group)));
  }
}
