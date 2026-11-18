package dev.arakiel.curiosfabric.common.inventory;

/** Shared safety rules for inventory operations that move or retain item stacks. */
public final class InventoryOperationPolicy {

  private InventoryOperationPolicy() {
  }

  /**
   * Death drops only remove the stacks that should leave the entity. The remaining snapshot must
   * therefore follow the same character through respawn, including KeepInventory and ALWAYS_KEEP.
   */
  public static boolean shouldCopyForRespawn(boolean lossless, boolean sameCharacter) {
    return lossless || sameCharacter;
  }

  /** Unequip hooks and modifiers only change when the entire stack has left the slot. */
  public static boolean shouldRunUnequip(int beforeCount, int remainingCount) {
    return beforeCount > 0 && remainingCount == 0;
  }

  /** Drop callbacks may only clear a staged slot when the event accepted that drop. */
  public static boolean shouldClearAfterDrops(boolean canceled, boolean accepted) {
    return !canceled && accepted;
  }
}
