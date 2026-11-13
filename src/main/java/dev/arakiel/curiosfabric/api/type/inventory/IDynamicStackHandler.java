package dev.arakiel.curiosfabric.api.type.inventory;

import javax.annotation.Nonnull;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public interface IDynamicStackHandler extends IItemHandlerModifiable {

  /**
   * Sets a {@link ItemStack} to the given slot index as the current stack.
   *
   * @param slot  The slot index
   * @param stack The {@link ItemStack} to assign as the current stack
   */
  void setStackInSlot(int slot, @Nonnull ItemStack stack);

  /**
   * Gets the {@link ItemStack} assigned as the current stack in the given slot index
   *
   * @param slot The slot index
   * @return The {@link ItemStack} assigned as the current stack
   */
  @Nonnull
  ItemStack getStackInSlot(int slot);

  /**
   * Sets a {@link ItemStack} to the given slot index as the previous stack, for comparison purposes
   * with the current stack.
   *
   * @param slot  The slot index
   * @param stack The {@link ItemStack} to assign as the previous stack
   */
  void setPreviousStackInSlot(int slot, @Nonnull ItemStack stack);

  /**
   * Gets the {@link ItemStack} assigned as the previous stack in the given slot index
   *
   * @param slot The slot index
   * @return The {@link ItemStack} assigned as the previous stack
   */
  ItemStack getPreviousStackInSlot(int slot);

  /**
   * @return The total number of slots
   */
  int getSlots();

  /**
   * Increases the number of slots by the given amount.
   *
   * @param amount The number of slots to add
   */
  void grow(int amount);

  /**
   * Decreases the number of slots by the given amount.
   *
   * @param amount The number of slots to remove
   */
  void shrink(int amount);

  /**
   * Writes the data for this handler.
   *
   * @return A {@link CompoundTag} representing the serialized data
   */
  CompoundTag serializeNBT(HolderLookup.Provider provider);

  /**
   * Reads the data into this handler.
   *
   * @param nbt A {@link CompoundTag} representing the serialized data
   */
  void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt);
}
