package dev.arakiel.curiosfabric.api;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curiosfabric.api.type.inventory.IItemHandler;
import dev.arakiel.curiosfabric.api.type.capability.ICurio;
import dev.arakiel.curiosfabric.api.type.capability.ICuriosItemHandler;

/**
 * Access to the Curios capabilities.
 *
 * <p>The Neoforge edition exposes these through Neoforge's capability system. Fabric has no such
 * system, so the same three capabilities are offered as static accessors. The identifiers are kept
 * so that documentation and cross loader tooling still have a stable reference.</p>
 */
public final class CuriosCapability {

  public static final ResourceLocation ID_INVENTORY =
      ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, "inventory");
  public static final ResourceLocation ID_ITEM_HANDLER =
      ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, "item_handler");
  public static final ResourceLocation ID_ITEM =
      ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, "item");

  private CuriosCapability() {
  }

  /**
   * Gets the curio inventory of an entity, empty when the entity has no curio slots.
   *
   * @param livingEntity The entity to query
   * @return The curio inventory handler
   */
  public static Optional<ICuriosItemHandler> getInventory(LivingEntity livingEntity) {
    return CuriosApi.getCuriosInventory(livingEntity);
  }

  /**
   * Gets a plain {@link IItemHandler} view of the equipped (non cosmetic) curios of an entity.
   *
   * @param livingEntity The entity to query
   * @return The item handler view
   */
  public static Optional<IItemHandler> getItemHandler(LivingEntity livingEntity) {
    return CuriosApi.getCuriosInventory(livingEntity)
        .map(handler -> (IItemHandler) handler.getEquippedCurios());
  }

  /**
   * Gets the curio behaviour of an item stack, empty when the stack is not a curio.
   *
   * @param stack The stack to query
   * @return The curio behaviour
   */
  public static Optional<ICurio> getItem(ItemStack stack) {
    return CuriosApi.getCurio(stack);
  }
}
