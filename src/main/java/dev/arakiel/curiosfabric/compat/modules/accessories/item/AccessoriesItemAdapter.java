package dev.arakiel.curiosfabric.compat.modules.accessories.item;

import dev.arakiel.curiosfabric.compat.contribution.item.ItemBehaviorResolution;
import dev.arakiel.curiosfabric.compat.modules.accessories.AccessoriesCompat;
import io.wispforest.accessories.api.AccessoriesAPI;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/** Resolves the single CuriosFabric behavior adapter for an Accessories item. */
public final class AccessoriesItemAdapter {

  private static final AccessoriesCurio DEFAULT = new AccessoriesCurio();

  private AccessoriesItemAdapter() {
  }

  public static Optional<ItemBehaviorResolution> resolve(ItemStack stack) {
    if (!AccessoriesCompat.isAccessoriesItem(stack, false)
        && !AccessoriesCompat.isAccessoriesItem(stack, true)) {
      return Optional.empty();
    }
    boolean codeBacked = AccessoriesAPI.getAccessory(stack.getItem()) != null;
    return Optional.of(new ItemBehaviorResolution(DEFAULT, codeBacked));
  }
}
