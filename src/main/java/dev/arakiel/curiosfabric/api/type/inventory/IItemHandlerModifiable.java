package dev.arakiel.curiosfabric.api.type.inventory;

import net.minecraft.world.item.ItemStack;

/** An {@link IItemHandler} whose contents can be replaced directly. */
public interface IItemHandlerModifiable extends IItemHandler {

  /**
   * Overrides the stack in the given slot. Implementations must not copy the stack.
   *
   * @param slot  The slot to change
   * @param stack The new stack, may be empty
   */
  void setStackInSlot(int slot, ItemStack stack);
}
