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

package dev.arakiel.curios.common.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import dev.arakiel.curios.CuriosConstants;

/**
 * Reads and writes Fabric's {@code fabric:load_conditions} entries.
 *
 * <p>NeoForge's {@code ICondition} was used for this in the NeoForge edition. Fabric's API is not
 * applied automatically to custom reload listeners such as Curios' slot and entity managers, so the
 * check is done explicitly here.</p>
 */
public final class CuriosConditions {

  private CuriosConditions() {
  }

  /** {@code true} when the JSON object has no conditions or all of them match. */
  public static boolean matches(JsonObject object) {
    JsonElement element = object.get(ResourceConditions.CONDITIONS_KEY);

    if (element == null) {
      return true;
    }

    try {
      return ResourceCondition.CONDITION_CODEC.parse(JsonOps.INSTANCE, element).result()
          .map(condition -> condition.test(null))
          .orElse(true);
    } catch (RuntimeException exception) {
      CuriosConstants.LOG.warn("Failed to parse the load conditions of a curio data file",
          exception);
      return true;
    }
  }

  /** Writes the conditions into the JSON object, if there are any. */
  public static void write(JsonObject object, List<ResourceCondition> conditions) {

    if (conditions == null || conditions.isEmpty()) {
      return;
    }
    JsonArray array = new JsonArray();

    for (ResourceCondition condition : conditions) {
      ResourceCondition.CODEC.encodeStart(JsonOps.INSTANCE, condition).result()
          .ifPresent(array::add);
    }
    object.add(ResourceConditions.CONDITIONS_KEY, array);
  }
}
