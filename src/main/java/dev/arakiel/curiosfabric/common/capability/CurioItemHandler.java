package dev.arakiel.curiosfabric.common.capability;

import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import dev.arakiel.curiosfabric.api.type.inventory.IItemHandler;
import dev.arakiel.curiosfabric.api.type.inventory.IItemHandlerModifiable;
import dev.arakiel.curiosfabric.api.type.inventory.ICurioStacksHandler;
import dev.arakiel.curiosfabric.common.inventory.CombinedInvWrapper;
import dev.arakiel.gemini.component.GeminiComponents;

public class CurioItemHandler implements IItemHandler {

  final IItemHandler curios;
  final LivingEntity livingEntity;

  public CurioItemHandler(final LivingEntity livingEntity) {
    this.livingEntity = livingEntity;
    CurioInventory inv = GeminiComponents.get(CurioInventory.KEY, livingEntity);
    Map<String, ICurioStacksHandler> curios = inv.curios;
    IItemHandlerModifiable[] itemHandlers = new IItemHandlerModifiable[curios.size()];
    int index = 0;

    for (ICurioStacksHandler stacksHandler : curios.values()) {

      if (index < itemHandlers.length) {
        itemHandlers[index] = stacksHandler.getStacks();
        index++;
      }
    }
    this.curios = new CombinedInvWrapper(itemHandlers);
  }

  @Override
  public int getSlots() {
    return this.curios.getSlots();
  }

  @Override
  public @NotNull ItemStack getStackInSlot(int slot) {
    return this.curios.getStackInSlot(slot);
  }

  @Override
  public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
    return this.curios.insertItem(slot, stack, simulate);
  }

  @Override
  public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
    return this.curios.extractItem(slot, amount, simulate);
  }

  @Override
  public int getSlotLimit(int slot) {
    return this.curios.getSlotLimit(slot);
  }

  @Override
  public boolean isItemValid(int slot, @NotNull ItemStack stack) {
    return this.curios.isItemValid(slot, stack);
  }
}
