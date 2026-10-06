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

import java.util.Collection;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import dev.arakiel.curios.api.type.capability.ICuriosItemHandler;

/**
 * LivingCurioDropsEvent is fired when an Entity's death causes dropped curios to appear.<br> This
 * event is fired whenever an Entity dies and drops items in {@link LivingEntity#die(DamageSource)}.<br>
 * <br>
 * This event is fired while the entity's death drops are collected.<br>
 * <br>
 * {@link #source} contains the DamageSource that caused the drop to occur.<br> {@link #drops}
 * contains the ArrayList of ItemEntity that will be dropped.<br> {@link #recentlyHit} determines whether the Entity doing
 * the drop has recently been damaged.<br>
 * <br>
 * This event is fired on the {@link CuriosEventBus}.
 **/
public class CurioDropsEvent implements ICancellableCuriosEvent {

  private final LivingEntity entity;
  private final DamageSource source;
  private final Collection<ItemEntity> drops;
  private final int lootingLevel;
  private final boolean recentlyHit;
  private final ICuriosItemHandler curioHandler; // Curio handler for the entity
  private boolean canceled;

  public CurioDropsEvent(LivingEntity entity, ICuriosItemHandler handler, DamageSource source,
                         Collection<ItemEntity> drops, int lootingLevel, boolean recentlyHit) {
    this.entity = entity;
    this.source = source;
    this.drops = drops;
    this.lootingLevel = lootingLevel;
    this.recentlyHit = recentlyHit;
    this.curioHandler = handler;
  }

  /** The entity that died. */
  public LivingEntity getEntity() {
    return this.entity;
  }

  @Override
  public boolean isCanceled() {
    return this.canceled;
  }

  @Override
  public void setCanceled(boolean canceled) {
    this.canceled = canceled;
  }

  public ICuriosItemHandler getCurioHandler() {
    return this.curioHandler;
  }

  public DamageSource getSource() {
    return this.source;
  }

  public Collection<ItemEntity> getDrops() {
    return this.drops;
  }

  public int getLootingLevel() {
    return this.lootingLevel;
  }

  public boolean isRecentlyHit() {
    return this.recentlyHit;
  }
}
