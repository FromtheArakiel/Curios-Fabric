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

package dev.arakiel.curios.mixin;

import java.util.Map;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.core.registries.Registries;
import java.util.Optional;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.entity.Entity;
import dev.arakiel.curios.api.CuriosApi;
import dev.arakiel.curios.api.SlotContext;
import dev.arakiel.curios.api.type.inventory.IItemHandler;
import dev.arakiel.curios.api.type.inventory.IItemHandlerModifiable;
import dev.arakiel.curios.api.type.inventory.ICurioStacksHandler;
import dev.arakiel.curios.api.type.inventory.IDynamicStackHandler;

public class CuriosUtilMixinHooks {

  private CuriosUtilMixinHooks() {
  }

  /**
   * Finds the first damaged curio with the Mending enchantment, so that an experience orb can repair
   * it. The returned stack is the live one held by the inventory.
   *
   * @param player The player to search
   * @return The stack to repair, or empty when no curio can be repaired
   */
  public static Optional<ItemStack> findMendingCurio(Player player) {
    Holder<Enchantment> mending =
        player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(Enchantments.MENDING);
    return CuriosApi.getCuriosInventory(player).flatMap(handler -> {

      for (ICurioStacksHandler stacksHandler : handler.getCurios().values()) {

        for (IDynamicStackHandler stacks : List.of(stacksHandler.getStacks(),
            stacksHandler.getCosmeticStacks())) {

          for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack stack = stacks.getStackInSlot(i);

            if (!stack.isEmpty() && stack.isDamaged()
                && EnchantmentHelper.getItemEnchantmentLevel(mending, stack) > 0) {
              return Optional.of(stack);
            }
          }
        }
      }
      return Optional.empty();
    });
  }

  public static boolean canNeutralizePiglins(LivingEntity livingEntity) {
    return CuriosApi.getCuriosInventory(livingEntity).map(handler -> {

      for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
        IDynamicStackHandler stacks = entry.getValue().getStacks();

        for (int i = 0; i < stacks.getSlots(); i++) {
          final int index = i;
          NonNullList<Boolean> renderStates = entry.getValue().getRenders();
          boolean canNeutralize =
              CuriosApi.getCurio(stacks.getStackInSlot(i)).map(curio -> curio
                      .makesPiglinsNeutral(new SlotContext(entry.getKey(), livingEntity, index, false,
                          renderStates.size() > index && renderStates.get(index))))
                  .orElse(false);

          if (canNeutralize) {
            return true;
          }
        }
      }
      return false;
    }).orElse(false);
  }

  public static boolean canWalkOnPowderSnow(LivingEntity livingEntity) {
    return CuriosApi.getCuriosInventory(livingEntity).map(handler -> {

      for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
        IDynamicStackHandler stacks = entry.getValue().getStacks();

        for (int i = 0; i < stacks.getSlots(); i++) {
          final int index = i;
          NonNullList<Boolean> renderStates = entry.getValue().getRenders();
          boolean canWalk =
              CuriosApi.getCurio(stacks.getStackInSlot(i)).map(curio -> curio
                      .canWalkOnPowderedSnow(new SlotContext(entry.getKey(), livingEntity, index, false,
                          renderStates.size() > index && renderStates.get(index))))
                  .orElse(false);

          if (canWalk) {
            return true;
          }
        }
      }
      return false;
    }).orElse(false);
  }

  public static int getFortuneLevel(LootContext lootContext) {
    Entity entity = lootContext.getParamOrNull(LootContextParams.THIS_ENTITY);

    if (entity instanceof LivingEntity livingEntity) {
      return CuriosApi.getCuriosInventory(livingEntity)
          .map(handler -> handler.getFortuneLevel(lootContext)).orElse(0);
    }
    return 0;
  }

  public static int getLootingLevel(LootContext lootContext) {
    Entity entity = lootContext.getParamOrNull(LootContextParams.ATTACKING_ENTITY);

    if (entity instanceof LivingEntity livingEntity) {
      return CuriosApi.getCuriosInventory(livingEntity)
          .map(handler -> handler.getLootingLevel(lootContext)).orElse(0);
    }
    return 0;
  }

  public static boolean isFreezeImmune(LivingEntity livingEntity) {
    return CuriosApi.getCuriosInventory(livingEntity).map(curios -> {
      IItemHandlerModifiable handler = curios.getEquippedCurios();

      for (int i = 0; i < handler.getSlots(); i++) {
        ItemStack stack = handler.getStackInSlot(i);

        if (stack.is(ItemTags.FREEZE_IMMUNE_WEARABLES)) {
          return true;
        }
      }
      return false;
    }).orElse(false);
  }

  public static CompoundTag mergeCuriosInventory(CompoundTag compoundTag, Entity entity) {

    if (entity instanceof LivingEntity livingEntity) {
      ListTag list = compoundTag.getList("Inventory", Tag.TAG_COMPOUND);
      return CuriosApi.getCuriosInventory(livingEntity).map(inv -> {
        IItemHandler handler = inv.getEquippedCurios();

        for (int i = 0; i < handler.getSlots(); i++) {
          ItemStack stack = handler.getStackInSlot(i);

          if (!stack.isEmpty()) {
            CompoundTag tag = new CompoundTag();
            tag.putByte("Slot", (byte) (4444 + i));
            list.add(stack.save(livingEntity.registryAccess(), tag));
          }
        }
        return compoundTag;
      }).orElse(compoundTag);
    }
    return compoundTag;
  }

  public static boolean containsStack(Player player, ItemStack stack) {
    return CuriosApi.getCuriosInventory(player).flatMap(inv -> inv.findFirstCurio(
            stack2 -> !stack2.isEmpty() && ItemStack.isSameItemSameComponents(stack, stack2)))
        .isPresent();
  }

  public static boolean containsTag(Player player, TagKey<Item> tagKey) {
    return CuriosApi.getCuriosInventory(player).flatMap(
            inv -> inv.findFirstCurio(stack2 -> !stack2.isEmpty() && stack2.is(tagKey)))
        .isPresent();
  }

  public static boolean contains(Player player, Predicate<ItemStack> predicate) {
    return CuriosApi.getCuriosInventory(player).flatMap(inv -> inv.findFirstCurio(predicate)).isPresent();
  }
}
