package dev.arakiel.curiosfabric.api.event;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curiosfabric.api.TriState;
import dev.arakiel.curiosfabric.api.SlotContext;

/**
 * CurioUnequipEvent is fired when a curio item is about to be unequipped and allows an event
 * listener to specify whether it should or not. <br>
 * This event is fired when ever the {@link dev.arakiel.curiosfabric.api.type.capability.ICurio#canUnequip(SlotContext)}
 * is checked. <br>
 * <br>
 * This event has a {@link TriState result}:
 * <ul><li>{@link TriState#TRUE} means the curio item can be unequipped.</li>
 * <li>{@link TriState#DEFAULT} means {@link dev.arakiel.curiosfabric.api.type.capability.ICurio#canUnequip(SlotContext)}
 * determines the result.</li>
 * <li>{@link TriState#FALSE} means the curio item cannot be unequipped.</li></ul><br>
 * This event is fired on the {@link CuriosEventBus}.
 */
public class CurioCanUnequipEvent implements ICuriosEvent {

  private final SlotContext slotContext;
  private final ItemStack stack;
  private TriState result;

  public CurioCanUnequipEvent(ItemStack stack, SlotContext slotContext) {
    this.slotContext = slotContext;
    this.stack = stack;
  }

  /** The entity involved in this event, may be null when there is no wearer. */
  @Nullable
  public LivingEntity getEntity() {
    return this.slotContext.entity();
  }

  public TriState getUnequipResult() {
    return this.result;
  }

  public void setUnequipResult(TriState result) {
    this.result = result;
  }

  public SlotContext getSlotContext() {
    return slotContext;
  }

  public ItemStack getStack() {
    return stack;
  }
}
