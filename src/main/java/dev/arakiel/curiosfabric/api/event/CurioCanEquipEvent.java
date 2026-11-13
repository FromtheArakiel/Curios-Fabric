package dev.arakiel.curiosfabric.api.event;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curiosfabric.api.TriState;
import dev.arakiel.curiosfabric.api.SlotContext;

/**
 * CurioEquipEvent is fired when a curio item is about to be equipped and allows an event listener
 * to specify whether it should or not. <br>
 * This event is fired when ever the {@link dev.arakiel.curiosfabric.api.type.capability.ICurio#canEquip(SlotContext)}
 * is checked. <br>
 * <br>
 * This event has a {@link TriState result}:
 * <ul><li>{@link TriState#TRUE} means the curio item can be equipped.</li>
 * <li>{@link TriState#DEFAULT} means the validators and {@link dev.arakiel.curiosfabric.api.type.capability.ICurio#canEquip(SlotContext)}
 * determines the result.</li>
 * <li>{@link TriState#FALSE} means the curio item cannot be equipped.</li></ul><br>
 * This event is fired on the {@link CuriosEventBus}.
 */
public class CurioCanEquipEvent implements ICuriosEvent {

  private final SlotContext slotContext;
  private final ItemStack stack;
  private TriState result;

  public CurioCanEquipEvent(ItemStack stack, SlotContext slotContext, TriState result) {
    this.slotContext = slotContext;
    this.stack = stack;
    this.result = result;
  }

  /** The entity involved in this event, may be null when there is no wearer. */
  @Nullable
  public LivingEntity getEntity() {
    return this.slotContext.entity();
  }

  @Deprecated(forRemoval = true, since = "1.22")
  public CurioCanEquipEvent(ItemStack stack, SlotContext slotContext) {
    this(stack, slotContext, TriState.DEFAULT);
  }

  public TriState getEquipResult() {
    return this.result;
  }

  public void setEquipResult(TriState result) {
    this.result = result;
  }

  public SlotContext getSlotContext() {
    return this.slotContext;
  }

  public ItemStack getStack() {
    return this.stack;
  }
}
