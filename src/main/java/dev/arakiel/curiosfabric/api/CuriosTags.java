package dev.arakiel.curiosfabric.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Collection of tags used by Curios. */
public final class CuriosTags {

  /** Items worn on the back, such as capes or backpacks. */
  public static final TagKey<Item> BACK = createItemTag("back");

  /** Items worn around the waist, such as belts or pouches. */
  public static final TagKey<Item> BELT = createItemTag("belt");

  /** Items worn on the torso/chest, such as cloaks or shirts. */
  public static final TagKey<Item> BODY = createItemTag("body");

  /** Items worn around the wrist, such as bands or bracelets. */
  public static final TagKey<Item> BRACELET = createItemTag("bracelet");

  /**
   * Miscellaneous items that are not strongly associated with a specific body part or usage type.
   */
  public static final TagKey<Item> CHARM = createItemTag("charm");

  /** Universal items that are able to equip or be equipped into any slot type. */
  public static final TagKey<Item> CURIO = createItemTag("curio");

  /** Items worn on the hands, such as gloves or gauntlets. */
  public static final TagKey<Item> HANDS = createItemTag("hands");

  /** Items worn on top of the head, such as crowns or hats. */
  public static final TagKey<Item> HEAD = createItemTag("head");

  /** Items worn around the neck, such as amulets or necklaces. */
  public static final TagKey<Item> NECKLACE = createItemTag("necklace");

  /** Items worn on the fingers, such as rings. */
  public static final TagKey<Item> RING = createItemTag("ring");

  /**
   * Creates an item tag key using the Curios namespace that is associated with the slot for the
   * given identifier.
   *
   * @param id The slot identifier
   * @return An item tag key
   */
  public static TagKey<Item> createItemTag(String id) {
    return TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(dev.arakiel.curiosfabric.CuriosFabricConstants.MOD_ID, id));
  }

  /**
   * The item tag of the original (Neoforge/Neoforge) Curios edition, {@code #curios:<slot>}.
   *
   * <p>Only the compatibility layer reads this: items that a Neoforge mod assigned to a slot through
   * the old tag namespace are mapped onto the matching {@link #createItemTag(String)} tag.</p>
   *
   * @param id The slot identifier
   * @return An item tag key in the neoforge namespace
   */
  public static TagKey<Item> neoforgeItemTag(String id) {
    return TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(dev.arakiel.curiosfabric.CuriosFabricConstants.NEOFORGE_CURIOS_DATA_NAMESPACE,
            id));
  }
}
