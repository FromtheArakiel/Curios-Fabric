package dev.arakiel.curiosfabric.compat.modules.accessories.api;

import com.mojang.datafixers.util.Pair;
import dev.arakiel.curiosfabric.api.type.inventory.IDynamicStackHandler;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.impl.AccessoriesContainerImpl;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * CuriosFabric-backed view of the concrete container type exposed by Accessories.
 *
 * <p>The parent class owns a dummy backing list because Accessories exposes a concrete class
 * rather than an interface. Every storage operation below is overridden and delegated; the dummy
 * list is never used as inventory state.</p>
 */
final class AccessoriesInventoryView extends ExpandedSimpleContainer {

  private final IDynamicStackHandler delegate;
  private final String viewName;

  AccessoriesInventoryView(
      AccessoriesCapability capability,
      SlotType slotType,
      IDynamicStackHandler delegate,
      String viewName) {
    super(
        new AccessoriesContainerImpl(capability, slotType),
        delegate.getSlots(),
        viewName,
        false);
    this.delegate = delegate;
    this.viewName = viewName;
  }

  @Override
  public int getContainerSize() {
    return delegate.getSlots();
  }

  @Override
  public boolean isEmpty() {
    for (int i = 0; i < delegate.getSlots(); i++) {
      if (!delegate.getStackInSlot(i).isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public ItemStack getItem(int slot) {
    return validIndex(slot) ? delegate.getStackInSlot(slot) : ItemStack.EMPTY;
  }

  @Override
  public ItemStack removeItem(int slot, int amount) {
    return validIndex(slot) ? delegate.extractItem(slot, amount, false) : ItemStack.EMPTY;
  }

  @Override
  public ItemStack removeItemNoUpdate(int slot) {
    if (!validIndex(slot)) {
      return ItemStack.EMPTY;
    }
    ItemStack removed = delegate.getStackInSlot(slot);
    delegate.setStackInSlot(slot, ItemStack.EMPTY);
    return removed;
  }

  @Override
  public void setItem(int slot, ItemStack stack) {
    if (validIndex(slot)) {
      delegate.setStackInSlot(slot, stack);
    }
  }

  @Override
  public ItemStack getPreviousItem(int slot) {
    return validIndex(slot) ? delegate.getPreviousStackInSlot(slot) : ItemStack.EMPTY;
  }

  @Override
  public void setPreviousItem(int slot, ItemStack stack) {
    if (validIndex(slot)) {
      delegate.setPreviousStackInSlot(slot, stack);
    }
  }

  @Override
  public int getMaxStackSize() {
    return delegate.getSlots() == 0 ? 64 : delegate.getSlotLimit(0);
  }

  @Override
  public int getMaxStackSize(ItemStack stack) {
    return Math.min(getMaxStackSize(), AccessoriesAPI.getOrDefaultAccessory(stack).maxStackSize(stack));
  }

  @Override
  public boolean validIndex(int slot) {
    return slot >= 0 && slot < delegate.getSlots();
  }

  @Override
  public boolean isSlotFlagged(int slot) {
    return false;
  }

  @Override
  public boolean wasNewlyConstructed() {
    return false;
  }

  @Override
  public String name() {
    return viewName;
  }

  @Override
  public void setChanged() {
  }

  @Override
  public void clearContent() {
    for (int i = 0; i < delegate.getSlots(); i++) {
      delegate.setStackInSlot(i, ItemStack.EMPTY);
    }
  }

  @Override
  public ListTag createTag(HolderLookup.Provider provider) {
    ListTag list = new ListTag();
    for (int i = 0; i < delegate.getSlots(); i++) {
      ItemStack stack = delegate.getStackInSlot(i);
      if (!stack.isEmpty()) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Slot", i);
        list.add(stack.save(provider, tag));
      }
    }
    return list;
  }

  @Override
  public void fromTag(ListTag list, HolderLookup.Provider provider) {
    clearContent();
    for (int i = 0; i < list.size(); i++) {
      CompoundTag tag = list.getCompound(i);
      int slot = tag.getInt("Slot");
      if (validIndex(slot)) {
        ItemStack.parse(provider, tag).ifPresent(stack -> delegate.setStackInSlot(slot, stack));
      }
    }
  }

  @Override
  public Iterator<Pair<Integer, ItemStack>> iterator() {
    return new Iterator<>() {
      private int index;

      @Override
      public boolean hasNext() {
        return index < delegate.getSlots();
      }

      @Override
      public Pair<Integer, ItemStack> next() {
        int current = index++;
        return Pair.of(current, delegate.getStackInSlot(current));
      }
    };
  }
}
