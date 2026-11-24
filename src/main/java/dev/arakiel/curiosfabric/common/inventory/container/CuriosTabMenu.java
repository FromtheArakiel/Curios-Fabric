package dev.arakiel.curiosfabric.common.inventory.container;

import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.common.CuriosFabricRegistry;
import dev.arakiel.curiosfabric.common.inventory.CurioSlot;
import dev.arakiel.curiosfabric.common.inventory.CuriosVisibleSlots;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Independent 54-slot Curios list opened by the Gemini tab. */
public class CuriosTabMenu extends AbstractContainerMenu {

  private final Player player;
  private final int currentPage;
  private final int totalPages;
  private final int rows;

  public CuriosTabMenu(int containerId, Inventory playerInventory, int requestedPage) {
    super(CuriosFabricRegistry.CURIO_TAB_MENU.get(), containerId);
    this.player = playerInventory.player;

    List<CuriosVisibleSlots.Entry> allSlots = CuriosVisibleSlots.collect(this.player);
    this.totalPages = CuriosTabLayout.pageCount(allSlots.size());
    this.currentPage = Math.max(0, Math.min(requestedPage, totalPages - 1));
    List<CuriosVisibleSlots.Entry> pageEntries =
        CuriosVisibleSlots.page(allSlots, currentPage);
    this.rows = CuriosTabLayout.rows(pageEntries.size());

    int playerInventoryY = CuriosTabLayout.playerInventoryY(rows);
    for (int row = 0; row < 3; row++) {
      for (int column = 0; column < CuriosTabLayout.COLUMNS; column++) {
        addSlot(new Slot(playerInventory, 9 + row * CuriosTabLayout.COLUMNS + column,
            CuriosTabLayout.SLOT_START_X + column * CuriosTabLayout.SLOT_SPACING,
            playerInventoryY + row * CuriosTabLayout.SLOT_SPACING));
      }
    }

    int hotbarY = CuriosTabLayout.playerHotbarY(rows);
    for (int column = 0; column < CuriosTabLayout.COLUMNS; column++) {
      addSlot(new Slot(playerInventory, column,
          CuriosTabLayout.SLOT_START_X + column * CuriosTabLayout.SLOT_SPACING, hotbarY));
    }

    for (int index = 0; index < pageEntries.size(); index++) {
      CuriosVisibleSlots.Entry entry = pageEntries.get(index);
      int column = index % CuriosTabLayout.COLUMNS;
      int row = index / CuriosTabLayout.COLUMNS;
      addSlot(new CurioSlot(
          player,
          entry.stacks(),
          entry.index(),
          entry.identifier(),
          CuriosTabLayout.SLOT_START_X + column * CuriosTabLayout.SLOT_SPACING,
          CuriosTabLayout.SLOT_START_Y + row * CuriosTabLayout.SLOT_SPACING,
          entry.stacksHandler().getRenders(),
          entry.stacksHandler().getActiveStates(),
          entry.stacksHandler().canToggleRendering(),
          false,
          false));
    }
  }

  public int currentPage() {
    return currentPage;
  }

  public int totalPages() {
    return totalPages;
  }

  public int rows() {
    return rows;
  }

  @Nonnull
  @Override
  public ItemStack quickMoveStack(@Nonnull Player player, int index) {
    if (index < 0 || index >= slots.size()) {
      return ItemStack.EMPTY;
    }
    Slot slot = slots.get(index);
    if (!slot.hasItem()) {
      return ItemStack.EMPTY;
    }

    ItemStack stack = slot.getItem();
    ItemStack original = stack.copy();
    if (index < CuriosTabLayout.PLAYER_INVENTORY_SLOTS) {
      if (!moveItemStackTo(
          stack, CuriosTabLayout.PLAYER_INVENTORY_SLOTS, slots.size(), false)) {
        return ItemStack.EMPTY;
      }
    } else if (!moveItemStackTo(
        stack, 0, CuriosTabLayout.PLAYER_INVENTORY_SLOTS, true)) {
      return ItemStack.EMPTY;
    }

    if (stack.isEmpty()) {
      slot.set(ItemStack.EMPTY);
    } else {
      slot.setChanged();
    }
    if (stack.getCount() == original.getCount()) {
      return ItemStack.EMPTY;
    }
    slot.onTake(player, stack);
    return original;
  }

  @Override
  public boolean stillValid(@Nonnull Player player) {
    return player == this.player && player.isAlive()
        && CuriosApi.getCuriosInventory(player).isPresent();
  }
}
