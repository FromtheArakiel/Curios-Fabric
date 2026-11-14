package dev.arakiel.curiosfabric.compat.api;

import dev.arakiel.curiosfabric.api.type.capability.ICurioItem;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/** Resolves the Curios behavior adapter for a foreign accessory. */
public interface ItemBehaviorBridge {

  Optional<ICurioItem> resolve(ItemStack stack);
}
