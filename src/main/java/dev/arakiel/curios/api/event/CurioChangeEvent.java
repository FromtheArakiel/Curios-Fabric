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

import javax.annotation.Nonnull;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * {@link CurioChangeEvent} is fired when the Curio of a LivingEntity changes. <br> This event is
 * fired whenever changes in curios are detected in the entity tick.
 * <br> This also includes entities joining the World, as well as being cloned. <br> This event is
 * fired on server-side only. <br>
 * <br>
 * {@link #type} contains the affected {@link dev.arakiel.curios.api.type.ISlotType}. <br>
 * {@link #from} contains the {@link ItemStack} that was equipped previously.
 * <br>
 * {@link #to} contains the {@link ItemStack} that is equipped now. <br> {@link #index} contains the
 * index of the curio slot
 * <br>
 * This event is fired on the {@link CuriosEventBus}.
 **/
public class CurioChangeEvent implements ICuriosEvent {

  private final LivingEntity livingEntity;
  private final String type;
  private final ItemStack from;
  private final ItemStack to;
  private final int index;

  public CurioChangeEvent(LivingEntity living, String type, int index, @Nonnull ItemStack from,
                          @Nonnull ItemStack to) {
    this.livingEntity = living;
    this.type = type;
    this.from = from;
    this.to = to;
    this.index = index;
  }

  /** The entity whose curio changed. */
  public LivingEntity getEntity() {
    return this.livingEntity;
  }

  public String getIdentifier() {
    return this.type;
  }

  public int getSlotIndex() {
    return this.index;
  }

  @Nonnull
  public ItemStack getFrom() {
    return this.from;
  }

  @Nonnull
  public ItemStack getTo() {
    return this.to;
  }
}
