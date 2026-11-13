package dev.arakiel.curiosfabric.common.inventory;

import javax.annotation.Nonnull;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A {@link Container} that holds nothing.
 *
 * <p>{@code Slot} requires a container in its constructor, but {@link SlotItemHandler} never
 * delegates to it. This is the Fabric equivalent of Neoforge's private {@code emptyInventory}
 * instance.</p>
 */
public final class EmptyContainer implements Container {

  public static final EmptyContainer INSTANCE = new EmptyContainer();

  private EmptyContainer() {
  }

  @Override
  public int getContainerSize() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Nonnull
  @Override
  public ItemStack getItem(int index) {
    return ItemStack.EMPTY;
  }

  @Nonnull
  @Override
  public ItemStack removeItem(int index, int count) {
    return ItemStack.EMPTY;
  }

  @Nonnull
  @Override
  public ItemStack removeItemNoUpdate(int index) {
    return ItemStack.EMPTY;
  }

  @Override
  public void setItem(int index, @Nonnull ItemStack stack) {
  }

  @Override
  public void setChanged() {
  }

  @Override
  public boolean stillValid(@Nonnull Player player) {
    return false;
  }

  @Override
  public void clearContent() {
  }
}
