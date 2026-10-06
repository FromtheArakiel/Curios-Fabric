/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package dev.arakiel.curios.common.capability;

import net.minecraft.world.entity.LivingEntity;

/**
 * Implemented by {@code LivingEntity} through {@code MixinLivingEntityInventory}.
 *
 * <p>The NeoForge edition stores the curios inventory in a NeoForge data attachment. Fabric has no
 * attachments in the loader itself, so the port keeps a lazily created {@link CurioInventory} on the
 * entity and persists it in the entity's NBT under {@code CuriosInventory}.</p>
 */
public interface CuriosInventoryHolder {

  /** The inventory of this entity, creating it on first access. */
  CurioInventory curios$getInventory();

  /** Access helper that never fails for living entities. */
  static CurioInventory get(LivingEntity livingEntity) {
    return ((CuriosInventoryHolder) livingEntity).curios$getInventory();
  }
}
