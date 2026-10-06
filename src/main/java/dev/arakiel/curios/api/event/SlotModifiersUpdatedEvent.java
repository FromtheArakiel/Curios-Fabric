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

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.world.entity.LivingEntity;

/**
 * {@link SlotModifiersUpdatedEvent} is fired when the slot size is dynamically changed during
 * gameplay through slot modifiers.
 * <br> This event is fired on both the client and the server.
 * <br>
 * {@link #types} contains the affected {@link dev.arakiel.curios.api.type.ISlotType}. <br>
 * <br>
 * This event is fired on the {@link CuriosEventBus}.
 **/
public class SlotModifiersUpdatedEvent implements ICuriosEvent {

  private final LivingEntity livingEntity;
  private final Set<String> types;

  public SlotModifiersUpdatedEvent(LivingEntity livingEntity, Set<String> types) {
    this.livingEntity = livingEntity;
    this.types = types;
  }

  /** The entity whose slot modifiers changed. */
  public LivingEntity getEntity() {
    return this.livingEntity;
  }

  public Set<String> getTypes() {
    return ImmutableSet.copyOf(this.types);
  }
}
