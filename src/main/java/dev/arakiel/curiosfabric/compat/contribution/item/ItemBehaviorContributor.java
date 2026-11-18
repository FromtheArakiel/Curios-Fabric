package dev.arakiel.curiosfabric.compat.contribution.item;

import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/** Resolves the CuriosFabric behavior adapter for a foreign accessory item. */
public interface ItemBehaviorContributor {

  Optional<ItemBehaviorResolution> resolve(ItemStack stack);
}
