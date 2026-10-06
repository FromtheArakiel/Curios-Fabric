/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Curios is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Curios.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package dev.arakiel.curios.api.event;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curios.api.TriState;
import dev.arakiel.curios.api.SlotContext;

/**
 * CurioUnequipEvent is fired when a curio item is about to be unequipped and allows an event
 * listener to specify whether it should or not. <br>
 * This event is fired when ever the {@link dev.arakiel.curios.api.type.capability.ICurio#canUnequip(SlotContext)}
 * is checked. <br>
 * <br>
 * This event has a {@link TriState result}:
 * <ul><li>{@link TriState#TRUE} means the curio item can be unequipped.</li>
 * <li>{@link TriState#DEFAULT} means {@link dev.arakiel.curios.api.type.capability.ICurio#canUnequip(SlotContext)}
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
