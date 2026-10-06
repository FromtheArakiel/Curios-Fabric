/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Curios is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Curios.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package dev.arakiel.curios.api;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curios.api.type.inventory.IItemHandler;
import dev.arakiel.curios.api.type.capability.ICurio;
import dev.arakiel.curios.api.type.capability.ICuriosItemHandler;

/**
 * Access to the Curios capabilities.
 *
 * <p>The NeoForge edition exposes these through NeoForge's capability system. Fabric has no such
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
