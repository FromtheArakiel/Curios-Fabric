package dev.arakiel.curiosfabric.api.type.inventory;

import net.minecraft.world.item.ItemStack;

/**
 * A minimal, loader independent item handler.
 *
 * <p>The Neoforge edition of Curios exposes Neoforge's {@code IItemHandler} in its public API. The
 * Fabric edition cannot do that, so the same contract is declared here with the exact same method
 * names and semantics. Everything that only needs to enumerate or transfer stacks keeps working
 * unchanged, and no Neoforge code is required at runtime.</p>
 */
public interface IItemHandler {

  /** The number of slots in this handler. */
  int getSlots();

  /** The stack in the given slot, never {@code null} (use {@link ItemStack#EMPTY}). */
  ItemStack getStackInSlot(int slot);

  /**
   * Inserts a stack into the given slot.
   *
   * @param slot     The slot to insert into
   * @param stack    The stack to insert
   * @param simulate True to only simulate the insertion
   * @return The remainder of the stack that did not fit
   */
  ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

  /**
   * Extracts a stack from the given slot.
   *
   * @param slot     The slot to extract from
   * @param amount   The maximum amount to extract
   * @param simulate True to only simulate the extraction
   * @return The extracted stack, possibly empty
   */
  ItemStack extractItem(int slot, int amount, boolean simulate);

  /** The maximum stack size of the given slot. */
  int getSlotLimit(int slot);

  /** True if the given stack may be placed into the given slot. */
  boolean isItemValid(int slot, ItemStack stack);
}
